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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.eclipse.cdt.debug.core.ICDTLaunchConfigurationConstants;
import org.eclipse.cdt.dsf.gdb.IGDBLaunchConfigurationConstants;
import org.eclipse.cdt.dsf.gdb.IGdbDebugConstants;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.corrosion.Messages;
import org.eclipse.corrosion.RustManager;
import org.eclipse.corrosion.cargo.core.CargoTools;
import org.eclipse.corrosion.debug.RustDebugDelegate;
import org.eclipse.corrosion.launch.RustLaunchDelegateTools;
import org.eclipse.corrosion.launch.TestAtCursor;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.ILaunchShortcut2;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.unittest.ui.ConfigureViewerSupport;

import com.google.gson.JsonArray;
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
			// The test launch stays active until the last test program ends
			DebugRunProcess debugRun = new DebugRunProcess(launch);
			launch.addProcess(debugRun);
			// Add the build process in this thread too. The unit test view makes its test
			// session when the first process is added, and it can make two sessions if two
			// threads add processes at the same time.
			List<String> buildCommand;
			Process build;
			try {
				buildCommand = buildCommand(configuration, finalProject);
				build = startBuild(buildCommand, launch, finalWorkingDirectory, env);
			} catch (CoreException e) {
				DebuggedTestPrograms.done(launch);
				debugRun.end();
				throw e;
			}
			// A job and not the common pool, because the debug run can take hours
			Job.createSystem("Debug Cargo tests", jobMonitor -> { //$NON-NLS-1$
				try {
					debugTests(configuration, launch, debugRun, finalProject, build, buildCommand,
							finalWorkingDirectory, env);
				} catch (CoreException e) {
					CorrosionPlugin.logError(e);
					RustLaunchDelegateTools.openError(Messages.CargoRunDelegate_unableToLaunch, e.getLocalizedMessage());
				}
			}).schedule();
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

	/** The "cargo test --no-run" command that builds the test programs */
	private static List<String> buildCommand(ILaunchConfiguration configuration, IProject project)
			throws CoreException {
		String options = RustLaunchDelegateTools.performVariableSubstitution(
				configuration.getAttribute(RustLaunchDelegateTools.OPTIONS_ATTRIBUTE, "").trim()); //$NON-NLS-1$
		List<String> buildCommand = new ArrayList<>();
		buildCommand.add(CargoTools.getCargoCommand());
		buildCommand.add("test"); //$NON-NLS-1$
		buildCommand.add("--no-run"); //$NON-NLS-1$
		if (!options.isEmpty()) {
			buildCommand.addAll(Arrays.asList(options.split("\\s+"))); //$NON-NLS-1$
		}
		buildCommand.add("--manifest-path"); //$NON-NLS-1$
		buildCommand.add(project.getFile("Cargo.toml").getLocation().toPortableString()); //$NON-NLS-1$
		return buildCommand;
	}

	/** Starts the build of the test programs. The console shows its output. */
	private static Process startBuild(List<String> buildCommand, ILaunch launch, File workingDirectory, String[] env)
			throws CoreException {
		String[] buildCmdLine = buildCommand.toArray(new String[buildCommand.size()]);
		Process build = DebugPlugin.exec(buildCmdLine, workingDirectory, env);
		IProcess buildProcess = DebugPlugin.newProcess(launch, build, "cargo test --no-run"); //$NON-NLS-1$
		buildProcess.setAttribute(IProcess.ATTR_CMDLINE, String.join(" ", buildCmdLine)); //$NON-NLS-1$
		return build;
	}

	/**
	 * Debugs the tests. A short test can end before a debugger can attach to it,
	 * thus GDB starts the test programs: first "cargo test --no-run" builds them,
	 * then each test program starts in a Rust debug launch, one after the other,
	 * as "cargo test" does. The unit test view reads the output of the test
	 * programs.
	 */
	private static void debugTests(ILaunchConfiguration configuration, ILaunch launch, DebugRunProcess debugRun,
			IProject project, Process build, List<String> buildCommand, File workingDirectory, String[] env)
			throws CoreException {
		try {
			String options = RustLaunchDelegateTools.performVariableSubstitution(
					configuration.getAttribute(RustLaunchDelegateTools.OPTIONS_ATTRIBUTE, "").trim()); //$NON-NLS-1$
			String testName = configuration.getAttribute(TEST_NAME_ATTRIBUTE, "").trim(); //$NON-NLS-1$
			String arguments = RustLaunchDelegateTools.performVariableSubstitution(
					configuration.getAttribute(RustLaunchDelegateTools.ARGUMENTS_ATTRIBUTE, "").trim()); //$NON-NLS-1$
			if (build.waitFor() != 0) {
				return; // the console shows the errors
			}

			// Get the test programs. Cargo does not build again.
			List<String> listCommand = new ArrayList<>(buildCommand);
			listCommand.add("--message-format=json"); //$NON-NLS-1$
			List<TestProgram> programs = listTestPrograms(listCommand, workingDirectory, env);

			String programArguments = (testName + " " + arguments).trim(); //$NON-NLS-1$
			if (!programArguments.contains("--color")) { //$NON-NLS-1$
				// GDB gives the test program a terminal. Colors in the output would stop
				// the unit test view from reading it.
				programArguments = (programArguments + " --color never").trim(); //$NON-NLS-1$
			}
			ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
			for (TestProgram program : programs) {
				if (debugRun.isTerminated() || !launchManager.isRegistered(launch)) {
					return; // the user stopped or removed the test launch
				}
				ILaunch debugLaunch = getTestDebugConfiguration(configuration, project, program, options,
						programArguments).launch(ILaunchManager.DEBUG_MODE, new NullProgressMonitor());
				debugRun.setCurrentLaunch(debugLaunch);
				IProcess testProcess = waitForTestProcess(debugLaunch);
				if (testProcess != null) {
					DebuggedTestPrograms.add(launch, testProcess);
				}
				while (!debugLaunch.isTerminated() && !debugRun.isTerminated() && launchManager.isRegistered(launch)) {
					Thread.sleep(100);
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			DebuggedTestPrograms.done(launch);
			debugRun.end();
		}
	}

	/**
	 * Runs the given "cargo --message-format=json" command and returns the test
	 * programs from its output.
	 */
	private static List<TestProgram> listTestPrograms(List<String> command, File workingDirectory, String[] env)
			throws CoreException {
		ProcessBuilder builder = new ProcessBuilder(command).directory(workingDirectory)
				// Cargo writes its status lines to stderr. Discard them, so that Cargo does not
				// block when the stderr buffer is full.
				.redirectError(ProcessBuilder.Redirect.DISCARD);
		if (env != null) {
			builder.environment().clear();
			for (String variable : env) {
				int separator = variable.indexOf('=');
				if (separator > 0) {
					builder.environment().put(variable.substring(0, separator), variable.substring(separator + 1));
				}
			}
		}
		List<TestProgram> programs = new ArrayList<>();
		try {
			Process process = builder.start();
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
				reader.lines().map(CargoTestDelegate::testProgram).filter(Objects::nonNull).forEach(programs::add);
			}
		} catch (IOException e) {
			throw new CoreException(Status.error(e.getMessage(), e));
		}
		return programs;
	}

	/**
	 * Waits until GDB starts the test program, and returns its process. GDB starts
	 * it in a different process of the debug launch.
	 *
	 * @return the process of the test program, or null if the debug launch ended
	 *         first
	 */
	private static IProcess waitForTestProcess(ILaunch debugLaunch) throws InterruptedException {
		while (!debugLaunch.isTerminated()) {
			for (IProcess process : debugLaunch.getProcesses()) {
				if (IGdbDebugConstants.INFERIOR_PROCESS_CREATION_VALUE
						.equals(process.getAttribute(IGdbDebugConstants.PROCESS_TYPE_CREATION_ATTR))) {
					return process;
				}
			}
			Thread.sleep(50);
		}
		return null;
	}

	/**
	 * A test program that Cargo built.
	 *
	 * @param executable the path of the test program
	 * @param directory  the folder of the package of the test program. Cargo
	 *                   starts the test program in this folder.
	 * @param target     the name and the kind of the Cargo target, for example
	 *                   "basic lib". Unlike the file name of the program, it does
	 *                   not change when the program is built again.
	 */
	record TestProgram(String executable, String directory, String target) {
	}

	/**
	 * A process of the test launch in debug mode, which shows the debug run of the
	 * test programs. While it is not terminated, the test launch is not
	 * terminated: Eclipse removes terminated launches when it adds a new launch,
	 * and the debug launches of the test programs are new launches. Terminate
	 * stops the debug run.
	 */
	static final class DebugRunProcess extends PlatformObject implements IProcess {
		private final ILaunch launch;
		private final Map<String, String> attributes = new HashMap<>();
		private volatile boolean terminated;
		private volatile ILaunch currentLaunch;

		DebugRunProcess(ILaunch launch) {
			this.launch = launch;
		}

		void setCurrentLaunch(ILaunch debugLaunch) {
			this.currentLaunch = debugLaunch;
			if (terminated) {
				terminateCurrentLaunch();
			}
		}

		/** The debug run ended. */
		void end() {
			if (!terminated) {
				terminated = true;
				DebugPlugin.getDefault().fireDebugEventSet(new DebugEvent[] { new DebugEvent(this, DebugEvent.TERMINATE) });
			}
		}

		private void terminateCurrentLaunch() {
			ILaunch debugLaunch = currentLaunch;
			if (debugLaunch != null && debugLaunch.canTerminate()) {
				try {
					debugLaunch.terminate();
				} catch (DebugException e) {
					CorrosionPlugin.logError(e);
				}
			}
		}

		@Override
		public boolean canTerminate() {
			return !terminated;
		}

		@Override
		public boolean isTerminated() {
			return terminated;
		}

		@Override
		public void terminate() {
			end();
			terminateCurrentLaunch();
		}

		@Override
		public String getLabel() {
			return Messages.CargoTestDelegate_debugRun;
		}

		@Override
		public ILaunch getLaunch() {
			return launch;
		}

		@Override
		public IStreamsProxy getStreamsProxy() {
			return null; // no console
		}

		@Override
		public synchronized void setAttribute(String key, String value) {
			attributes.put(key, value);
		}

		@Override
		public synchronized String getAttribute(String key) {
			return attributes.get(key);
		}

		@Override
		public int getExitValue() throws DebugException {
			if (!terminated) {
				throw new DebugException(Status.error("The debug run did not end")); //$NON-NLS-1$
			}
			return 0;
		}
	}

	/**
	 * Returns the test program from one line of "cargo --message-format=json"
	 * output, or null if the line is not about a test program.
	 */
	static TestProgram testProgram(String line) {
		try {
			if (!(JsonParser.parseString(line) instanceof JsonObject message)
					|| !"compiler-artifact".equals(getString(message, "reason")) //$NON-NLS-1$ //$NON-NLS-2$
					|| !(message.get("profile") instanceof JsonObject profile) //$NON-NLS-1$
					|| !profile.has("test") || !profile.get("test").getAsBoolean()) { //$NON-NLS-1$ //$NON-NLS-2$
				return null;
			}
			String executable = getString(message, "executable"); //$NON-NLS-1$
			if (executable == null) {
				return null;
			}
			String manifest = getString(message, "manifest_path"); //$NON-NLS-1$
			String directory = manifest != null ? new File(manifest).getParent() : null;
			String target = new File(executable).getName();
			if (message.get("target") instanceof JsonObject targetObject && getString(targetObject, "name") != null) { //$NON-NLS-1$ //$NON-NLS-2$
				target = getString(targetObject, "name"); //$NON-NLS-1$
				if (targetObject.get("kind") instanceof JsonArray kinds && !kinds.isEmpty() //$NON-NLS-1$
						&& kinds.get(0).isJsonPrimitive()) {
					target += " " + kinds.get(0).getAsString(); //$NON-NLS-1$
				}
			}
			return new TestProgram(executable, directory, target);
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
			IProject project, TestProgram program, String options, String programArguments) throws CoreException {
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType type = launchManager
				.getLaunchConfigurationType(RustLaunchDelegateTools.CORROSION_DEBUG_LAUNCH_CONFIG_TYPE);
		String name = testConfiguration.getName() + " (debug " + program.target() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		ILaunchConfigurationWorkingCopy wc = null;
		for (ILaunchConfiguration existing : launchManager.getLaunchConfigurations(type)) {
			if (existing.getName().equals(name)) {
				wc = existing.getWorkingCopy();
			}
		}
		if (wc == null) {
			wc = type.newInstance(null, launchManager.generateLaunchConfigurationName(name));
		}
		String directory = program.directory() != null ? program.directory() : project.getLocation().toString();
		wc.setAttribute(IDebugUIConstants.ATTR_PRIVATE, true);
		wc.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROJECT_NAME, project.getName());
		wc.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROGRAM_NAME, program.executable());
		wc.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, programArguments);
		wc.setAttribute(ICDTLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY, directory);
		// The Rust debug launch builds first: "cargo test --no-run" has nothing to do now
		wc.setAttribute(RustDebugDelegate.BUILD_COMMAND_ATTRIBUTE, ("test --no-run " + options).trim()); //$NON-NLS-1$
		wc.setAttribute(ICDTLaunchConfigurationConstants.ATTR_DEBUGGER_STOP_AT_MAIN, false);
		wc.setAttribute(IGDBLaunchConfigurationConstants.ATTR_DEBUG_NAME, RustManager.getDefaultDebugger());
		wc.setAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES,
				testConfiguration.getAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, Collections.emptyMap()));
		wc.setAttribute(ILaunchManager.ATTR_APPEND_ENVIRONMENT_VARIABLES,
				testConfiguration.getAttribute(ILaunchManager.ATTR_APPEND_ENVIRONMENT_VARIABLES, true));
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
			String arguments = configuration.getAttribute(RustLaunchDelegateTools.ARGUMENTS_ATTRIBUTE, ""); //$NON-NLS-1$
			if (test.name().equals(testName) && test.options().equals(options) && test.arguments().equals(arguments)) {
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
