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
import java.util.Iterator;

import org.eclipse.cdt.debug.core.ICDTLaunchConfigurationConstants;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

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
		if (selection instanceof IStructuredSelection) {
			Iterator<?> selectionIterator = ((IStructuredSelection) selection).iterator();
			while (selectionIterator.hasNext()) {
				Object element = selectionIterator.next();
				if (element instanceof IResource) {
					return (IResource) element;
				} else if (element instanceof IAdaptable) {
					return ((IAdaptable) element).getAdapter(IResource.class);
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
		if (launchConfig != null) {
			launchConfig.launch(mode, new NullProgressMonitor());
		}
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
			ILaunchConfiguration[] launchConfigurations = launchManager.getLaunchConfigurations(configType);
			final String projectName = resource.getProject().getName();
			String launchConfigProjectAttribute;
			if (launchConfigurationType.equals(CORROSION_DEBUG_LAUNCH_CONFIG_TYPE)) {
				launchConfigProjectAttribute = ICDTLaunchConfigurationConstants.ATTR_PROJECT_NAME;
			} else {
				launchConfigProjectAttribute = RustLaunchDelegateTools.PROJECT_ATTRIBUTE;
			}
			for (ILaunchConfiguration iLaunchConfiguration : launchConfigurations) {
				if (iLaunchConfiguration.getAttribute(launchConfigProjectAttribute, "") //$NON-NLS-1$
						.equals(projectName)) {
					return iLaunchConfiguration;
				}
			}
			String configName = launchManager.generateLaunchConfigurationName(projectName);
			return configType.newInstance(null, configName);
		} catch (CoreException e) {
			CorrosionPlugin.logError(e);
		}
		return null;
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
