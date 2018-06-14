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
package org.eclipse.corrosion.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.corrosion.Messages;
import org.eclipse.corrosion.cargo.core.CargoTools;
import org.eclipse.corrosion.launch.RustLaunchDelegateTools;
import org.eclipse.corrosion.run.CargoRunDelegate;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;

public class CargoTestDelegate extends LaunchConfigurationDelegate implements ILaunchShortcut {

	public static final String PROJECT_ATTRIBUTE = CargoRunDelegate.PROJECT_ATTRIBUTE;
	public static final String TEST_OPTIONS_ATTRIBUTE = CargoRunDelegate.RUN_OPTIONS_ATTRIBUTE;
	public static final String TEST_ARGUMENTS_ATTRIBUTE = CargoRunDelegate.RUN_ARGUMENTS_ATTRIBUTE;
	public static final String TEST_NAME_ATTRIBUTE = "TEST_NAME"; //$NON-NLS-1$

	@Override
	public void launch(ISelection selection, String mode) {
		ILaunchConfiguration launchConfig = getLaunchConfiguration(RustLaunchDelegateTools.firstResourceFromSelection(selection));
		RustLaunchDelegateTools.launch(launchConfig, mode);
	}

	@Override
	public void launch(IEditorPart editor, String mode) {
		ILaunchConfiguration launchConfig = getLaunchConfiguration(RustLaunchDelegateTools.resourceFromEditor(editor));
		RustLaunchDelegateTools.launch(launchConfig, mode);
	}

	@Override public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		List<String> cargoTestCommand = new ArrayList<>();
		cargoTestCommand.add(CargoTools.getCargoCommand());
		cargoTestCommand.add("test"); //$NON-NLS-1$
		String projectName = configuration.getAttribute(PROJECT_ATTRIBUTE, ""); //$NON-NLS-1$
		String options = configuration.getAttribute(TEST_OPTIONS_ATTRIBUTE, "").trim(); //$NON-NLS-1$
		String testName = configuration.getAttribute(TEST_NAME_ATTRIBUTE, ""); //$NON-NLS-1$
		String arguments = configuration.getAttribute(TEST_ARGUMENTS_ATTRIBUTE, "").trim(); //$NON-NLS-1$

		IProject project = null;
		if (!projectName.isEmpty()) {
			project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		}
		if (project == null || !project.exists()) {
			RustLaunchDelegateTools.openError(Messages.CargoRunDelegate_unableToLaunch,
					Messages.CargoRunDelegate_unableToFindProject);
			return;
		}
		IFile cargoManifest = project.getFile("Cargo.toml"); //$NON-NLS-1$
		if (!cargoManifest.exists()) {
			RustLaunchDelegateTools.openError(Messages.CargoRunDelegate_unableToLaunch,
					Messages.CargoRunDelegate_unableToFindToml);
			return;
		}
		IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
		if (!options.isEmpty()) {
			cargoTestCommand.addAll(Arrays.asList(manager.performStringSubstitution(options).split("\\s+"))); //$NON-NLS-1$
		}

		final String cargoPathString = cargoManifest.getLocation().toPortableString();
		cargoTestCommand.add("--manifest-path"); //$NON-NLS-1$
		cargoTestCommand.add(cargoPathString);

		if (testName != null && !testName.isEmpty()) {
			cargoTestCommand.add(testName);
		}

		if (!arguments.isEmpty()) {
			cargoTestCommand.add("--"); //$NON-NLS-1$
			cargoTestCommand.addAll(Arrays.asList(manager.performStringSubstitution(arguments).split("\\s+"))); //$NON-NLS-1$
		}

		final List<String> finalTestCommand = cargoTestCommand;
		CompletableFuture.runAsync(() -> {
			try {
				String[] cmdLine = finalTestCommand.toArray(new String[finalTestCommand.size()]);
				Process p = DebugPlugin.exec(cmdLine, null);
				IProcess process = DebugPlugin.newProcess(launch, p, "cargo test"); //$NON-NLS-1$
				process.setAttribute(IProcess.ATTR_CMDLINE, String.join(" ", cmdLine)); //$NON-NLS-1$
			} catch (CoreException e) {
				RustLaunchDelegateTools.openError(Messages.CargoRunDelegate_unableToLaunch, e.getLocalizedMessage());
			}
		});
	}

	private ILaunchConfiguration getLaunchConfiguration(IResource resource) {
		ILaunchConfiguration launchConfiguration = RustLaunchDelegateTools.getLaunchConfiguration(resource,
				"org.eclipse.corrosion.test.CargoTestDelegate"); //$NON-NLS-1$
		if (launchConfiguration instanceof ILaunchConfigurationWorkingCopy) {
			ILaunchConfigurationWorkingCopy wc = (ILaunchConfigurationWorkingCopy) launchConfiguration;
			wc.setAttribute(PROJECT_ATTRIBUTE, resource.getProject().getName());
		}
		return launchConfiguration;
	}
}
