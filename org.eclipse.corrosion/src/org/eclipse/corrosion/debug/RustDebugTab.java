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
 *  Max Bureck (Fraunhofer FOKUS) - Added icon
 *******************************************************************************/
package org.eclipse.corrosion.debug;

import static org.eclipse.corrosion.debug.DebugUtil.getDefaultExecutablePath;

import org.eclipse.cdt.debug.core.ICDTLaunchConfigurationConstants;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.corrosion.Messages;
import org.eclipse.corrosion.cargo.core.CargoProjectTester;
import org.eclipse.corrosion.ui.InputComponent;
import org.eclipse.corrosion.ui.OptionalDefaultInputComponent;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

public class RustDebugTab extends AbstractLaunchConfigurationTab {

	private InputComponent projectInput;
	private InputComponent buildInput;
	private OptionalDefaultInputComponent executableInput;

	private IProject project;

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(RustDebugDelegate.BUILD_COMMAND_ATTRIBUTE, buildInput.getValue());
		configuration.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROJECT_NAME, projectInput.getValue());
		configuration.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROGRAM_NAME, executableInput.getValue());
		setDirty(false);
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Group(parent, SWT.BORDER);
		setControl(container);
		GridLayoutFactory.swtDefaults().numColumns(4).applyTo(container);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		projectInput = new InputComponent(container, Messages.LaunchUI_project, result -> {
			setDirty(true);
			setProject(projectInput.getValue());
			updateLaunchConfigurationDialog();
		});
		projectInput.createComponent();
		projectInput.createProjectSelection();

		ModifyListener updateListener = result -> {
			setDirty(true);
			updateLaunchConfigurationDialog();
		};

		buildInput = new InputComponent(container, Messages.RustDebugTab_buildCommand, updateListener);
		buildInput.createComponent();
		buildInput.createVariableSelection();

		executableInput = new OptionalDefaultInputComponent(container, Messages.RustDebugTab_Executable, updateListener,
				() -> getDefaultExecutablePath(project));
		executableInput.createComponent();
		executableInput.createResourceSelection(() -> project);
	}

	private void setProject(String projectName) {
		if (projectName != null && !projectName.isEmpty()) {
			project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		} else {
			project = null;
		}
		if (executableInput.getSelection()) {
			executableInput.setValue(getDefaultExecutablePath(project));
		}
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(RustDebugDelegate.BUILD_COMMAND_ATTRIBUTE, "build"); //$NON-NLS-1$
		configuration.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROJECT_NAME, ""); //$NON-NLS-1$
		configuration.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROGRAM_NAME, ""); //$NON-NLS-1$
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		try {
			projectInput.setValue(configuration.getAttribute(ICDTLaunchConfigurationConstants.ATTR_PROJECT_NAME, "")); //$NON-NLS-1$
		} catch (CoreException ce) {
			projectInput.setValue(""); //$NON-NLS-1$
		}
		setProject(projectInput.getValue());
		try {
			buildInput.setValue(configuration.getAttribute(RustDebugDelegate.BUILD_COMMAND_ATTRIBUTE, "build")); //$NON-NLS-1$
		} catch (CoreException ce) {
			buildInput.setValue("build"); //$NON-NLS-1$
		}
		try {
			executableInput
					.setValue(configuration.getAttribute(ICDTLaunchConfigurationConstants.ATTR_PROGRAM_NAME, "")); //$NON-NLS-1$
		} catch (CoreException ce) {
			executableInput.setValue(""); //$NON-NLS-1$
		}
		executableInput.updateSelection(executableInput.getValue().equals(getDefaultExecutablePath(project)));
	}

	@Override
	public boolean isValid(ILaunchConfiguration launchConfig) {
		return canSave();
	}

	private static CargoProjectTester tester = new CargoProjectTester();

	@Override
	public boolean canSave() {
		if (project == null || !project.exists()
				|| !tester.test(project, CargoProjectTester.PROPERTY_NAME, null, null)) {
			setErrorMessage(Messages.LaunchUI_invalidCargoProjectName);
			return false;
		}
		if (buildInput.getValue().isEmpty()) {
			setErrorMessage(Messages.RustDebugTab_cargoCommandConnotBeEmpty);
			return false;
		}
		if (executableInput.getValue().isEmpty()) {
			setErrorMessage(Messages.RustDebugTab_InvalidProjectExecutablePath);
			return false;
		}
		setErrorMessage(null);
		return true;
	}

	@Override
	public String getName() {
		return Messages.LaunchUI_main;
	}

	@Override
	public Image getImage() {
		return CorrosionPlugin.getDefault().getImageRegistry().get("icons/rustEditorIcon.png"); //$NON-NLS-1$
	}
}
