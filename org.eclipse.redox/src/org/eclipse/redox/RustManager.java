/*********************************************************************
 * Copyright (c) 2017 Red Hat Inc. and others.
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
package org.eclipse.redox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.lsp4e.LanguageServiceAccessor;
import org.eclipse.lsp4e.LanguageServiceAccessor.LSPDocumentInfo;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
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
	private static final IPreferenceStore STORE = RedoxPlugin.getDefault().getPreferenceStore();

	public static String getDefaultToolchain() {
		String rustup = STORE.getString(RedoxPreferenceInitializer.rustupPathPreference);
		if (!rustup.isEmpty()) {
			try {
				ProcessBuilder builder = new ProcessBuilder(new String[] { rustup, "show" });
				Process process = builder.start();

				if (process.waitFor() == 0) {
					try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
						String line = in.readLine();
						while (line != null) {
							if (line.matches("^.*\\(default\\)$")) {
								if (line.matches("^nightly-\\d{4}-\\d{2}-\\d{2}.*$")) {
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
		return "";
	}

	public static void setDefaultToolchain(String toolchainId) {
		Job.create("Setting RLS Toolchain", (ICoreRunnable) monitor -> {
			monitor.beginTask("Installing Toolchain", 5);
			monitor.worked(1);
			if (!runRustupCommand("toolchain", "install", toolchainId)) {
				showToolchainSelectionError("Unable to install toolchain `" + toolchainId
						+ "`. Ensure the `rustup` command path is correct and that `" + toolchainId
						+ "` is a valid toolchain ID.");
				return;
			}
			monitor.subTask("Setting default toolchain");
			monitor.worked(1);
			if (!runRustupCommand("default", toolchainId)) {
				showToolchainSelectionError("Unable to set `" + toolchainId + "` as the default toolchain");
				return;
			}
			monitor.subTask("Adding `rls-preview` component");
			monitor.worked(1);
			if (!runRustupCommand("component", "add", "rls-preview")) {
				showToolchainSelectionError("The toolchain `" + toolchainId
						+ "` does not contain the Rust Language Server, please select a different toolchain");
				return;
			}
			monitor.subTask("Adding `rust-analysis` and `rust-src` components");
			monitor.worked(1);
			if (!runRustupCommand("component", "add", "rust-analysis")
					|| !runRustupCommand("component", "add", "rust-src")) {
				showToolchainSelectionError("Unable to add required components, please select a different toolchain");
				return;
			}
			Map<String, String> updatedSettings = new HashMap<>();
			updatedSettings.put("target", toolchainId);

			sendDidChangeConfigurationsMessage(updatedSettings);
		}).schedule();
	}

	private static void sendDidChangeConfigurationsMessage(Map<String, String> updatedSettings) {
		DidChangeConfigurationParams params = new DidChangeConfigurationParams();
		params.setSettings(updatedSettings);
		LSPDocumentInfo info = infoFromOpenEditors();
		if (info != null) {
			info.getInitializedLanguageClient()
					.thenAccept(languageServer -> languageServer.getWorkspaceService().didChangeConfiguration(params));
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
					if (input.getName().endsWith(".rs") && editor.getEditor(false) instanceof ITextEditor) {
						IDocument document = (((ITextEditor) editor.getEditor(false)).getDocumentProvider())
								.getDocument(input);
						Collection<LSPDocumentInfo> infos = LanguageServiceAccessor.getLSPDocumentInfosFor(document,
								capabilities -> Boolean.TRUE.equals(capabilities.getReferencesProvider()));
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
			MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
					"Rust Toolchain Selection Failure", message);
		});
	}

	private static boolean runRustupCommand(String... arguments) {
		String rustup = STORE.getString(RedoxPreferenceInitializer.rustupPathPreference);
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
			return process.waitFor() == 0;
		} catch (IOException | InterruptedException e) {
			return false;
		}
	}

	public static List<String> getToolchains() {
		List<String> toolchainsList = new ArrayList<>();
		String rustup = STORE.getString(RedoxPreferenceInitializer.rustupPathPreference);
		if (!rustup.isEmpty()) {
			try {
				ProcessBuilder builder = new ProcessBuilder(new String[] { rustup, "show" });
				Process process = builder.start();

				if (process.waitFor() == 0) {
					try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
						String line = in.readLine();
						while (line != null && !line.equals("active toolchain")) {
							String toolchain = "";
							if (line.matches("^nightly-\\d{4}-\\d{2}-\\d{2}.*$")) {
								toolchain = line.substring(0, 18);// "nightly-YYYY-MM-DD".length()==18
							} else if (line.matches("\\w+\\-.*")) {
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
