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
 *  Nicola Orru - Added support for external RLS startup configuration
 *******************************************************************************/
package org.eclipse.corrosion;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.DoubleConsumer;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ecf.filetransfer.IFileTransferListener;
import org.eclipse.ecf.filetransfer.events.IIncomingFileTransferReceiveDataEvent;
import org.eclipse.ecf.filetransfer.events.IIncomingFileTransferReceiveDoneEvent;
import org.eclipse.ecf.filetransfer.events.IIncomingFileTransferReceiveStartEvent;
import org.eclipse.ecf.filetransfer.identity.FileIDFactory;
import org.eclipse.ecf.filetransfer.identity.IFileID;
import org.eclipse.ecf.filetransfer.service.IRetrieveFileTransfer;
import org.eclipse.ecf.filetransfer.service.IRetrieveFileTransferFactory;
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
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

@SuppressWarnings("restriction")
public class RustManager {
	private static final IPreferenceStore STORE = CorrosionPlugin.getDefault().getPreferenceStore();
	/**
	 * Set according to rust-analyzer documentation
	 */
	static final File RUST_ANALYZER_DEFAULT_LOCATION = new File(System.getProperty("user.home"), //$NON-NLS-1$
			".local/bin/rust-analyzer"); //$NON-NLS-1$
	public static final String RLS_VERSION_FORMAT_REGEX = "^(rls|rust-analyzer).*$"; //$NON-NLS-1$
	public static final String CARGO_VERSION_FORMAT_REGEX = "^cargo .*$"; //$NON-NLS-1$
	public static final String RUSTUP_VERSION_FORMAT_REGEX = "^rustup .*$"; //$NON-NLS-1$
	public static final Pattern RLS_VERSION_FORMAT_PATTERN = Pattern.compile(RLS_VERSION_FORMAT_REGEX);
	public static final Pattern CARGO_VERSION_FORMAT_PATTERN = Pattern.compile(CARGO_VERSION_FORMAT_REGEX);
	public static final Pattern RUSTUP_VERSION_FORMAT_PATTERN = Pattern.compile(RUSTUP_VERSION_FORMAT_REGEX);

	private RustManager() {
		throw new IllegalStateException("Utility class"); //$NON-NLS-1$
	}

	/**
	 * Returns the default GDB location set in the preferences. This is most
	 * interesting for debug launch configurations to set the default debugger.
	 *
	 * @return GDB location from preferences
	 */
	public static String getDefaultDebugger() {
		return STORE.getString(CorrosionPreferenceInitializer.DEFAULT_GDB_PREFERENCE);
	}

	public static String getDefaultToolchain() {
		String rustup = STORE.getString(CorrosionPreferenceInitializer.RUSTUP_PATHS_PREFERENCE);
		final String emptyResult = ""; //$NON-NLS-1$
		if (rustup.isEmpty()) {
			return emptyResult;
		}

		try {
			Process process = CorrosionPlugin.getProcessForCommand(rustup, "show"); //$NON-NLS-1$
			if (process.waitFor() != 0) {
				return emptyResult;
			}
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
		} catch (IOException e) {
			CorrosionPlugin.logError(e);
			return emptyResult;
		} catch (InterruptedException e) {
			CorrosionPlugin.logError(e);
			Thread.currentThread().interrupt();
		}
		return emptyResult;
	}

	private static Job settingToolchainJob = null;

