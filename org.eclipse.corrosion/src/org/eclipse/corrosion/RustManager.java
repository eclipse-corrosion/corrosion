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
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.lsp4e.LanguageServiceAccessor;
import org.eclipse.lsp4e.LanguageServiceAccessor.LSPDocumentInfo;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.osgi.util.NLS;
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
		settingToolchainJob = new Job(Messages.RustManager_settingRLSToolchain) {
			CommandJob currentCommandJob;

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				List<CommandJob> jobs = new ArrayList<>();
				jobs.add(createRustupCommandJob(Messages.RustManager_installingToolchain,
						NLS.bind(Messages.RustManager_unableToInstallToolchain, toolchainId), "toolchain", "install", //$NON-NLS-1$ //$NON-NLS-2$
						toolchainId));
				jobs.add(createRustupCommandJob(Messages.RustManager_settingDefaultToolchain,
						NLS.bind(Messages.RustManager_unableToSetDefaultToolchain, toolchainId), "default", //$NON-NLS-1$
						toolchainId));
				jobs.add(createRustupCommandJob(Messages.RustManager_addingRLSPrevios,
						NLS.bind(Messages.RustManager_toolchainDoesntIncludeRLS, toolchainId), "component", "add", //$NON-NLS-1$ //$NON-NLS-2$
						"rls-preview")); //$NON-NLS-1$
				jobs.add(createRustupCommandJob(Messages.RustManager_addingRustAnalysisRustSrc,
						Messages.RustManager_unableToAddComponent, "component", //$NON-NLS-1$
						"add", "rust-analysis")); //$NON-NLS-1$ //$NON-NLS-2$

				for (CommandJob commandJob : jobs) {
					currentCommandJob = commandJob;
					if (currentCommandJob.run(monitor) == Status.CANCEL_STATUS) {
						return Status.CANCEL_STATUS;
					}
					monitor.worked(1);
				}
				Map<String, String> updatedSettings = new HashMap<>();
				updatedSettings.put("target", toolchainId); //$NON-NLS-1$
				sendDidChangeConfigurationsMessage(updatedSettings);
				return Status.OK_STATUS;
			}

			@Override
			protected void canceling() {
				if (currentCommandJob != null) {
					currentCommandJob.cancel();
				}
			}
		};
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

	private static CommandJob createRustupCommandJob(String progressMessage, String errorMessage, String... arguments) {
		String rustup = STORE.getString(CorrosionPreferenceInitializer.rustupPathPreference);
		if (rustup.isEmpty()) {
			return null;
		}
		String[] command = new String[arguments.length + 1];
		command[0] = rustup;
		System.arraycopy(arguments, 0, command, 1, arguments.length);
		CommandJob commandJob = new CommandJob(command, progressMessage,
				Messages.RustManager_rootToolchainSelectionFailure, errorMessage, 0);
		return commandJob;
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
