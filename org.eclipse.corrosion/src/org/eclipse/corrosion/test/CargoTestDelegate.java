/*********************************************************************
 * Copyright (c) 2017, 2020 Red Hat Inc. and others.
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

import java.io.File;
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
import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.corrosion.Messages;
import org.eclipse.corrosion.cargo.core.CargoTools;
import org.eclipse.corrosion.launch.RustLaunchDelegateTools;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.unittest.ui.ConfigureViewerSupport;

public class CargoTestDelegate extends LaunchConfigurationDelegate implements ILaunchShortcut {
	public static final String CARGO_TEST_LAUNCH_CONFIG_TYPE_ID = "org.eclipse.corrosion.test.CargoTestDelegate"; //$NON-NLS-1$
	public static final String TEST_NAME_ATTRIBUTE = "TEST_NAME"; //$NON-NLS-1$
	public static final String CARGO_UNITTEST_VIEW_SUPPORT_ID = "org.eclipse.corrosion.unitTestSupport"; //$NON-NLS-1$

	@Override
	public ILaunch getLaunch(ILaunchConfiguration configuration, String mode) throws CoreException {
		CorrosionPlugin.activateUnitTestCoreBundle();
		updatedLaunchConfiguration(configuration);
		return super.getLaunch(configuration, mode);
	}

	@Override
	public void launch(ISelection selection, String mode) {
		try {
			ILaunchConfiguration launchConfig = getLaunchConfiguration(
					RustLaunchDelegateTools.firstResourceFromSelection(selection));
			RustLaunchDelegateTools.launch(launchConfig, mode);
		} catch (CoreException e) {
			CorrosionPlugin.logError(e);
		}
	}

	@Override
	public void launch(IEditorPart editor, String mode) {
		try {
			ILaunchConfiguration launchConfig = getLaunchConfiguration(
					RustLaunchDelegateTools.resourceFromEditor(editor));
			RustLaunchDelegateTools.launch(launchConfig, mode);
		} catch (CoreException e) {
			CorrosionPlugin.logError(e);
		}
	}

	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
			throws CoreException {
		updatedLaunchConfiguration(configuration);
		String projectName = configuration.getAttribute(RustLaunchDelegateTools.PROJECT_ATTRIBUTE, ""); //$NON-NLS-1$
		String options = configuration.getAttribute(RustLaunchDelegateTools.OPTIONS_ATTRIBUTE, "").trim(); //$NON-NLS-1$
		String testName = configuration.getAttribute(TEST_NAME_ATTRIBUTE, ""); //$NON-NLS-1$
		String arguments = configuration.getAttribute(RustLaunchDelegateTools.ARGUMENTS_ATTRIBUTE, "").trim(); //$NON-NLS-1$
		String workingDirectoryString = RustLaunchDelegateTools.performVariableSubstitution(
				configuration.getAttribute(RustLaunchDelegateTools.WORKING_DIRECTORY_ATTRIBUTE, "").trim()); //$NON-NLS-1$
		File workingDirectory = RustLaunchDelegateTools.convertToAbsolutePath(workingDirectoryString);
		ILaunchConfigurationWorkingCopy wc = null;

		IProject project = null;
		if (!projectName.isEmpty()) {
			project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		}
		if (project == null || !project.exists()) {
			RustLaunchDelegateTools.openError(Messages.CargoRunDelegate_unableToLaunch,
					Messages.CargoRunDelegate_unableToFindProject);
			return;
		}
		if (workingDirectoryString.isEmpty() || !workingDirectory.exists() || !workingDirectory.isDirectory()) {
			if (configuration instanceof ILaunchConfigurationWorkingCopy) {
				wc = (ILaunchConfigurationWorkingCopy) configuration;
				wc.setAttribute(RustLaunchDelegateTools.WORKING_DIRECTORY_ATTRIBUTE, project.getLocation().toString());
			}
		}
		IFile cargoManifest = project.getFile("Cargo.toml"); //$NON-NLS-1$
		if (!cargoManifest.exists()) {
			RustLaunchDelegateTools.openError(Messages.CargoRunDelegate_unableToLaunch,
					Messages.CargoRunDelegate_unableToFindToml);
			return;
		}

		List<String> cargoTestCommand = new ArrayList<>();
		cargoTestCommand.add(CargoTools.getCargoCommand());
		cargoTestCommand.add("test"); //$NON-NLS-1$
		if (!options.isEmpty()) {
			cargoTestCommand
					.addAll(Arrays.asList(RustLaunchDelegateTools.performVariableSubstitution(options).split("\\s+"))); //$NON-NLS-1$
		}

		final String cargoPathString = cargoManifest.getLocation().toPortableString();
		cargoTestCommand.add("--manifest-path"); //$NON-NLS-1$
		cargoTestCommand.add(cargoPathString);

		if (testName != null && !testName.isEmpty()) {
			cargoTestCommand.add(testName);
		}

		if (!arguments.isEmpty()) {
			cargoTestCommand.add("--"); //$NON-NLS-1$
			cargoTestCommand.addAll(
					Arrays.asList(RustLaunchDelegateTools.performVariableSubstitution(arguments).split("\\s+"))); //$NON-NLS-1$
		}

		final List<String> finalTestCommand = cargoTestCommand;
		final File finalWorkingDirectory = workingDirectory;
		final String[] envArgs = DebugPlugin.getDefault().getLaunchManager().getEnvironment(configuration);
		CompletableFuture.runAsync(() -> {
			try {
				String[] cmdLine = finalTestCommand.toArray(new String[finalTestCommand.size()]);
				Process p = DebugPlugin.exec(cmdLine, finalWorkingDirectory, envArgs);
				IProcess process = DebugPlugin.newProcess(launch, p, "cargo test"); //$NON-NLS-1$
				process.setAttribute(IProcess.ATTR_CMDLINE, String.join(" ", cmdLine)); //$NON-NLS-1$
			} catch (CoreException e) {
				e.printStackTrace();
				RustLaunchDelegateTools.openError(Messages.CargoRunDelegate_unableToLaunch, e.getLocalizedMessage());
			}
		});
		if (wc != null) {
			wc.doSave();
		}
	}

	/**
	 * Makes the necessary changes to the launch configuration before passing it to
	 * the underlying delegate. Currently, updates the program arguments with the
	 * value that was obtained from Tests Runner provider plug-in.
	 *
	 * @param config launch configuration
	 * @throws CoreException in case of error
	 */
	private static void updatedLaunchConfiguration(ILaunchConfiguration config) throws CoreException {
		ILaunchConfigurationWorkingCopy configWC = config.getWorkingCopy();
		new ConfigureViewerSupport(CARGO_UNITTEST_VIEW_SUPPORT_ID).apply(configWC);
		configWC.doSave();
	}

	private static ILaunchConfiguration getLaunchConfiguration(IResource resource) throws CoreException {
		ILaunchConfiguration launchConfiguration = RustLaunchDelegateTools
				.getLaunchConfiguration(resource, CARGO_TEST_LAUNCH_CONFIG_TYPE_ID).getWorkingCopy();
		if (launchConfiguration instanceof ILaunchConfigurationWorkingCopy) {
			ILaunchConfigurationWorkingCopy wc = (ILaunchConfigurationWorkingCopy) launchConfiguration;
			wc.setAttribute(RustLaunchDelegateTools.PROJECT_ATTRIBUTE, resource.getProject().getName());
			new ConfigureViewerSupport(CARGO_UNITTEST_VIEW_SUPPORT_ID).apply(wc);
		}
		return launchConfiguration;
	}
}
