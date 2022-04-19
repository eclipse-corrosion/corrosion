/*********************************************************************
 * Copyright (c) 2019, 2021 Fraunhofer FOKUS and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Max Bureck (Fraunhofer FOKUS) - Initial implementation
 *******************************************************************************/
package org.eclipse.corrosion.launch;

import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.corrosion.Messages;
import org.eclipse.corrosion.test.CargoTestDelegate;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.lsp4e.command.LSPCommandHandler;
import org.eclipse.lsp4j.Command;
import org.eclipse.osgi.util.NLS;

/**
 * Handler for {@code rls.run} RLS LSP command ID.
 */
@SuppressWarnings("restriction") // We know we are using an unstable LSP4E feature
public class LaunchHandler extends LSPCommandHandler {

	private static final Pattern ENV_SPLITTER = Pattern.compile("="); //$NON-NLS-1$
	private static final Predicate<String> NOT_EMPTY = not(String::isEmpty);

	private static <T> Predicate<T> not(Predicate<T> p) {
		return p.negate();
	}

	@Override
	public Object execute(ExecutionEvent event, Command command, IPath context) throws ExecutionException {
		// Note that this check must change if we don't import the .cargo project
		// anymore
		IPath cargoRegistry = new Path("/.cargo/registry"); //$NON-NLS-1$
		if (cargoRegistry.isPrefixOf(context)) {
			RustLaunchDelegateTools.openError(Messages.LaunchHandler_unableToLaunch,
					Messages.LaunchHandler_launchingFromCargoRegistryUnsupported);
			return null;
		}

		Optional<RLSRunCommand> runCommand = RLSRunCommand.fromLSPCommand(command);
		if (!runCommand.isPresent()) {
			// RLS sent unknown command format, we cannot give advice to user, just log
			// the error
			String msg = NLS.bind(Messages.LaunchHandler_unableToLaunchCommand, command);
			CorrosionPlugin.logError(new Status(IStatus.ERROR, CorrosionPlugin.PLUGIN_ID, msg));
		}

		IProject project = ResourcesPlugin.getWorkspace().getRoot().getFile(context).getProject();
		if (project.exists()) {
			Optional<RLSRunCommand> cargoRunCommand = runCommand.filter(LaunchHandler::isCargoBin)
					.filter(LaunchHandler::isTestArg);
			cargoRunCommand.ifPresent(c -> createAndStartCargoTest(c, project));

			if (!cargoRunCommand.isPresent()) {
				// If command is not present, we have RLS command we don't know how to execute.
				// We cannot give user advice on what to do, just log the error
				String msg = NLS.bind(Messages.LaunchHandler_unableToLaunchCommand, command);
				CorrosionPlugin.logError(new Status(IStatus.ERROR, CorrosionPlugin.PLUGIN_ID, msg));
			}

			return null;
		}
		RustLaunchDelegateTools.openError(Messages.LaunchHandler_unableToLaunch,
				Messages.LaunchHandler_unsupportedProjectLocation);

		// TODO if no project, let's find the parent folder of context that contains a
		// Cargo.toml file.

		// TODO: if not cargo test, then run with external tool launch in context of
		// project/folder of context. Since the RLS currently only creates commands for
		// "cargo test", this is not urgent.
		return null;
	}

