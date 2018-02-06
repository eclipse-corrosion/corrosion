/*********************************************************************
 * Copyright (c) 2017,2018 Red Hat Inc. and others.
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
package org.eclipse.corrosion.wizards.export;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.corrosion.CorrosionPreferenceInitializer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

public class CargoExportWizard extends Wizard implements IExportWizard {
	private CargoExportWizardPage wizardPage;

	public CargoExportWizard() {
		super();
		setNeedsProgressMonitor(true);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle("Package Cargo Based Rust Project");

		Iterator<?> selectionIterator = selection.iterator();
		IProject project = null;

		while (selectionIterator.hasNext() && project == null) {
			IResource resource = (IResource) selectionIterator.next();
			if (resource.getProject().getFile("Cargo.toml").exists()) {
				project = resource.getProject();
			}
		}
		wizardPage = new CargoExportWizardPage(project);
	}

	@Override
	public void addPages() {
		addPage(wizardPage);
	}

	@Override
	public boolean performFinish() {
		IProject project = wizardPage.getProject();
		String toolchain = wizardPage.getToolchain();
		Boolean noVerify = wizardPage.noVerify();
		Boolean noMetadata = wizardPage.noMetadata();
		Boolean allowDirty = wizardPage.allowDirty();

		IPreferenceStore store = CorrosionPlugin.getDefault().getPreferenceStore();
		String cargo = store.getString(CorrosionPreferenceInitializer.cargoPathPreference);

		List<String> exportCommandList = new ArrayList<>();
		exportCommandList.add(cargo);
		exportCommandList.add("package");
		if (noVerify) {
			exportCommandList.add("--no-verify");
		}
		if (noMetadata) {
			exportCommandList.add("--no-metadata");
		}
		if (allowDirty) {
			exportCommandList.add("--allow-dirty");
		}
		if (!toolchain.isEmpty()) {
			exportCommandList.add("--target");
			exportCommandList.add(toolchain);
		}
		exportCommandList.add("--manifest-path");
		exportCommandList.add(project.getFile("Cargo.toml").getLocation().toString());

		Job.create("Cargo Package", (ICoreRunnable) monitor -> {
			try {
				ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
				ILaunch newLaunch = new Launch(null, ILaunchManager.RUN_MODE, null);

				Process packageProcess = DebugPlugin
						.exec(exportCommandList.toArray(new String[exportCommandList.size()]), null);
				DebugPlugin.newProcess(newLaunch, packageProcess, "cargo package");
				launchManager.addLaunch(newLaunch);

				try {
					packageProcess.waitFor();
				} catch (InterruptedException e) { // errors will be shown in console
				}
				if (packageProcess.exitValue() == 0) {
					project.refreshLocal(IResource.DEPTH_INFINITE, null);
				} else {
					String errorOutput = "";
					try (BufferedReader in = new BufferedReader(
							new InputStreamReader(packageProcess.getErrorStream()))) {
						String errorLine;
						while ((errorLine = in.readLine()) != null) {
							errorOutput += errorLine + "\n";
						}
					} catch (IOException e) {
						errorOutput = "Unable to generate error log.";
					}
					final String finalErrorOutput = errorOutput;
					Display.getDefault().asyncExec(() -> {
						MessageDialog.openError(getShell(), "Cannot Create Rust Project", "Create unsuccessful.`"
								+ String.join(" ", exportCommandList) + "` error log:\n\n" + finalErrorOutput);
					});
				}

			} catch (CoreException e) {
				Display.getDefault().asyncExec(() -> {
					MessageDialog.openError(getShell(), "Cannot Package Cargo Based Rust Project",
							"The '" + String.join(" ", exportCommandList) + "' command failed: " + e);
				});
			}
		}).schedule();
		return true;
	}
}
