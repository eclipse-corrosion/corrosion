/*********************************************************************
 * Copyright (c) 2017, 2019 Red Hat Inc. and others.
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
import java.net.URL;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.corrosion.cargo.core.CargoTools;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
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
		Job.create("Import .cargo in workspace", //$NON-NLS-1$
				(ICoreRunnable) (monitor -> CargoTools.ensureDotCargoImportedAsProject(monitor))).schedule();
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
		logError(new Status(IStatus.ERROR, PLUGIN_ID, t.getMessage(), t));
	}

	public static void logError(IStatus s) {
		getDefault().getLog().log(s);
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

	@Override
	protected void initializeImageRegistry(ImageRegistry reg) {
		super.initializeImageRegistry(reg);
		declareRegistryImage(reg, "images/cargo.png"); //$NON-NLS-1$
		declareRegistryImage(reg, "images/cargo16.png"); //$NON-NLS-1$
	}

	private final static void declareRegistryImage(ImageRegistry reg, String image) {
		ImageDescriptor desc = ImageDescriptor.getMissingImageDescriptor();
		URL url = null;
		Bundle bundle = plugin.getBundle();
		if (bundle != null) {
			url = FileLocator.find(bundle, new Path(image), null);
			if (url != null) {
				desc = ImageDescriptor.createFromURL(url);
			}
		}
		reg.put(image, desc);
	}

	public static boolean validateCommandVersion(String[] commandStrings, Pattern matchPattern) {
		String[] command = new String[1 + commandStrings.length];

		System.arraycopy(commandStrings, 0, command, 0, commandStrings.length);
		command[commandStrings.length] = "--version"; //$NON-NLS-1$

		return matchPattern.matcher(getOutputFromCommand(command)).matches();
	}

	public static boolean validateCommandVersion(String commandPath, Pattern matchPattern) {
		return matchPattern.matcher(getOutputFromCommand(commandPath, "--version")).matches(); //$NON-NLS-1$
	}

	public static Process getProcessForCommand(String... commandStrings) throws IOException {
		ProcessBuilder builder = new ProcessBuilder(commandStrings);
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
