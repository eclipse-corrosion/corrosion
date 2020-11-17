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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import org.eclipse.cdt.debug.core.ICDTLaunchConfigurationConstants;
import org.eclipse.cdt.dsf.gdb.IGDBLaunchConfigurationConstants;
import org.eclipse.cdt.dsf.gdb.IGdbDebugPreferenceConstants;
import org.eclipse.cdt.dsf.gdb.service.GDBBackend_7_12;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.corrosion.Messages;
import org.eclipse.corrosion.RustManager;
import org.eclipse.corrosion.cargo.core.CargoTools;
import org.eclipse.corrosion.launch.RustLaunchDelegateTools;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
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

	private static final class GDBCommandAccessor extends GDBBackend_7_12 {
		public GDBCommandAccessor(ILaunchConfiguration config) {
			super(null, config);
		}

		@Override
		public String[] getDebuggerCommandLine() {
			return super.getDebuggerCommandLine();
		}

		@Override
		protected IPath getGDBPath() {
			return new Path(RustManager.getDefaultDebugger());
		}
	}

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
		String workingDirectoryString = RustLaunchDelegateTools
				.performVariableSubstitution(configuration.getAttribute(DebugPlugin.ATTR_WORKING_DIRECTORY, "").trim()); //$NON-NLS-1$
		File workingDirectory = RustLaunchDelegateTools.convertToAbsolutePath(workingDirectoryString);
		ILaunchConfigurationWorkingCopy wc = configuration.isWorkingCopy()
				? (ILaunchConfigurationWorkingCopy) configuration
				: configuration.getWorkingCopy();

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
			wc.setAttribute(DebugPlugin.ATTR_WORKING_DIRECTORY, project.getLocation().toString());
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
		String[] envArgs = DebugPlugin.getDefault().getLaunchManager().getEnvironment(configuration);
		if (envArgs == null) {
			envArgs = new String[0];
		}
		final String[] env = envArgs;
		CompletableFuture.runAsync(() -> {
			try {
				String[] cmdLine = finalTestCommand.toArray(new String[finalTestCommand.size()]);
				Process p = DebugPlugin.exec(cmdLine, finalWorkingDirectory, env);
				IProcess process = DebugPlugin.newProcess(launch, p, "cargo test"); //$NON-NLS-1$
				if (ILaunchManager.DEBUG_MODE.equals(mode)) {
					ProcessHandle cargoHandle = p.toHandle();
					Set<ProcessHandle> captured = new HashSet<>();
					Job pollChildren = Job.createSystem("Capture children processes", //$NON-NLS-1$
							(ICoreRunnable) progressMonitor -> {
								while (!process.isTerminated() && !monitor.isCanceled()) {
									try {
										Thread.sleep(50);
									} catch (InterruptedException e) {
										CorrosionPlugin.logError(e);
										Thread.currentThread().interrupt();
									}
									cargoHandle.descendants() //
											.filter(Predicate.not(captured::contains)) //
											.filter(handle -> handle.info().commandLine()
													.map(line -> line.contains("target/debug")).orElse(Boolean.FALSE)) //$NON-NLS-1$
											.map(handle -> {
												captured.add(handle);
												return debug(handle, configuration);
											}).filter(Objects::nonNull) //
											.forEach(config -> {
												try {
													config.launch(ILaunchManager.DEBUG_MODE, new NullProgressMonitor());
												} catch (CoreException e) {
													CorrosionPlugin.logError(e);
												}
											});
								}
							});
					pollChildren.schedule();
					cargoHandle.onExit().thenAccept(h -> pollChildren.cancel());
				}
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
		if (configWC.getAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, (Map<String, String>) null) == null) {
			configWC.setAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, Collections.emptyMap());
		}
		configWC.doSave();
	}

	private static ILaunchConfiguration debug(ProcessHandle process, ILaunchConfiguration initialLaunchConfiguration) {
		ILaunchConfigurationWorkingCopy configWC;
		try {
			configWC = DebugPlugin.getDefault().getLaunchManager()
					.getLaunchConfigurationType(ICDTLaunchConfigurationConstants.ID_LAUNCH_C_ATTACH)
					.newInstance(null, process.info().commandLine().map(line -> line.split(File.separator))
							.map(segments -> segments[segments.length - 1]).orElse("unset")); //$NON-NLS-1$
			configWC.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROJECT_NAME, initialLaunchConfiguration
					.getAttribute(ICDTLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String) null));
		} catch (CoreException e) {
			CorrosionPlugin.logError(e);
			return null;
		}
		configWC.setAttribute(ICDTLaunchConfigurationConstants.ATTR_DEBUGGER_START_MODE,
				ICDTLaunchConfigurationConstants.DEBUGGER_MODE_ATTACH);
		configWC.setAttribute(IGdbDebugPreferenceConstants.PREF_DEFAULT_NON_STOP, true);
		configWC.setAttribute(ICDTLaunchConfigurationConstants.ATTR_ATTACH_PROCESS_ID, (int) process.pid());
		configWC.setAttribute(ICDTLaunchConfigurationConstants.ATTR_DEBUGGER_STOP_AT_MAIN, false);
		configWC.setAttribute(IGDBLaunchConfigurationConstants.ATTR_DEBUG_NAME, RustManager.getDefaultDebugger());
		configWC.setAttribute(DebugPlugin.ATTR_ENVIRONMENT, Collections.emptyMap());
		return configWC;
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
