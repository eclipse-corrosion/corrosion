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
package org.eclipse.corrosion.ui.launch;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.corrosion.Messages;
import org.eclipse.corrosion.cargo.core.CargoProjectTester;
import org.eclipse.corrosion.cargo.core.CargoTools;
import org.eclipse.corrosion.launch.RustLaunchDelegateTools;
import org.eclipse.corrosion.ui.InputComponent;
import org.eclipse.corrosion.ui.OptionalDefaultInputComponent;
import org.eclipse.corrosion.ui.cargo.OptionSelector;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

public abstract class AbstractCargoLaunchConfigurationTab extends AbstractLaunchConfigurationTab {

	private InputComponent projectInput;
	private OptionalDefaultInputComponent workingDirectoryInput;
	private InputComponent optionsInput;
	private InputComponent argsInput;

	private IProject project;

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(RustLaunchDelegateTools.PROJECT_ATTRIBUTE, projectInput.getValue());
		configuration.setAttribute(RustLaunchDelegateTools.ARGUMENTS_ATTRIBUTE, argsInput.getValue());
		configuration.setAttribute(RustLaunchDelegateTools.OPTIONS_ATTRIBUTE, optionsInput.getValue());
		configuration.setAttribute(RustLaunchDelegateTools.WORKING_DIRECTORY_ATTRIBUTE,
				workingDirectoryInput.getValue());
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

		createExtraControlsGroup(container);

		workingDirectoryInput = new OptionalDefaultInputComponent(container, Messages.LaunchUI_workingDirectory,
				result -> {
					setDirty(true);
					updateLaunchConfigurationDialog();
				}, () -> getDefaultWorkingDirectoryPath());
		workingDirectoryInput.createComponent();
		workingDirectoryInput.createContainerSelection(() -> project);
	}

	protected abstract String getCommandGroupText();

	protected abstract String getCargoSubcommand();

	protected Group createExtraControlsGroup(Composite container) {
		Group commandGroup = new Group(container, SWT.NONE);
		commandGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1));
		commandGroup.setLayout(new GridLayout(4, false));
		commandGroup.setText(getCommandGroupText());

		optionsInput = new InputComponent(commandGroup, Messages.LaunchUI_options, e -> {
			setDirty(true);
			updateLaunchConfigurationDialog();
		});
		optionsInput.createComponent();
		optionsInput.makeSpaceForButton();

		Button optionButton = new Button(commandGroup, SWT.NONE);
		optionButton.setText(Messages.LaunchUI_optionsColon);
		optionButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		optionButton.addSelectionListener(widgetSelectedAdapter(e -> {
			OptionSelector dialog = new OptionSelector(optionButton.getShell(),
					CargoTools.getOptions(getCargoSubcommand()).stream()
							.filter(o -> !o.getFlag().equals("--manifest-path")).collect(Collectors.toList())); //$NON-NLS-1$
			dialog.open();
			String result = dialog.returnOptionSelection();
			if (result != null) {
				optionsInput.setValue(optionsInput.getValue() + " " + result); //$NON-NLS-1$
				setDirty(true);
				updateLaunchConfigurationDialog();
			}
		}));
		optionsInput.createVariableSelection();

		argsInput = new InputComponent(commandGroup, Messages.LaunchUI_arguments, e -> {
			setDirty(true);
			updateLaunchConfigurationDialog();
		});
		argsInput.createComponent();
		argsInput.createVariableSelection();
		return commandGroup;
	}

	private String getDefaultWorkingDirectoryPath() {
		if (project == null) {
			return ""; //$NON-NLS-1$
		} else {
			return project.getName();
		}
	}

	private void setProject(String projectName) {
		if (!projectInput.getValue().isEmpty()) {
			project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		} else {
			project = null;
		}
		if (workingDirectoryInput.getSelection()) {
			workingDirectoryInput.setValue(getDefaultWorkingDirectoryPath());
		}
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(RustLaunchDelegateTools.PROJECT_ATTRIBUTE, ""); //$NON-NLS-1$
		configuration.setAttribute(RustLaunchDelegateTools.ARGUMENTS_ATTRIBUTE, ""); //$NON-NLS-1$
		configuration.setAttribute(RustLaunchDelegateTools.OPTIONS_ATTRIBUTE, ""); //$NON-NLS-1$
		configuration.setAttribute(RustLaunchDelegateTools.WORKING_DIRECTORY_ATTRIBUTE, ""); //$NON-NLS-1$
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		try {
			projectInput.setValue(configuration.getAttribute(RustLaunchDelegateTools.PROJECT_ATTRIBUTE, "")); //$NON-NLS-1$
		} catch (CoreException ce) {
			projectInput.setValue(""); //$NON-NLS-1$
		}
		try {
			optionsInput.setValue(configuration.getAttribute(RustLaunchDelegateTools.OPTIONS_ATTRIBUTE, "")); //$NON-NLS-1$
		} catch (CoreException ce) {
			optionsInput.setValue(""); //$NON-NLS-1$
		}
		try {
			argsInput.setValue(configuration.getAttribute(RustLaunchDelegateTools.ARGUMENTS_ATTRIBUTE, "")); //$NON-NLS-1$
		} catch (CoreException ce) {
			argsInput.setValue(""); //$NON-NLS-1$
		}
		try {
			workingDirectoryInput
					.setValue(configuration.getAttribute(RustLaunchDelegateTools.WORKING_DIRECTORY_ATTRIBUTE, "")); //$NON-NLS-1$
		} catch (CoreException ce) {
			workingDirectoryInput.setValue(""); //$NON-NLS-1$
		}
		workingDirectoryInput
				.updateSelection(workingDirectoryInput.getValue().equals(getDefaultWorkingDirectoryPath()));
	}

	@Override
	public boolean isValid(ILaunchConfiguration launchConfig) {
		return canSave();
	}

	private static CargoProjectTester tester = new CargoProjectTester();

	@Override
	public boolean canSave() {
		if (project == null || !project.exists()
				|| !tester.test(project, CargoProjectTester.PROPERTY_NAME, null, null)) { // $NON-NLS-1$
			setErrorMessage(Messages.LaunchUI_invalidCargoProjectName);
			return false;
		}
		if (workingDirectoryInput.getValue().isEmpty()) {
			setErrorMessage(Messages.LaunchUI_invalidWorkingDirectory);
			return false;
		}
		setErrorMessage(null);
		return true;
	}

	@Override
	public String getName() {
		return Messages.LaunchUI_main;
	}
}