	public static synchronized Job setDefaultToolchain(String toolchainId) {
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
		return settingToolchainJob;
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

	public static CommandJob createRustupCommandJob(String progressMessage, String errorMessage, String... arguments) {
		String rustup = STORE.getString(CorrosionPreferenceInitializer.RUSTUP_PATHS_PREFERENCE);
		if (rustup.isEmpty()) {
			return null;
		}
		String[] command = new String[arguments.length + 1];
		command[0] = rustup;
		System.arraycopy(arguments, 0, command, 1, arguments.length);
		return new CommandJob(command, progressMessage, Messages.RustManager_rootToolchainSelectionFailure,
				errorMessage, 0);
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
						IDocument document = (((ITextEditor) editor.getEditor(false)).getDocumentProvider())
								.getDocument(input);
						Collection<LSPDocumentInfo> infos = LanguageServiceAccessor.getLSPDocumentInfosFor(document,
								capabilities -> Boolean.TRUE.equals(capabilities.getReferencesProvider()));
						if (!infos.isEmpty()) {
							return infos.iterator().next();
						}
					}
				}
			}
		}
		return null;
	}

	public static List<String> getToolchains() {
		List<String> toolchainsList = new ArrayList<>();
		String rustup = STORE.getString(CorrosionPreferenceInitializer.RUSTUP_PATHS_PREFERENCE);
		if (rustup.isEmpty()) {
			return toolchainsList;
		}
		try {
			Process process = CorrosionPlugin.getProcessForCommand(rustup, "show"); //$NON-NLS-1$

			if (process.waitFor() != 0) {
				return toolchainsList;
			}
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
		} catch (Exception e) { // Errors will be caught with empty return
			CorrosionPlugin.logError(e);
		}
		return toolchainsList;
	}

	public static boolean setSystemProperties() {
		CorrosionPlugin plugin = CorrosionPlugin.getDefault();
		IPreferenceStore preferenceStore = plugin.getPreferenceStore();

		String sysrootPath = preferenceStore.getString(CorrosionPreferenceInitializer.SYSROOT_PATH_PREFERENCE);
		if (sysrootPath == null || sysrootPath.isBlank()) {
			String rustup = preferenceStore.getString(CorrosionPreferenceInitializer.RUSTUP_PATHS_PREFERENCE);
			String toolchain = preferenceStore.getString(CorrosionPreferenceInitializer.TOOLCHAIN_ID_PREFERENCE);
			if (!(rustup.isEmpty() || toolchain.isEmpty())) {
				String[] command = new String[] { rustup, "run", toolchain, "rustc", "--print", "sysroot" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				sysrootPath = CorrosionPlugin.getOutputFromCommand(command);
			}
		}

		if (sysrootPath != null && !sysrootPath.isEmpty()) {
			System.setProperty("SYS_ROOT", sysrootPath); //$NON-NLS-1$
			System.setProperty("LD_LIBRARY_PATH", sysrootPath + "/lib"); //$NON-NLS-1$ //$NON-NLS-2$
			String sysRoot = System.getProperty("SYS_ROOT"); //$NON-NLS-1$
			String ldLibraryPath = System.getProperty("LD_LIBRARY_PATH"); //$NON-NLS-1$
			if (!(sysRoot == null || sysRoot.isEmpty() || ldLibraryPath == null || ldLibraryPath.isEmpty())) {
				return true;
			}
		}
		CorrosionPlugin.getDefault().getLog()
				.log(new Status(IStatus.ERROR, CorrosionPlugin.getDefault().getBundle().getSymbolicName(),
						Messages.RLSStreamConnectionProvider_unableToSet));
		return false;
	}

	public static File getLanguageServerConfiguration() {
		CorrosionPlugin plugin = CorrosionPlugin.getDefault();
		IPreferenceStore preferenceStore = plugin.getPreferenceStore();
		String preferencePath = preferenceStore
				.getString(CorrosionPreferenceInitializer.RLS_CONFIGURATION_PATH_PREFERENCE);
		if (preferencePath.isEmpty()) {
			CorrosionPlugin.getDefault().getLog()
					.log(new Status(IStatus.WARNING, CorrosionPlugin.getDefault().getBundle().getSymbolicName(),
							Messages.RLSStreamConnectionProvider_rlsConfigurationNotSet));
			return null;
		}
		return new File(preferencePath);
	}

	public static File getLanguageServerExecutable() {
		CorrosionPlugin plugin = CorrosionPlugin.getDefault();
		IPreferenceStore preferenceStore = plugin.getPreferenceStore();
		String rlsPath = preferenceStore.getString(CorrosionPreferenceInitializer.RLS_PATH_PREFERENCE);
		if (rlsPath.isEmpty()) {
			CorrosionPlugin.getDefault().getLog()
					.log(new Status(IStatus.ERROR, CorrosionPlugin.getDefault().getBundle().getSymbolicName(),
							Messages.RLSStreamConnectionProvider_rlsNotFound));
			return null;
		}
		return new File(rlsPath);
	}

	public static CompletableFuture<File> downloadAndInstallRustAnalyzer(DoubleConsumer progressConsumer) {
		if (!RUST_ANALYZER_DEFAULT_LOCATION.getParentFile().exists()) {
			RUST_ANALYZER_DEFAULT_LOCATION.getParentFile().mkdirs();
		}
		String filename = "rust-analyzer-" + //$NON-NLS-1$
				(Platform.OS_LINUX.equals(Platform.getOS()) ? "linux" : //$NON-NLS-1$
						Platform.OS_WIN32.equals(Platform.getOS()) ? "win.exe" : //$NON-NLS-1$
								Platform.OS_MACOSX.equals(Platform.getOS()) ? "mac" : "os-not-found"); //$NON-NLS-1$ //$NON-NLS-2$
		String url = "https://github.com/rust-analyzer/rust-analyzer/releases/latest/download/" + filename; //$NON-NLS-1$

		BundleContext bundleContext = CorrosionPlugin.getDefault().getBundle().getBundleContext();
		ServiceReference<IRetrieveFileTransferFactory> ref = bundleContext
				.getServiceReference(IRetrieveFileTransferFactory.class);
		IRetrieveFileTransferFactory transferFactory = bundleContext.getService(ref);
		IRetrieveFileTransfer retrieve = transferFactory.newInstance();
		// Use retrieve to initiate file transfer
		try {
			CompletableFuture<File> res = new CompletableFuture<>();
			IFileID id = FileIDFactory.getDefault().createFileID(retrieve.getRetrieveNamespace(), URI.create(url));
			IFileTransferListener listener = event -> {
				if (event instanceof IIncomingFileTransferReceiveStartEvent) {
					IIncomingFileTransferReceiveStartEvent rse = (IIncomingFileTransferReceiveStartEvent) event;
					try {
						rse.receive(RUST_ANALYZER_DEFAULT_LOCATION);
					} catch (IOException e) {
						res.completeExceptionally(e);
					}
				} else if (event instanceof IIncomingFileTransferReceiveDataEvent) {
					progressConsumer
							.accept(((IIncomingFileTransferReceiveDataEvent) event).getSource().getPercentComplete());
				} else if (event instanceof IIncomingFileTransferReceiveDoneEvent) {
					RUST_ANALYZER_DEFAULT_LOCATION.setExecutable(true);
					res.complete(RUST_ANALYZER_DEFAULT_LOCATION);
				}
			};
			retrieve.sendRetrieveRequest(id, listener, Map.of());
			return res;
		} catch (Exception e) {
			return CompletableFuture.failedFuture(e);
		}
	}
}
