/*********************************************************************
 * Copyright (c) 2018 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Lucas Bullen (Red Hat Inc.) - Initial implementation
 *  Max Bureck (Fraunhofer FOKUS) - Moved default GDB definition to re-usable constant
 *                                - Moved default GDB to properties
 *******************************************************************************/
package org.eclipse.corrosion.debug;

import static org.eclipse.corrosion.debug.DebugUtil.getDefaultExecutablePath;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.cdt.debug.core.ICDTLaunchConfigurationConstants;
import org.eclipse.cdt.dsf.debug.sourcelookup.DsfSourceLookupDirector;
import org.eclipse.cdt.dsf.gdb.IGDBLaunchConfigurationConstants;
import org.eclipse.cdt.dsf.gdb.launching.GdbLaunchDelegate;
import org.eclipse.cdt.dsf.gdb.launching.LaunchUtils;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.corrosion.RustManager;
import org.eclipse.corrosion.cargo.core.CargoTools;
import org.eclipse.corrosion.launch.RustLaunchDelegateTools;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;

public class RustDebugDelegate extends GdbLaunchDelegate implements ILaunchShortcut {
	public static final String BUILD_COMMAND_ATTRIBUTE = CorrosionPlugin.PLUGIN_ID + ".BUILD_COMMAND"; //$NON-NLS-1$

	@Override
	public void launch(ILaunchConfiguration config, String mode, ILaunch launch, IProgressMonitor monitor)
			throws CoreException {
		ILaunchConfiguration configuration = launch.getLaunchConfiguration();
		String buildCommand = configuration.getAttribute(BUILD_COMMAND_ATTRIBUTE, ""); //$NON-NLS-1$
		String projectName = configuration.getAttribute(ICDTLaunchConfigurationConstants.ATTR_PROJECT_NAME, ""); //$NON-NLS-1$
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		String workingDirectoryString = RustLaunchDelegateTools.performVariableSubstitution(
				configuration.getAttribute(ICDTLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY, "").trim()); //$NON-NLS-1$
		File workingDirectory = RustLaunchDelegateTools.convertToAbsolutePath(workingDirectoryString);
		if (workingDirectoryString.isEmpty() || !workingDirectory.exists() || !workingDirectory.isDirectory()) {
			workingDirectory = project.getLocation().toFile();
		}

		List<String> cmdLine = new ArrayList<>();
		cmdLine.add(CargoTools.getCargoCommand());
		if (buildCommand.isEmpty()) {
			cmdLine.add("build"); //$NON-NLS-1$
		} else {
			IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
			buildCommand = manager.performStringSubstitution(buildCommand);
			cmdLine.addAll(Arrays.asList(buildCommand.replace('\n', ' ').split(" "))); //$NON-NLS-1$
		}

		final String manifestPathOptionString = "--manifest-path";//$NON-NLS-1$
		if (!buildCommand.contains(manifestPathOptionString)) {
			cmdLine.add(manifestPathOptionString);
			cmdLine.add(project.getFile("Cargo.toml").getLocation().toString()); //$NON-NLS-1$
		}

		Process restoreProcess = DebugPlugin.exec(cmdLine.toArray(new String[cmdLine.size()]), workingDirectory);
		String labelString = "cargo "; //$NON-NLS-1$
		if (buildCommand.length() > 20) {
			labelString += buildCommand.substring(0, 20) + "..."; //$NON-NLS-1$
		} else {
			labelString += buildCommand;
		}
		IProcess process = DebugPlugin.newProcess(launch, restoreProcess, labelString);
		process.setAttribute(IProcess.ATTR_CMDLINE, String.join(" ", cmdLine)); //$NON-NLS-1$

		try {
			restoreProcess.waitFor();
		} catch (InterruptedException e) {
			CorrosionPlugin.logError(e);
			Thread.currentThread().interrupt();
		}
		if (restoreProcess.exitValue() != 0) { // errors will be shown in console
			return;
		}
		if (!(launch instanceof RustGDBLaunchWrapper)) {
			launch = new RustGDBLaunchWrapper(launch);
		}
		super.launch(configuration, mode, launch, monitor);
	}

	@Override
	public void launch(ISelection selection, String mode) {
		ILaunchConfiguration launchConfig = getLaunchConfiguration(
				RustLaunchDelegateTools.firstResourceFromSelection(selection));
		try {
			RustLaunchDelegateTools.launch(launchConfig, mode);
		} catch (CoreException e) {
			CorrosionPlugin.logError(e);
		}
	}

	@Override
	public void launch(IEditorPart editor, String mode) {
		ILaunchConfiguration launchConfig = getLaunchConfiguration(RustLaunchDelegateTools.resourceFromEditor(editor));
		try {
			RustLaunchDelegateTools.launch(launchConfig, mode);
		} catch (CoreException e) {
			CorrosionPlugin.logError(e);
		}
	}

