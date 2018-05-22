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
package org.eclipse.corrosion.edit;

import java.io.BufferedReader;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.corrosion.CorrosionPreferenceInitializer;
import org.eclipse.corrosion.CorrosionPreferencePage;
import org.eclipse.corrosion.Messages;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.lsp4e.server.StreamConnectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class RLSStreamConnectionProvider implements StreamConnectionProvider {

	private static boolean hasCancelledSetup = false;
	private boolean DEBUG = Boolean.parseBoolean(System.getProperty("rsl.lsp.debug")); //$NON-NLS-1$
	private Process process;

	@Override public void start() throws IOException {
		boolean wereSystemPropertiesSet = setSystemProperties();
		String rls = getRLS();
		if ((rls.isEmpty() || !wereSystemPropertiesSet)) {
			showSetupRustNotification();
			return;
		}
		String[] command = new String[] { "/bin/bash", "-c", rls }; //$NON-NLS-1$ //$NON-NLS-2$
		if (Platform.getOS().equals(Platform.OS_WIN32)) {
			command = new String[] { "cmd", "/c", rls }; //$NON-NLS-1$ //$NON-NLS-2$
		}
		this.process = Runtime.getRuntime().exec(command);
	}

	private boolean setSystemProperties() {
		CorrosionPlugin plugin = CorrosionPlugin.getDefault();
		IPreferenceStore preferenceStore = plugin.getPreferenceStore();
		int rustSourceIndex = CorrosionPreferencePage.RUST_SOURCE_OPTIONS.indexOf(preferenceStore.getString(CorrosionPreferenceInitializer.rustSourcePreference));

		String sysrootPath = ""; //$NON-NLS-1$

		if (rustSourceIndex == 0) {
			String rustup = preferenceStore.getString(CorrosionPreferenceInitializer.rustupPathPreference);
			String toolchain = preferenceStore.getString(CorrosionPreferenceInitializer.toolchainIdPreference);
			if (!(rustup.isEmpty() || toolchain.isEmpty())) {
				String[] command = new String[] { rustup, "run", toolchain, "rustc", "--print", "sysroot" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				try {
					Process process = Runtime.getRuntime().exec(command);
					try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
						sysrootPath = in.readLine();
					}
				} catch (IOException e) {
					// Caught with final return
				}
			}
		} else if (rustSourceIndex == 1) {
			sysrootPath = preferenceStore.getString(CorrosionPreferenceInitializer.sysrootPathPreference);
		}

		if (!sysrootPath.isEmpty()) {
			System.setProperty("SYS_ROOT", sysrootPath); //$NON-NLS-1$
			System.setProperty("LD_LIBRARY_PATH", sysrootPath + "/lib"); //$NON-NLS-1$ //$NON-NLS-2$
			String sysRoot = System.getProperty("SYS_ROOT"); //$NON-NLS-1$
			String ldLibraryPath = System.getProperty("LD_LIBRARY_PATH"); //$NON-NLS-1$
			if (!(sysRoot == null || sysRoot.isEmpty() || ldLibraryPath == null || ldLibraryPath.isEmpty())) {
				return true;
			}
		}
		CorrosionPlugin.getDefault().getLog().log(new Status(IStatus.ERROR, CorrosionPlugin.getDefault().getBundle().getSymbolicName(), Messages.RLSStreamConnectionProvider_unableToSet));
		return false;
	}

	private String getRLS() {
		CorrosionPlugin plugin = CorrosionPlugin.getDefault();
		IPreferenceStore preferenceStore = plugin.getPreferenceStore();
		int rustSourceIndex = CorrosionPreferencePage.RUST_SOURCE_OPTIONS.indexOf(preferenceStore.getString(CorrosionPreferenceInitializer.rustSourcePreference));

		if (rustSourceIndex == 0) {
			String rustup = preferenceStore.getString(CorrosionPreferenceInitializer.rustupPathPreference);
			String toolchain = preferenceStore.getString(CorrosionPreferenceInitializer.toolchainIdPreference);
			if (!(rustup.isEmpty() || toolchain.isEmpty())) {
				return rustup + " run " + toolchain + " rls"; //$NON-NLS-1$ //$NON-NLS-2$
			}
		} else if (rustSourceIndex == 1) {
			String rls = preferenceStore.getString(CorrosionPreferenceInitializer.rlsPathPreference);
			if (!rls.isEmpty()) {
				return rls;
			}
		}

		CorrosionPlugin.getDefault().getLog().log(new Status(IStatus.ERROR, CorrosionPlugin.getDefault().getBundle().getSymbolicName(), Messages.RLSStreamConnectionProvider_rlsNotFound));
		return ""; //$NON-NLS-1$
	}

	private void showSetupRustNotification() {
		if (hasCancelledSetup) {
			return;
		}
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		int dialogResponse = MessageDialog.open(MessageDialog.CONFIRM, shell, Messages.RLSStreamConnectionProvider_rustSupportNotFound, Messages.RLSStreamConnectionProvider_requirementsNotFound, SWT.NONE, Messages.RLSStreamConnectionProvider_OpenPreferences, IDialogConstants.CANCEL_LABEL); // $NON-NLS-4$
		if (dialogResponse == 0) {
			PreferenceDialog preferenceDialog = PreferencesUtil.createPreferenceDialogOn(shell, CorrosionPreferencePage.PAGE_ID, new String[] { CorrosionPreferencePage.PAGE_ID }, null);
			preferenceDialog.setBlockOnOpen(true);
			preferenceDialog.open();
		} else {
			hasCancelledSetup = true;
		}
	}

	@Override public InputStream getInputStream() {
		if (DEBUG) {
			return new FilterInputStream(process.getInputStream()) {
				@Override public int read() throws IOException {
					int res = super.read();
					System.err.print((char) res);
					return res;
				}

				@Override public int read(byte[] b, int off, int len) throws IOException {
					int bytes = super.read(b, off, len);
					byte[] payload = new byte[bytes];
					System.arraycopy(b, off, payload, 0, bytes);
					System.err.print(new String(payload));
					return bytes;
				}

				@Override public int read(byte[] b) throws IOException {
					int bytes = super.read(b);
					byte[] payload = new byte[bytes];
					System.arraycopy(b, 0, payload, 0, bytes);
					System.err.print(new String(payload));
					return bytes;
				}
			};
		} else {
			return process.getInputStream();
		}
	}

	@Override public OutputStream getOutputStream() {
		if (DEBUG) {
			return new FilterOutputStream(process.getOutputStream()) {
				@Override public void write(int b) throws IOException {
					System.err.print((char) b);
					super.write(b);
				}

				@Override public void write(byte[] b) throws IOException {
					System.err.print(new String(b));
					super.write(b);
				}

				@Override public void write(byte[] b, int off, int len) throws IOException {
					byte[] actual = new byte[len];
					System.arraycopy(b, off, actual, 0, len);
					System.err.print(new String(actual));
					super.write(b, off, len);
				}
			};
		} else {
			return process.getOutputStream();
		}
	}

	@Override public void stop() {
		process.destroy();
	}

	@Override public InputStream getErrorStream() {
		if (DEBUG) {
			return new FilterInputStream(process.getErrorStream()) {
				@Override public int read() throws IOException {
					int res = super.read();
					System.err.print((char) res);
					return res;
				}

				@Override public int read(byte[] b, int off, int len) throws IOException {
					int bytes = super.read(b, off, len);
					byte[] payload = new byte[bytes];
					System.arraycopy(b, off, payload, 0, bytes);
					System.err.print(new String(payload));
					return bytes;
				}

				@Override public int read(byte[] b) throws IOException {
					int bytes = super.read(b);
					byte[] payload = new byte[bytes];
					System.arraycopy(b, 0, payload, 0, bytes);
					System.err.print(new String(payload));
					return bytes;
				}
			};
		} else {
			return process.getErrorStream();
		}
	}
}
