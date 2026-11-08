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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.eclipse.cdt.debug.core.ICDTLaunchConfigurationConstants;
import org.eclipse.cdt.dsf.gdb.IGDBLaunchConfigurationConstants;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.corrosion.Messages;
import org.eclipse.corrosion.RustManager;
import org.eclipse.corrosion.cargo.core.CargoTools;
import org.eclipse.corrosion.debug.RustDebugDelegate;
import org.eclipse.corrosion.launch.RustLaunchDelegateTools;
import org.eclipse.corrosion.launch.TestAtCursor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.ILaunchShortcut2;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.unittest.ui.ConfigureViewerSupport;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class CargoTestDelegate extends LaunchConfigurationDelegate implements ILaunchShortcut2 {
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
	public ILaunchConfiguration[] getLaunchConfigurations(ISelection selection) {
		return RustLaunchDelegateTools.findLaunchConfigurations(
				RustLaunchDelegateTools.firstResourceFromSelection(selection), CARGO_TEST_LAUNCH_CONFIG_TYPE_ID);
	}

	@Override
	public ILaunchConfiguration[] getLaunchConfigurations(IEditorPart editor) {
		return RustLaunchDelegateTools.findLaunchConfigurations(RustLaunchDelegateTools.resourceFromEditor(editor),
				CARGO_TEST_LAUNCH_CONFIG_TYPE_ID);
	}

	@Override
	public IResource getLaunchableResource(ISelection selection) {
		return RustLaunchDelegateTools.firstResourceFromSelection(selection);
	}

	@Override
	public IResource getLaunchableResource(IEditorPart editor) {
		return RustLaunchDelegateTools.resourceFromEditor(editor);
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
		if (ILaunchManager.DEBUG_MODE.equals(mode)) {
			final IProject finalProject = project;
			CompletableFuture.runAsync(() -> {
				try {
					debugTests(configuration, launch, finalProject, finalWorkingDirectory, env);
				} catch (CoreException e) {
					CorrosionPlugin.logError(e);
					RustLaunchDelegateTools.openError(Messages.CargoRunDelegate_unableToLaunch, e.getLocalizedMessage());
				}
			});
		} else {
			CompletableFuture.runAsync(() -> {
				try {
					String[] cmdLine = finalTestCommand.toArray(new String[finalTestCommand.size()]);
					Process p = DebugPlugin.exec(cmdLine, finalWorkingDirectory, env);
					IProcess process = DebugPlugin.newProcess(launch, p, "cargo test"); //$NON-NLS-1$
					process.setAttribute(IProcess.ATTR_CMDLINE, String.join(" ", cmdLine)); //$NON-NLS-1$
				} catch (CoreException e) {
					CorrosionPlugin.logError(e);
					RustLaunchDelegateTools.openError(Messages.CargoRunDelegate_unableToLaunch, e.getLocalizedMessage());
				}
			});
		}
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
		// consider setting CARGO_TARGET_X86_64_UNKNOWN_LINUX_GNU_RUNNER and so on in
		// debug mode with a good launch command to make the process
		// wait at startup, since tests can often complete before debugger has time to
		// attach. Eg trying `/bin/bash -e "( kill -SIGSTOP $BASHPID; exec my_command )
		// &"` to start the process and restart it with `kill -SIGCONTINUE` once
		// debugger
		// is attached.
		configWC.doSave();
	}

	/**
	 * Debugs the tests. A short test can end before a debugger can attach to it,
	 * thus GDB starts the test programs: first "cargo test --no-run" builds them,
	 * then each test program starts in a Rust debug launch.
	 */
	private static void debugTests(ILaunchConfiguration configuration, ILaunch launch, IProject project,
			File workingDirectory, String[] env) throws CoreException {
		String options = RustLaunchDelegateTools
				.performVariableSubstitution(configuration.getAttribute(RustLaunchDelegateTools.OPTIONS_ATTRIBUTE, "").trim()); //$NON-NLS-1$
		String testName = configuration.getAttribute(TEST_NAME_ATTRIBUTE, "").trim(); //$NON-NLS-1$
		String arguments = RustLaunchDelegateTools.performVariableSubstitution(
				configuration.getAttribute(RustLaunchDelegateTools.ARGUMENTS_ATTRIBUTE, "").trim()); //$NON-NLS-1$
		String manifest = project.getFile("Cargo.toml").getLocation().toPortableString(); //$NON-NLS-1$

		// Build the test programs. The console shows the build output and errors.
		List<String> buildCommand = new ArrayList<>();
		buildCommand.add(CargoTools.getCargoCommand());
		buildCommand.add("test"); //$NON-NLS-1$
		buildCommand.add("--no-run"); //$NON-NLS-1$
		if (!options.isEmpty()) {
			buildCommand.addAll(Arrays.asList(options.split("\\s+"))); //$NON-NLS-1$
		}
		buildCommand.add("--manifest-path"); //$NON-NLS-1$
		buildCommand.add(manifest);
		String[] buildCmdLine = buildCommand.toArray(new String[buildCommand.size()]);
		Process build = DebugPlugin.exec(buildCmdLine, workingDirectory, env);
		IProcess buildProcess = DebugPlugin.newProcess(launch, build, "cargo test --no-run"); //$NON-NLS-1$
		buildProcess.setAttribute(IProcess.ATTR_CMDLINE, String.join(" ", buildCmdLine)); //$NON-NLS-1$
		try {
			if (build.waitFor() != 0) {
				return; // the console shows the errors
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return;
		}

		// Get the paths of the test programs. Cargo does not build again.
		buildCommand.add("--message-format=json"); //$NON-NLS-1$
		List<String> executables = new ArrayList<>();
		Process list = DebugPlugin.exec(buildCommand.toArray(new String[buildCommand.size()]), workingDirectory, env);
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(list.getInputStream(), StandardCharsets.UTF_8))) {
			reader.lines().map(CargoTestDelegate::testExecutable).filter(Objects::nonNull).forEach(executables::add);
		} catch (IOException e) {
			throw new CoreException(Status.error(e.getMessage(), e));
		}

		String programArguments = (testName + " " + arguments).trim(); //$NON-NLS-1$
		for (String executable : executables) {
			getTestDebugConfiguration(configuration, project, executable, options, programArguments)
					.launch(ILaunchManager.DEBUG_MODE, new NullProgressMonitor());
		}
	}

	/**
	 * Returns the test program path from one line of "cargo --message-format=json"
	 * output, or null if the line is not about a test program.
	 */
	private static String testExecutable(String line) {
		try {
			if (!(JsonParser.parseString(line) instanceof JsonObject message)
					|| !"compiler-artifact".equals(getString(message, "reason")) //$NON-NLS-1$ //$NON-NLS-2$
					|| !(message.get("profile") instanceof JsonObject profile) //$NON-NLS-1$
					|| !profile.has("test") || !profile.get("test").getAsBoolean()) { //$NON-NLS-1$ //$NON-NLS-2$
				return null;
			}
			return getString(message, "executable"); //$NON-NLS-1$
		} catch (RuntimeException e) {
			return null; // not JSON
		}
	}

	private static String getString(JsonObject object, String member) {
		JsonElement element = object.get(member);
		return element != null && element.isJsonPrimitive() ? element.getAsString() : null;
	}

	/**
	 * Finds or makes the private Rust debug configuration that starts one test
	 * program in GDB. It is private, so it does not show in the launch dialogs and
	 * the launch history.
	 */
	private static ILaunchConfiguration getTestDebugConfiguration(ILaunchConfiguration testConfiguration,
			IProject project, String executable, String options, String programArguments) throws CoreException {
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType type = launchManager
				.getLaunchConfigurationType(RustLaunchDelegateTools.CORROSION_DEBUG_LAUNCH_CONFIG_TYPE);
		String name = testConfiguration.getName() + " (debug " + new File(executable).getName() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		ILaunchConfigurationWorkingCopy wc = null;
		for (ILaunchConfiguration existing : launchManager.getLaunchConfigurations(type)) {
			if (existing.getName().equals(name)) {
				wc = existing.getWorkingCopy();
			}
		}
		if (wc == null) {
			wc = type.newInstance(null, launchManager.generateLaunchConfigurationName(name));
		}
		wc.setAttribute(IDebugUIConstants.ATTR_PRIVATE, true);
		wc.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROJECT_NAME, project.getName());
		wc.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROGRAM_NAME, executable);
		wc.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, programArguments);
		wc.setAttribute(ICDTLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY, project.getLocation().toString());
		// The Rust debug launch builds first: "cargo test --no-run" has nothing to do now
		wc.setAttribute(RustDebugDelegate.BUILD_COMMAND_ATTRIBUTE, ("test --no-run " + options).trim()); //$NON-NLS-1$
		wc.setAttribute(ICDTLaunchConfigurationConstants.ATTR_DEBUGGER_STOP_AT_MAIN, false);
		wc.setAttribute(IGDBLaunchConfigurationConstants.ATTR_DEBUG_NAME, RustManager.getDefaultDebugger());
		wc.setAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES,
				testConfiguration.getAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, Collections.emptyMap()));
		return wc.doSave();
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

	/**
	 * Finds or makes a saved launch configuration that runs only the given test.
	 *
	 * @param project
	 * @param test    the test at the cursor
	 * @return the saved launch configuration
	 */
	public static ILaunchConfiguration getSingleTestConfiguration(IProject project, TestAtCursor test)
			throws CoreException {
		for (ILaunchConfiguration configuration : RustLaunchDelegateTools.findLaunchConfigurations(project,
				CARGO_TEST_LAUNCH_CONFIG_TYPE_ID)) {
			String testName = configuration.getAttribute(TEST_NAME_ATTRIBUTE, ""); //$NON-NLS-1$
			String options = configuration.getAttribute(RustLaunchDelegateTools.OPTIONS_ATTRIBUTE, ""); //$NON-NLS-1$
			if (test.name().equals(testName) && test.options().equals(options)) {
				return configuration;
			}
		}
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		// "::" is not permitted in a configuration name
		String name = launchManager
				.generateLaunchConfigurationName(project.getName() + " " + test.name().replace("::", ".")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		ILaunchConfigurationWorkingCopy wc = launchManager.getLaunchConfigurationType(CARGO_TEST_LAUNCH_CONFIG_TYPE_ID)
				.newInstance(null, name);
		wc.setAttribute(RustLaunchDelegateTools.PROJECT_ATTRIBUTE, project.getName());
		wc.setAttribute(RustLaunchDelegateTools.OPTIONS_ATTRIBUTE, test.options());
		wc.setAttribute(TEST_NAME_ATTRIBUTE, test.name());
		wc.setAttribute(RustLaunchDelegateTools.ARGUMENTS_ATTRIBUTE, test.arguments());
		new ConfigureViewerSupport(CARGO_UNITTEST_VIEW_SUPPORT_ID).apply(wc);
		return wc.doSave();
	}
}