	@Override
	protected ISourceLocator getSourceLocator(ILaunchConfiguration configuration, DsfSession session)
			throws CoreException {
		SourceLookupDirector locator = new SourceLookupDirector();
		String memento = configuration.getAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_MEMENTO, (String) null);
		if (memento == null) {
			locator.initializeDefaults(configuration);
		} else {
			locator.initializeFromMemento(memento, configuration);
		}
		return locator;
	}

	@Override
	protected DsfSourceLookupDirector createDsfSourceLocator(ILaunchConfiguration configuration, DsfSession session)
			throws CoreException {
		DsfSourceLookupDirector sourceLookupDirector = new DsfSourceLookupDirector(session);
		sourceLookupDirector.setSourceContainers(
				((SourceLookupDirector) getSourceLocator(configuration, session)).getSourceContainers());
		return sourceLookupDirector;
	}

	@Override
	public ILaunch getLaunch(ILaunchConfiguration configuration, String mode) throws CoreException {
		setDefaultProcessFactory(configuration); // Reset process factory to what GdbLaunch expected
		String projectName = configuration.getAttribute(ICDTLaunchConfigurationConstants.ATTR_PROJECT_NAME, ""); //$NON-NLS-1$
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		String workingDirectoryString = RustLaunchDelegateTools.performVariableSubstitution(
				configuration.getAttribute(ICDTLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY, "").trim()); //$NON-NLS-1$
		File workingDirectory = RustLaunchDelegateTools.convertToAbsolutePath(workingDirectoryString);
		if (workingDirectoryString.isEmpty() || !workingDirectory.exists() || !workingDirectory.isDirectory()) {
			workingDirectory = project.getLocation().toFile();
		}
		String executableString = RustLaunchDelegateTools.performVariableSubstitution(
				configuration.getAttribute(ICDTLaunchConfigurationConstants.ATTR_PROGRAM_NAME, "").trim()); //$NON-NLS-1$
		File executable = RustLaunchDelegateTools.convertToAbsolutePath(executableString);
		if (!executable.exists()) {
			IPath executablePath = Path.fromPortableString(executableString);
			if (project != null && executablePath.segment(0).equals(project.getName())) { // project relative path
				executable = project.getFile(executablePath.removeFirstSegments(1)).getLocation().toFile()
						.getAbsoluteFile();
			}
		}

		ILaunchConfigurationWorkingCopy wc = configuration.copy(configuration.getName());
		wc.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROGRAM_NAME, executable.getAbsolutePath());
		wc.setAttribute(ICDTLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY, workingDirectory.getAbsolutePath());
		wc.setAttribute(ICDTLaunchConfigurationConstants.ATTR_LOCATION, project.getLocation().toString());

		String stopInMainSymbol = configuration
				.getAttribute(ICDTLaunchConfigurationConstants.ATTR_DEBUGGER_STOP_AT_MAIN_SYMBOL, ""); //$NON-NLS-1$
		if (stopInMainSymbol.equals("main")) { //$NON-NLS-1$
			wc.setAttribute(ICDTLaunchConfigurationConstants.ATTR_DEBUGGER_STOP_AT_MAIN_SYMBOL, projectName + "::main"); //$NON-NLS-1$
		}

		ILaunch launch = super.getLaunch(wc.doSave(), mode);
		if (!(launch instanceof RustGDBLaunchWrapper)) {
			launch = new RustGDBLaunchWrapper(launch);
		}
		// workaround for DLTK bug: https://bugs.eclipse.org/bugs/show_bug.cgi?id=419273
		launch.setAttribute("org.eclipse.dltk.debug.debugConsole", Boolean.toString(false)); //$NON-NLS-1$
		return launch;
	}

	@Override
	protected IPath checkBinaryDetails(ILaunchConfiguration config) throws CoreException {
		return LaunchUtils.verifyProgramPath(config, null);
	}

	private static ILaunchConfiguration getLaunchConfiguration(IResource resource) {
		ILaunchConfiguration launchConfiguration = RustLaunchDelegateTools.getLaunchConfiguration(resource,
				RustLaunchDelegateTools.CORROSION_DEBUG_LAUNCH_CONFIG_TYPE);
		if (launchConfiguration instanceof ILaunchConfigurationWorkingCopy) {
			ILaunchConfigurationWorkingCopy wc = (ILaunchConfigurationWorkingCopy) launchConfiguration;
			final IProject project = resource.getProject();
			wc.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROJECT_NAME, project.getName());
			wc.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROGRAM_NAME, getDefaultExecutablePath(project)); // $NON-NLS-1$
			wc.setAttribute(ICDTLaunchConfigurationConstants.ATTR_DEBUGGER_STOP_AT_MAIN, false);
			wc.setAttribute(IGDBLaunchConfigurationConstants.ATTR_DEBUG_NAME, RustManager.getDefaultDebugger());
		}
		return launchConfiguration;
	}
}
