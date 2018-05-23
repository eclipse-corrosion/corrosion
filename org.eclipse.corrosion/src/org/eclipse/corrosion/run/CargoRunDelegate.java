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
package org.eclipse.corrosion.run;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.corrosion.Messages;
import org.eclipse.corrosion.cargo.core.CargoTools;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

public class CargoRunDelegate extends LaunchConfigurationDelegate implements ILaunchShortcut {

	public static final String PROJECT_ATTRIBUTE = "PROJECT"; //$NON-NLS-1$
	public static final String RUN_OPTIONS_ATTRIBUTE = "RUN_OPTIONS"; //$NON-NLS-1$
	public static final String RUN_ARGUMENTS_ATTRIBUTE = "RUN_ARGUMENTS"; //$NON-NLS-1$

	@Override public void launch(ISelection selection, String mode) {

		if (selection instanceof IStructuredSelection) {
			Iterator<?> selectionIterator = ((IStructuredSelection) selection).iterator();
			while (selectionIterator.hasNext()) {
				Object element = selectionIterator.next();
				IResource resource = null;
				if (element instanceof IResource) {
					resource = (IResource) element;
				} else if (element instanceof IAdaptable) {
					resource = ((IAdaptable) element).getAdapter(IResource.class);
				}

				if (resource != null) {
					try {
						ILaunchConfiguration launchConfig = getLaunchConfiguration(mode, resource);
						if (launchConfig != null) {
							launchConfig.launch(mode, new NullProgressMonitor());
						}
					} catch (CoreException e) {
						CorrosionPlugin.logError(e);
					}
					return;
				}
			}
		}
		openError(Messages.CargoRunDelegate_unableToLaunch, Messages.CargoRunDelegate_unableToFindProject);
	}

	@Override public void launch(IEditorPart editor, String mode) {
		IEditorInput input = editor.getEditorInput();
		IFile file = input.getAdapter(IFile.class);

		try {
			ILaunchConfiguration launchConfig = getLaunchConfiguration(mode, file);
			if (launchConfig != null) {
				launchConfig.launch(mode, new NullProgressMonitor());
			}
		} catch (CoreException e) {
			CorrosionPlugin.logError(e);
		}
	}

	@Override public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		List<String> cargoRunCommand = new ArrayList<>();
		cargoRunCommand.add(CargoTools.getCargoCommand());
		cargoRunCommand.add("run"); //$NON-NLS-1$
		String projectName = configuration.getAttribute(PROJECT_ATTRIBUTE, ""); //$NON-NLS-1$
		String options = configuration.getAttribute(RUN_OPTIONS_ATTRIBUTE, "").trim(); //$NON-NLS-1$
		String arguments = configuration.getAttribute(RUN_ARGUMENTS_ATTRIBUTE, "").trim(); //$NON-NLS-1$

		IProject project = null;
		if (!projectName.isEmpty()) {
			project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		}
		if (project == null || !project.exists()) {
			openError(Messages.CargoRunDelegate_unableToLaunch, Messages.CargoRunDelegate_unableToFindProject);
			return;
		}
		IFile cargoManifest = project.getFile("Cargo.toml"); //$NON-NLS-1$
		if (!cargoManifest.exists()) {
			openError(Messages.CargoRunDelegate_unableToLaunch, Messages.CargoRunDelegate_unableToFindToml);
			return;
		}
		IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
		if (!options.isEmpty()) {
			cargoRunCommand.addAll(Arrays.asList(manager.performStringSubstitution(options).split("\\s+"))); //$NON-NLS-1$
		}

		final String cargoPathString = cargoManifest.getLocation().toPortableString();
		cargoRunCommand.add("--manifest-path"); //$NON-NLS-1$
		cargoRunCommand.add(cargoPathString);

		if (!arguments.isEmpty()) {
			cargoRunCommand.add("--"); //$NON-NLS-1$
			cargoRunCommand.addAll(Arrays.asList(manager.performStringSubstitution(arguments).split("\\s+"))); //$NON-NLS-1$
		}

		final List<String> finalRunCommand = cargoRunCommand;
		CompletableFuture.runAsync(() -> {
			try {
				String[] cmdLine = finalRunCommand.toArray(new String[finalRunCommand.size()]);
				Process p = DebugPlugin.exec(cmdLine, null);
				IProcess process = DebugPlugin.newProcess(launch, p, "cargo run"); //$NON-NLS-1$
				process.setAttribute(IProcess.ATTR_CMDLINE, String.join(" ", cmdLine)); //$NON-NLS-1$
			} catch (CoreException e) {
				openError(Messages.CargoRunDelegate_unableToLaunch, e.getLocalizedMessage());
			}
		});
	}

	private ILaunchConfiguration getLaunchConfiguration(String mode, IResource resource) {
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType configType = launchManager.getLaunchConfigurationType("org.eclipse.corrosion.run.CargoRunDelegate"); //$NON-NLS-1$
		try {
			ILaunchConfiguration[] launchConfigurations = launchManager.getLaunchConfigurations(configType);
			final String projectName = resource.getProject().getName();

			for (ILaunchConfiguration iLaunchConfiguration : launchConfigurations) {
				if (iLaunchConfiguration.getAttribute(PROJECT_ATTRIBUTE, "").equals(projectName)) { //$NON-NLS-1$
					return iLaunchConfiguration;
				}
			}
			String configName = launchManager.generateLaunchConfigurationName(projectName);
			ILaunchConfigurationWorkingCopy wc = configType.newInstance(null, configName);
			wc.setAttribute(PROJECT_ATTRIBUTE, projectName);
			return wc;
		} catch (CoreException e) {
			CorrosionPlugin.logError(e);
		}
		return null;
	}

	private void openError(String title, String message) {
		Display.getDefault().asyncExec(() -> {
			MessageDialog dialog = new MessageDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
					title, null, message, MessageDialog.ERROR, 0, IDialogConstants.OK_LABEL);
			dialog.setBlockOnOpen(false);
			dialog.open();
		});
	}
}
