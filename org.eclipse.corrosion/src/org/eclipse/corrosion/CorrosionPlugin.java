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
package org.eclipse.corrosion;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class CorrosionPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.corrosion"; //$NON-NLS-1$

	// The shared instance
	private static CorrosionPlugin plugin;

	private static synchronized void setSharedInstance(CorrosionPlugin newValue) {
		plugin = newValue;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		setSharedInstance(this);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		setSharedInstance(null);
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static CorrosionPlugin getDefault() {
		return plugin;
	}

	public static void logError(Throwable t) {
		getDefault().getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, t.getMessage(), t));
	}

	public static void showError(String title, String message, Exception exception) {
		CorrosionPlugin.showError(title, message + '\n' + exception.getLocalizedMessage());
	}

	public static void showError(String title, String message) {
		Display.getDefault().asyncExec(() -> {
			MessageDialog dialog = new MessageDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
					title, null, message, MessageDialog.ERROR, 0, IDialogConstants.OK_LABEL);
			dialog.setBlockOnOpen(false);
			dialog.open();
		});
	}

	public static boolean validateCommandVersion(String commandPath, Pattern matchPattern) {
		return matchPattern.matcher(getOutputFromCommand(commandPath + " --version")).matches(); //$NON-NLS-1$
	}

	public static Process getProcessForCommand(String... commandStrings) throws IOException {
		String[] command = new String[3];
		if (Platform.getOS().equals(Platform.OS_WIN32)) {
			command[0] = "cmd"; //$NON-NLS-1$
			command[1] = "/c"; //$NON-NLS-1$
		} else {
			command[0] = "/bin/bash"; //$NON-NLS-1$
			command[1] = "-c"; //$NON-NLS-1$
		}
		command[2] = String.join(" ", commandStrings); //$NON-NLS-1$
		ProcessBuilder builder = new ProcessBuilder(command);
		builder.directory(getWorkingDirectoryFromPreferences());
		return builder.start();
	}

	private static File getWorkingDirectoryFromPreferences() {
		String wdString = getDefault().getPreferenceStore()
				.getString(CorrosionPreferenceInitializer.WORKING_DIRECTORY_PREFERENCE);
		if (wdString == null) {
			return null;
		}
		File wdFile = new File(wdString);
		if (wdFile.exists() && wdFile.isDirectory()) {
			return wdFile;
		}
		return null;
	}

	public static String getOutputFromCommand(String... commandStrings) {
		try {
			Process process = getProcessForCommand(commandStrings);
			if (process.waitFor() == 0) {
				try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
					return in.readLine();
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (IOException e) {
			// Error will be caught with empty response
		}
		return ""; //$NON-NLS-1$
	}
}
