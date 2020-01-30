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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.corrosion.CorrosionPreferencePage;
import org.eclipse.corrosion.Messages;
import org.eclipse.corrosion.RustManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.lsp4e.server.StreamConnectionProvider;
import org.eclipse.lsp4j.jsonrpc.messages.Message;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

public class RLSStreamConnectionProvider implements StreamConnectionProvider {

	private static boolean hasCancelledSetup = false;
	private Process process;

	@Override
	public void start() throws IOException {
		String rls = RustManager.getRLS();
		if ((rls.isEmpty() || !RustManager.setSystemProperties()
				|| !CorrosionPlugin.validateCommandVersion(rls, RustManager.RLS_VERSION_FORMAT_PATTERN))) {
			showSetupRustNotification();
			return;
		}
		this.process = CorrosionPlugin.getProcessForCommand(rls);
	}

	private static void showSetupRustNotification() {
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

	@Override
	public void handleMessage(Message message, LanguageServer languageServer, URI rootURI) {
		// System.out.println(message);
	}

	private static Map<String, Object> getDefaultInitializationOptions() {
		final Map<String, Object> initializationOptions = new HashMap<>();
		final Map<String, Object> rustInitializationSettings = new HashMap<>();
		rustInitializationSettings.put("clippy_preference", "on"); //$NON-NLS-1$//$NON-NLS-2$
		initializationOptions.put("settings", Collections.singletonMap("rust", rustInitializationSettings)); //$NON-NLS-1$ //$NON-NLS-2$
		initializationOptions.put("cmdRun", true); //$NON-NLS-1$
		return initializationOptions;
	}

	@Override
	public Object getInitializationOptions(URI rootUri) {
		final String settingsPath = RustManager.getRlsConfigurationPath();
		if (settingsPath != null && !settingsPath.isEmpty()) {
			final File settingsFile = new File(settingsPath);
			final Gson gson = new Gson();
			try (JsonReader reader = new JsonReader(new FileReader(settingsFile))) {
				return gson.fromJson(reader, HashMap.class);
			} catch (FileNotFoundException e) {
				CorrosionPlugin.getDefault().getLog()
						.log(new Status(IStatus.INFO, CorrosionPlugin.getDefault().getBundle().getSymbolicName(),
								MessageFormat.format(Messages.RLSStreamConnectionProvider_rlsConfigurationNotFound,
										settingsPath)));
			} catch (Throwable e) {
				CorrosionPlugin.getDefault().getLog()
						.log(new Status(IStatus.ERROR, CorrosionPlugin.getDefault().getBundle().getSymbolicName(),
								MessageFormat.format(Messages.RLSStreamConnectionProvider_rlsConfigurationError,
										settingsPath, e)));
			}
		}
		return getDefaultInitializationOptions();
	}

	@Override
	public InputStream getInputStream() {
		return process == null ? null : process.getInputStream();
	}

	@Override
	public OutputStream getOutputStream() {
		return process == null ? null : process.getOutputStream();
	}

	@Override
	public void stop() {
		if (process != null)
			process.destroy();
	}

	@Override
	public InputStream getErrorStream() {
		return process == null ? null : process.getErrorStream();
	}
}
