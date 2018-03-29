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
 *******************************************************************************/
package org.eclipse.corrosion.debug;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.cdt.debug.core.ICDTLaunchConfigurationConstants;
import org.eclipse.cdt.dsf.debug.sourcelookup.DsfSourceLookupDirector;
import org.eclipse.cdt.dsf.gdb.IGDBLaunchConfigurationConstants;
import org.eclipse.cdt.dsf.gdb.launching.GdbLaunchDelegate;
import org.eclipse.cdt.dsf.gdb.launching.LaunchUtils;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.corrosion.CorrosionPreferenceInitializer;
import org.eclipse.corrosion.RustGDBLaunchWrapper;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

public class RustDebugDelegate extends GdbLaunchDelegate implements ILaunchShortcut {
	public static final String BUILD_COMMAND_ATTRIBUTE = CorrosionPlugin.PLUGIN_ID + "BUILD_COMMAND";

	@Override
	public void launch(ILaunchConfiguration config, String mode, ILaunch launch, IProgressMonitor monitor)
			throws CoreException {
		IPreferenceStore store = CorrosionPlugin.getDefault().getPreferenceStore();
		String cargo = store.getString(CorrosionPreferenceInitializer.cargoPathPreference);
		String buildCommand = config.getAttribute(BUILD_COMMAND_ATTRIBUTE, "");
		File projectLocation = new File(
				config.getAttribute(ICDTLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY, ""));

		List<String> cmdLine = new ArrayList<>();
		cmdLine.add(cargo);
		if (buildCommand.isEmpty()) {
			buildCommand = "build";
		}
		IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
		buildCommand = manager.performStringSubstitution(buildCommand);
		cmdLine.addAll(Arrays.asList(buildCommand.replace('\n', ' ').split(" ")));
		Process restoreProcess = DebugPlugin.exec(cmdLine.toArray(new String[cmdLine.size()]), projectLocation);
		String labelString = "cargo ";
		if (buildCommand.length() > 20) {
			labelString += buildCommand.substring(0, 20) + "...";
		} else {
			labelString += buildCommand;
		}
		IProcess process = DebugPlugin.newProcess(launch, restoreProcess, labelString);
		process.setAttribute(IProcess.ATTR_CMDLINE, String.join(" ", cmdLine));

		try {
			restoreProcess.waitFor();
		} catch (InterruptedException e) {
		}
		if (restoreProcess.exitValue() != 0) { // errors will be shown in console
			return;
		}
		if (!(launch instanceof RustGDBLaunchWrapper)) {
			launch = new RustGDBLaunchWrapper(launch);
		}
		super.launch(config, mode, launch, monitor);
	}

	@Override
	public void launch(ISelection selection, String mode) {

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
		Display.getDefault().asyncExec(() -> {
			MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Unable to Launch",
					"Unable to launch Rust Project from selection.");
		});
	}

	@Override
	public void launch(IEditorPart editor, String mode) {
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

		ILaunch launch = super.getLaunch(configuration, mode);
		if (!(launch instanceof RustGDBLaunchWrapper)) {
			launch = new RustGDBLaunchWrapper(launch);
		}
		// workaround for DLTK bug: https://bugs.eclipse.org/bugs/show_bug.cgi?id=419273
		launch.setAttribute("org.eclipse.dltk.debug.debugConsole", "false");
		return launch;
	}

	@Override
	protected IPath checkBinaryDetails(ILaunchConfiguration config) throws CoreException {
		return LaunchUtils.verifyProgramPath(config, null);
	}

	private ILaunchConfiguration getLaunchConfiguration(String mode, IResource resource) {
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType configType = launchManager
				.getLaunchConfigurationType("org.eclipse.corrosion.debug.RustDebugDelegate");
		try {
			ILaunchConfiguration[] launchConfigurations = launchManager.getLaunchConfigurations(configType);
			final IProject project = resource.getProject();
			final String projectName = project.getName();

			for (ILaunchConfiguration iLaunchConfiguration : launchConfigurations) {
				if (iLaunchConfiguration.getAttribute(ICDTLaunchConfigurationConstants.ATTR_PROJECT_NAME, "")
						.equals(projectName)) {
					return iLaunchConfiguration;
				}
			}
			String configName = launchManager.generateLaunchConfigurationName(projectName);
			ILaunchConfigurationWorkingCopy wc = configType.newInstance(null, configName);
			wc.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROJECT_NAME, projectName);
			wc.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROGRAM_NAME,
					project.getLocation().toString() + "/target/debug/" + projectName);
			wc.setAttribute(ICDTLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY, project.getLocation().toString());
			wc.setAttribute(ICDTLaunchConfigurationConstants.ATTR_DEBUGGER_STOP_AT_MAIN, false);
			wc.setAttribute(IGDBLaunchConfigurationConstants.ATTR_DEBUG_NAME, "rust-gdb");
			wc.doSave();
			return wc;
		} catch (CoreException e) {
			CorrosionPlugin.logError(e);
		}
		return null;
	}
}