	private static boolean isCargoBin(RLSRunCommand command) {
		return Objects.equals(command.binary, "cargo") || Objects.equals(command.binary, "cargo.exe"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static boolean isTestArg(RLSRunCommand runCommand) {
		return runCommand.args.length > 0 && Objects.equals(runCommand.args[0], "test"); //$NON-NLS-1$
	}

	/**
	 * Looks up an existing cargo test launch configuration with the arguments
	 * specified in {@code command} for the given {@code project}. If no such launch
	 * configuration exists, creates a new one. The launch configuration is then
	 * run.
	 *
	 * @param command rls.run command with all information needed to run cargo test
	 * @param project the context project for which the launch configuration is
	 *                looked up / created
	 */
	private static void createAndStartCargoTest(RLSRunCommand command, IProject project) {
		CargoArgs args = CargoArgs.fromAllArguments(command.args);
		Map<String, String> envMap = command.env;

		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType type = manager
				.getLaunchConfigurationType(CargoTestDelegate.CARGO_TEST_LAUNCH_CONFIG_TYPE_ID);
		try {
			// check if launch config already exists
			ILaunchConfiguration[] launchConfigurations = manager.getLaunchConfigurations(type);

			Set<Entry<String, String>> envMapEntries = envMap.entrySet();
			// prefer running existing launch configuration with same parameters
			ILaunchConfiguration launchConfig = Arrays.stream(launchConfigurations)
					.filter(l -> matchesTestLaunchConfig(l, project, args, envMapEntries)).findAny()
					.orElseGet(() -> createCargoLaunchConfig(manager, type, project, args, envMap));

			if (launchConfig != null) {
				DebugUITools.launch(launchConfig, ILaunchManager.RUN_MODE);
			}
		} catch (CoreException e) {
			CorrosionPlugin.logError(e);
		}
	}

	/**
	 * This method can be used to create cargo test or cargo run launch
	 * configurations (depending on the {@code cargoLaunchType} argument). The
	 * attributes of the returned launch configuration will be set based on the
	 * information passed in as parameters.
	 *
	 * @param manager         is used to create the launch config
	 * @param cargoLaunchType determines which type of launch config to create
	 * @param project         the cargo project in the workspace, the launch config
	 *                        is created for.
	 * @param args            arguments to cargo
	 * @param envMap          environment variables to be set for the launch
	 *                        configuration
	 * @return a new launch configuration, based on the given parameters
	 */
	private static ILaunchConfiguration createCargoLaunchConfig(ILaunchManager manager,
			ILaunchConfigurationType cargoLaunchType, IProject project, CargoArgs args, Map<String, String> envMap) {
		try {
			String projectName = project.getName();
			String optionsString = toSpaceSeparatedString(args.options);
			String argumentsString = toSpaceSeparatedString(args.arguments);
			String launchConfigName = createLaunchConfigName(manager, projectName, args.command, optionsString,
					argumentsString);

			ILaunchConfigurationWorkingCopy launchWorkingCopy = cargoLaunchType.newInstance(null, launchConfigName);
			launchWorkingCopy.setAttribute(RustLaunchDelegateTools.PROJECT_ATTRIBUTE, projectName);
			launchWorkingCopy.setAttribute(RustLaunchDelegateTools.OPTIONS_ATTRIBUTE, optionsString);
			launchWorkingCopy.setAttribute(RustLaunchDelegateTools.ARGUMENTS_ATTRIBUTE, argumentsString);
			launchWorkingCopy.setAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, envMap);
			launchWorkingCopy.setAttribute(DebugPlugin.ATTR_WORKING_DIRECTORY, projectName);

			// we currently do not call launchWorkingCopy.doSave(), since in practice it
			// would clutter the workspace with launch configurations. Maybe at some point
			// in time we may add the ability for the user to make the launch configuration
			// persistent (e.g. by holing CTRL on click, or whatever is possible).
			return launchWorkingCopy;
		} catch (CoreException e) {
			CorrosionPlugin.logError(e);
			return null;
		}
	}

	private static String toSpaceSeparatedString(List<String> strings) {
		return strings.stream().collect(Collectors.joining(" ")); //$NON-NLS-1$
	}

	private static String createLaunchConfigName(ILaunchManager manager, String projectName, String command,
			String optionsString, String argumentsString) {
		String nameSuffix = Stream.of(command, optionsString, "--", argumentsString).filter(NOT_EMPTY) //$NON-NLS-1$
				.collect(Collectors.joining(" ", " [", "]")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		String nameWithSuffix = projectName + nameSuffix;
		return manager.generateLaunchConfigurationName(nameWithSuffix);
	}

	private static boolean matchesTestLaunchConfig(ILaunchConfiguration launchConfig, IProject cmdProject,
			CargoArgs cmdArgs, Set<Entry<String, String>> cmdEnvEntries) {
		try {
			String project = launchConfig.getAttribute(RustLaunchDelegateTools.PROJECT_ATTRIBUTE, ""); //$NON-NLS-1$
			if (!project.equals(cmdProject.getName())) {
				return false;
			}

			String optionsStr = launchConfig.getAttribute(RustLaunchDelegateTools.OPTIONS_ATTRIBUTE, ""); //$NON-NLS-1$
			String[] options = {};
			if (!optionsStr.isEmpty()) {
				options = substituteVariablesAndSplit(optionsStr);
			}
			if (!cmdArgs.options.equals(Arrays.asList(options))) {
				return false;
			}

			String argumentsStr = launchConfig.getAttribute(RustLaunchDelegateTools.ARGUMENTS_ATTRIBUTE, ""); //$NON-NLS-1$
			String[] arguments = {};
			if (!argumentsStr.isEmpty()) {
				arguments = substituteVariablesAndSplit(argumentsStr);
			}
			if (!cmdArgs.arguments.equals(Arrays.asList(arguments))) {
				return false;
			}

			final String[] envArgs = DebugPlugin.getDefault().getLaunchManager().getEnvironment(launchConfig);
			Set<Entry<String, String>> envEntries = Collections.emptySet();
			if (envArgs != null) {
				envEntries = Arrays.stream(envArgs).map(LaunchHandler::splitToEntry).collect(Collectors.toSet());
			}
			if (!envEntries.containsAll(cmdEnvEntries)) {
				return false;
			}

			// Project, options, test arguments, and environment variables match
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private static String[] substituteVariablesAndSplit(String argumentsStr) throws CoreException {
		return RustLaunchDelegateTools.performVariableSubstitution(argumentsStr).split("\\s+"); //$NON-NLS-1$
	}

	private static Entry<String, String> splitToEntry(String s) {
		String[] split = ENV_SPLITTER.split(s, 2);
		String key = split[0];
		String value = ""; //$NON-NLS-1$
		if (split.length > 1) {
			value = split[1];
		}
		return new SimpleEntry<>(key, value);
	}
}