/*********************************************************************
 * Copyright (c) 2017, 2018 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Lucas Bullen (Red Hat Inc.) - Initial implementation
 *******************************************************************************/
package org.eclipse.corrosion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.lsp4e.LanguageServiceAccessor;
import org.eclipse.lsp4e.LanguageServiceAccessor.LSPDocumentInfo;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

//TODO: investigate solution not requiring breaking restrictions
@SuppressWarnings("restriction")
public class RustManager {
	private static final IPreferenceStore STORE = CorrosionPlugin.getDefault().getPreferenceStore();

	public static String getDefaultToolchain() {
		String rustup = STORE.getString(CorrosionPreferenceInitializer.rustupPathPreference);
		if (!rustup.isEmpty()) {
			try {
				ProcessBuilder builder = new ProcessBuilder(new String[] { rustup, "show" }); //$NON-NLS-1$
				Process process = builder.start();

				if (process.waitFor() == 0) {
					try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
						String line = in.readLine();
						while (line != null) {
							if (line.matches("^.*\\(default\\)$")) { //$NON-NLS-1$
								if (line.matches("^nightly-\\d{4}-\\d{2}-\\d{2}.*$")) { //$NON-NLS-1$
									return line.substring(0, 18);// "nightly-YYYY-MM-DD".length()==18
								}
								int splitIndex = line.indexOf('-');
								if (splitIndex != -1) {
									return line.substring(0, splitIndex);
								}
								return line;
							}
							line = in.readLine();
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				// Errors will be caught with empty return
			}
		}
		return ""; //$NON-NLS-1$
	}

	private static Job settingToolchainJob = null;

	public static void setDefaultToolchain(String toolchainId) {
		if (settingToolchainJob != null) {
			settingToolchainJob.cancel();
		}

		settingToolchainJob = Job.create(Messages.RustManager_settingRLSToolchain, monitor -> {
			SubMonitor subMonitor = SubMonitor.convert(monitor, 5);
			subMonitor.beginTask(Messages.RustManager_installingToolchain, 5);
			subMonitor.split(1);
			if (!runRustupCommand(subMonitor, "toolchain", "install", toolchainId)) { //$NON-NLS-1$ //$NON-NLS-2$
				if (!monitor.isCanceled()) {
					showToolchainSelectionError(NLS.bind(Messages.RustManager_unableToInstallToolchain, toolchainId));
				}
				return;
			}
			subMonitor.subTask(Messages.RustManager_settingDefaultToolchain);
			subMonitor.split(1);
			if (!runRustupCommand(subMonitor, "default", toolchainId)) { //$NON-NLS-1$
				if (!monitor.isCanceled()) {
					showToolchainSelectionError(NLS.bind(Messages.RustManager_unableToSetDefaultToolchain, toolchainId));
				}
				return;
			}
			subMonitor.subTask(Messages.RustManager_addingRLSPrevios);
			subMonitor.split(1);
			if (!runRustupCommand(subMonitor, "component", "add", "rls-preview")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				if (!monitor.isCanceled()) {
					showToolchainSelectionError(NLS.bind(Messages.RustManager_toolchainDoesntIncludeRLS, toolchainId));
				}
				return;
			}
			subMonitor.subTask(Messages.RustManager_addingRustAnalysisRustSrc);
			subMonitor.split(1);
			if (!runRustupCommand(subMonitor, "component", "add", "rust-analysis") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					|| !runRustupCommand(subMonitor, "component", "add", "rust-src")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				if (!monitor.isCanceled()) {
					showToolchainSelectionError(Messages.RustManager_unableToAddComponent);
				}
				return;
			}
			Map<String, String> updatedSettings = new HashMap<>();
			updatedSettings.put("target", toolchainId); //$NON-NLS-1$

			sendDidChangeConfigurationsMessage(updatedSettings);
		});
		settingToolchainJob.schedule();
	}

	private static void sendDidChangeConfigurationsMessage(Map<String, String> updatedSettings) {
		DidChangeConfigurationParams params = new DidChangeConfigurationParams();
		params.setSettings(updatedSettings);
		LSPDocumentInfo info = infoFromOpenEditors();
		if (info != null) {
			info.getInitializedLanguageClient().thenAccept(languageServer -> languageServer.getWorkspaceService().didChangeConfiguration(params));
		}
	}

	private static LSPDocumentInfo infoFromOpenEditors() {
		for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
			for (IWorkbenchPage page : window.getPages()) {
				for (IEditorReference editor : page.getEditorReferences()) {
					IEditorInput input;
					try {
						input = editor.getEditorInput();
					} catch (PartInitException e) {
						continue;
					}
					if (input.getName().endsWith(".rs") && editor.getEditor(false) instanceof ITextEditor) { //$NON-NLS-1$
						IDocument document = (((ITextEditor) editor.getEditor(false)).getDocumentProvider()).getDocument(input);
						Collection<LSPDocumentInfo> infos = LanguageServiceAccessor.getLSPDocumentInfosFor(document, capabilities -> Boolean.TRUE.equals(capabilities.getReferencesProvider()));
						if (infos.isEmpty()) {
							continue;
						}
						return infos.iterator().next();
					}
				}
			}
		}
		return null;
	}

	private static void showToolchainSelectionError(String message) {
		Display.getDefault().asyncExec(() -> {
			MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), Messages.RustManager_rootToolchainSelectionFailure, message);
		});
	}

	private static boolean runRustupCommand(SubMonitor monitor, String... arguments) {
		String rustup = STORE.getString(CorrosionPreferenceInitializer.rustupPathPreference);
		if (rustup.isEmpty()) {
			return false;
		}
		try {
			String[] command = new String[arguments.length + 1];
			command[0] = rustup;
			System.arraycopy(arguments, 0, command, 1, arguments.length);
			ProcessBuilder builder = new ProcessBuilder(command);
			builder.inheritIO();
			Process process = builder.start();
			while (process.isAlive() && !monitor.isCanceled()) {
				Thread.sleep(50);
			}
			if (monitor.isCanceled()) {
				process.destroyForcibly();
				return false;
			}

			return process.waitFor() == 0;
		} catch (IOException | InterruptedException e) {
			return false;
		}
	}

	public static List<String> getToolchains() {
		List<String> toolchainsList = new ArrayList<>();
		String rustup = STORE.getString(CorrosionPreferenceInitializer.rustupPathPreference);
		if (!rustup.isEmpty()) {
			try {
				ProcessBuilder builder = new ProcessBuilder(new String[] { rustup, "show" }); //$NON-NLS-1$
				Process process = builder.start();

				if (process.waitFor() == 0) {
					try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
						String line = in.readLine();
						while (line != null && !line.equals("active toolchain")) { //$NON-NLS-1$
							String toolchain = ""; //$NON-NLS-1$
							if (line.matches("^nightly-\\d{4}-\\d{2}-\\d{2}.*$")) { //$NON-NLS-1$
								toolchain = line.substring(0, 18);// "nightly-YYYY-MM-DD".length()==18
							} else if (line.matches("\\w+\\-.*")) { //$NON-NLS-1$
								int splitIndex = line.indexOf('-');
								if (splitIndex != -1) {
									toolchain = line.substring(0, splitIndex);
								}
							}
							if (!toolchain.isEmpty()) {
								toolchainsList.add(toolchain);
							}
							line = in.readLine();
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				// Errors will be caught with empty return
			}
		}
		return toolchainsList;
	}
}
