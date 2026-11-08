/*********************************************************************
 * Copyright (c) 2018, 2019 Red Hat Inc. and others.
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
package org.eclipse.corrosion.launch;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.cdt.debug.core.ICDTLaunchConfigurationConstants;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.corrosion.run.CargoRunDelegate;
import org.eclipse.corrosion.test.CargoTestDelegate;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchHistory;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

@SuppressWarnings("restriction") // The launch history has no public API
public class RustLaunchDelegateTools {

	public static final String CORROSION_DEBUG_LAUNCH_CONFIG_TYPE = "org.eclipse.corrosion.debug.RustDebugDelegate"; //$NON-NLS-1$
	public static final String PROJECT_ATTRIBUTE = ICDTLaunchConfigurationConstants.ATTR_PROJECT_NAME;
	public static final String OPTIONS_ATTRIBUTE = "OPTIONS"; //$NON-NLS-1$
	public static final String ARGUMENTS_ATTRIBUTE = "ARGUMENTS"; //$NON-NLS-1$

	private RustLaunchDelegateTools() {
		throw new IllegalStateException("Utility class"); //$NON-NLS-1$
	}

	/**
	 * Returns the first resource from a structured selection
	 *
	 * @param selection
	 * @return First element in the selection or null if nothing is selected
	 */
	public static IResource firstResourceFromSelection(ISelection selection) {
		if (selection instanceof IStructuredSelection structuredSelection) {
			Iterator<?> selectionIterator = structuredSelection.iterator();
			while (selectionIterator.hasNext()) {
				IResource adapted = Adapters.adapt(selectionIterator.next(), IResource.class);
				if (adapted != null) {
					return adapted;
				}
			}
		}
		return null;
	}

	/**
	 * Returns the resource that is the input of the given editor
	 *
	 * @param editor
	 * @return The resource from the editor
	 */
	public static IResource resourceFromEditor(IEditorPart editor) {
		if (editor == null) {
			return null;
		}
		IEditorInput input = editor.getEditorInput();
		return input.getAdapter(IResource.class);
	}

	/**
	 * Converts the given relative path to a workspace resource and converts it to a
	 * {@code File} with the absolute path on the file system. If file does not
	 * exist in the workspace, the returned file will be based on the given relative
	 * path.
	 *
	 * @param path to a workspace resource
	 * @return File object of the given {@code path}, with an absolute path on the
	 *         file system
	 */
	public static File convertToAbsolutePath(String path) {
		final File file = new File(path);
		if (file.isAbsolute()) {
			return file;
		}
		final IResource filePath = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
		if (filePath != null && filePath.exists()) {
			return filePath.getLocation().toFile();
		}
		return file;
	}

	/**
	 * Launches the given launch configuration
	 *
	 * @param launchConfig
	 * @param mode
	 */
	public static void launch(ILaunchConfiguration launchConfig, String mode) throws CoreException {
		// Eclipse adds only saved configurations to the launch history. Without the
		// history, "Run" and "Debug" cannot launch this configuration again.
		while (launchConfig instanceof ILaunchConfigurationWorkingCopy wc) {
			if (wc.getParent() == null && wc.getOriginal() != null && !wc.isDirty()) {
				launchConfig = wc.getOriginal(); // nothing changed, do not write the file again
			} else {
				// A copy of a new copy saves only into its parent, so save again
				launchConfig = wc.doSave();
			}
		}
		if (launchConfig != null) {
			launchConfig.launch(mode, new NullProgressMonitor());
		}
	}

	/**
	 * Returns the Cargo run or Cargo test configuration of the project of the given
	 * resource that was launched last, from the history of the "Run" launch group.
	 *
	 * @param resource
	 * @return The last launched configuration, or null if there is none
	 */
	public static ILaunchConfiguration lastRunOrTestConfiguration(IResource resource) {
		if (resource == null || resource.getProject() == null) {
			return null;
		}
		String projectName = resource.getProject().getName();
		LaunchHistory history = DebugUIPlugin.getDefault().getLaunchConfigurationManager()
				.getLaunchHistory(IDebugUIConstants.ID_RUN_LAUNCH_GROUP);
		if (history == null) {
			return null;
		}
		for (ILaunchConfiguration configuration : history.getCompleteLaunchHistory()) {
			try {
				String type = configuration.getType().getIdentifier();
				if ((CargoRunDelegate.CARGO_RUN_LAUNCH_CONFIG_TYPE.equals(type)
						|| CargoTestDelegate.CARGO_TEST_LAUNCH_CONFIG_TYPE_ID.equals(type))
						&& configuration.getAttribute(PROJECT_ATTRIBUTE, "").equals(projectName)) { //$NON-NLS-1$
					return configuration;
				}
			} catch (CoreException e) {
				CorrosionPlugin.logError(e);
			}
		}
		return null;
	}

	/**
	 * Finds or creates a new launch configuration that matches the given search
	 * terms
	 *
	 * @param resource
	 * @param launchConfigurationType
	 * @return The matching launch configuration or a new launch configuration
	 *         working copy or null if unable to make a new one
	 */
	public static ILaunchConfiguration getLaunchConfiguration(IResource resource, String launchConfigurationType) {
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType configType = launchManager.getLaunchConfigurationType(launchConfigurationType);
		try {
			ILaunchConfiguration[] launchConfigurations = findLaunchConfigurations(resource, launchConfigurationType);
			if (launchConfigurations.length > 0) {
				return launchConfigurations[0];
			}
			String configName = launchManager.generateLaunchConfigurationName(resource.getProject().getName());
			return configType.newInstance(null, configName);
		} catch (CoreException e) {
			CorrosionPlugin.logError(e);
		}
		return null;
	}

	/**
	 * Finds the launch configurations of the given type for the project of the
	 * given resource. Launch shortcuts give these configurations to Eclipse, so
	 * that "Run" can run the last used configuration.
	 *
	 * @param resource
	 * @param launchConfigurationType
	 * @return The matching launch configurations, or an empty array if there is no
	 *         resource or no matching configuration
	 */
	public static ILaunchConfiguration[] findLaunchConfigurations(IResource resource,
			String launchConfigurationType) {
		if (resource == null || resource.getProject() == null) {
			return new ILaunchConfiguration[0];
		}
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType configType = launchManager.getLaunchConfigurationType(launchConfigurationType);
		final String projectName = resource.getProject().getName();
		List<ILaunchConfiguration> result = new ArrayList<>();
		try {
			for (ILaunchConfiguration launchConfiguration : launchManager.getLaunchConfigurations(configType)) {
				if (launchConfiguration.getAttribute(PROJECT_ATTRIBUTE, "").equals(projectName)) { //$NON-NLS-1$
					result.add(launchConfiguration);
				}
			}
		} catch (CoreException e) {
			CorrosionPlugin.logError(e);
		}
		return result.toArray(new ILaunchConfiguration[result.size()]);
	}

	public static String performVariableSubstitution(String string) throws CoreException {
		IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
		return manager.performStringSubstitution(string);
	}

	/**
	 * Opens a non blocking error dialog
	 *
	 * @param title
	 * @param message
	 */
	public static void openError(String title, String message) {
		Display.getDefault().asyncExec(() -> {
			MessageDialog dialog = new MessageDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
					title, null, message, MessageDialog.ERROR, 0, IDialogConstants.OK_LABEL);
			dialog.setBlockOnOpen(false);
			dialog.open();
		});
	}
}
