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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.core.runtime.Platform;
import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.corrosion.CorrosionPreferencePage;
import org.eclipse.corrosion.Messages;
import org.eclipse.corrosion.RustManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.lsp4e.server.StreamConnectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class RLSStreamConnectionProvider implements StreamConnectionProvider {

	private static boolean hasCancelledSetup = false;
	private Process process;

	@Override public void start() throws IOException {
		String rls = RustManager.getRLS();
		if ((rls.isEmpty() || !RustManager.setSystemProperties()
				|| !CorrosionPlugin.validateCommandVersion(rls, RustManager.RLS_VERSION_FORMAT_PATTERN))) {
			showSetupRustNotification();
			return;
		}
		String[] command = new String[] { "/bin/bash", "-c", rls }; //$NON-NLS-1$ //$NON-NLS-2$
		if (Platform.getOS().equals(Platform.OS_WIN32)) {
			command = new String[] { "cmd", "/c", rls }; //$NON-NLS-1$ //$NON-NLS-2$
		}
		this.process = Runtime.getRuntime().exec(command);
	}

	private void showSetupRustNotification() {
		Display.getDefault().asyncExec(() -> {
			if (hasCancelledSetup) {
				return;
			}
			setHasCancelledSetup(true);
			Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
			int dialogResponse = MessageDialog.open(MessageDialog.CONFIRM, shell,
					Messages.RLSStreamConnectionProvider_rustSupportNotFound,
					Messages.RLSStreamConnectionProvider_requirementsNotFound, SWT.NONE,
					Messages.RLSStreamConnectionProvider_OpenPreferences, IDialogConstants.CANCEL_LABEL); // $NON-NLS-4$
			if (dialogResponse == 0) {
				PreferenceDialog preferenceDialog = PreferencesUtil.createPreferenceDialogOn(shell,
						CorrosionPreferencePage.PAGE_ID, new String[] { CorrosionPreferencePage.PAGE_ID }, null);
				preferenceDialog.setBlockOnOpen(true);
				preferenceDialog.open();
				setHasCancelledSetup(false);
			}
		});
	}

	private static synchronized void setHasCancelledSetup(Boolean newValue) {
		hasCancelledSetup = newValue;
	}

	@Override public InputStream getInputStream() {
		return process.getInputStream();
	}

	@Override public OutputStream getOutputStream() {
		return process.getOutputStream();
	}

	@Override public void stop() {
		process.destroy();
	}

	@Override public InputStream getErrorStream() {
		return process.getErrorStream();
	}
}
