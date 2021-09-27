/*********************************************************************
 * Copyright (c) 2017,2021 Red Hat Inc. and others.
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.corrosion.CommandJob;
import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.corrosion.Messages;
import org.eclipse.corrosion.cargo.core.CargoTools;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
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
		setWindowTitle(Messages.CargoExportWizard_title);

		Iterator<?> selectionIterator = selection.iterator();
		IProject project = null;

		while (selectionIterator.hasNext() && project == null) {
			IResource resource = (IResource) selectionIterator.next();
			if (resource.getProject().getFile("Cargo.toml").exists()) { //$NON-NLS-1$
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
		boolean noVerify = wizardPage.noVerify();
		boolean noMetadata = wizardPage.noMetadata();
		boolean allowDirty = wizardPage.allowDirty();

		List<String> exportCommandList = new ArrayList<>();
		exportCommandList.add(CargoTools.getCargoCommand());
		exportCommandList.add("package"); //$NON-NLS-1$
		if (noVerify) {
			exportCommandList.add("--no-verify"); //$NON-NLS-1$
		}
		if (noMetadata) {
			exportCommandList.add("--no-metadata"); //$NON-NLS-1$
		}
		if (allowDirty) {
			exportCommandList.add("--allow-dirty"); //$NON-NLS-1$
		}
		if (!toolchain.isEmpty()) {
			exportCommandList.add("--target"); //$NON-NLS-1$
			exportCommandList.add(toolchain);
		}
		exportCommandList.add("--manifest-path"); //$NON-NLS-1$
		exportCommandList.add(project.getFile("Cargo.toml").getLocation().toString()); //$NON-NLS-1$

		CommandJob packageCommandJob = new CommandJob(exportCommandList.toArray(new String[exportCommandList.size()]),
				"Cargo Package", //$NON-NLS-1$
				Messages.CargoExportWizard_cannotCreateProject, Messages.CargoExportWizard_cannotCreateProject_details,
				0);
		packageCommandJob.setUser(true);
		packageCommandJob.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(final IJobChangeEvent event) {
				if (event.getResult() == Status.OK_STATUS) {
					try {
						project.refreshLocal(IResource.DEPTH_INFINITE, null);
					} catch (CoreException e) {
						CorrosionPlugin.logError(e);
					}
				}
			}
		});
		packageCommandJob.schedule();
		return true;
	}
}
