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

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.io.File;

import org.eclipse.cdt.debug.core.ICDTLaunchConfigurationConstants;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.corrosion.cargo.core.CargoProjectTester;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ListSelectionDialog;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class RustDebugTab extends AbstractLaunchConfigurationTab {

	private Text projectText;
	private Text buildCommandText;
	private Button defaultExecutablePathButton;

	private Text executablePathText;
	private Label executableLabel;
	private Button browseExecutableButton;

	private IProject project;
	private File executable;

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(RustDebugDelegate.BUILD_COMMAND_ATTRIBUTE, buildCommandText.getText());
		configuration.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROJECT_NAME, projectText.getText());
		configuration.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROGRAM_NAME, executablePathText.getText());
		if (project != null) {
			configuration.setAttribute(ICDTLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY,
					project.getLocation().toString());
		}
		setDirty(false);
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Group(parent, SWT.BORDER);
		setControl(container);
		GridLayoutFactory.swtDefaults().numColumns(3).applyTo(container);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Label projectLabel = new Label(container, SWT.NONE);
		projectLabel.setText("Project:");
		projectLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		projectText = new Text(container, SWT.BORDER);
		projectText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		projectText.addModifyListener(e -> {
			setDirty(true);
			if (!projectText.getText().isEmpty()) {
				project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectText.getText());
			} else {
				project = null;
			}
			if (defaultExecutablePathButton.getSelection()) {
				executablePathText.setText(getDefaultExecutablePath());
			}
			updateLaunchConfigurationDialog();
		});

		Button browseButton = new Button(container, SWT.NONE);
		browseButton.setText("Browse...");
		browseButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		browseButton.addSelectionListener(widgetSelectedAdapter(e -> {
			ListSelectionDialog dialog = new ListSelectionDialog(browseButton.getShell(),
					ResourcesPlugin.getWorkspace().getRoot(), new BaseWorkbenchContentProvider(),
					new WorkbenchLabelProvider(), "Select the Project:");
			dialog.setTitle("Project Selection");
			int returnCode = dialog.open();
			Object[] results = dialog.getResult();
			if (returnCode == 0 && results.length > 0) {
				setDirty(true);
				projectText.setText(((IProject) results[0]).getName());
				if (defaultExecutablePathButton.getSelection()) {
					executablePathText.setText(getDefaultExecutablePath());
				}
				updateLaunchConfigurationDialog();
			}
		}));

		Group buildCommandGroup = new Group(container, SWT.NONE);
		buildCommandGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		buildCommandGroup.setLayout(new GridLayout(2, false));
		buildCommandGroup.setText("Build Command");

		buildCommandText = new Text(buildCommandGroup, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		GridData buildCommandGridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		buildCommandGridData.heightHint = 100;
		buildCommandText.setLayoutData(buildCommandGridData);
		buildCommandText.addModifyListener(e -> {
			setDirty(true);
			updateLaunchConfigurationDialog();
		});

		Button variableButton = new Button(buildCommandGroup, SWT.NONE);
		variableButton.setText("Variables");
		variableButton.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
		variableButton.addSelectionListener(widgetSelectedAdapter(e -> {
			StringVariableSelectionDialog variableSelector = new StringVariableSelectionDialog(
					variableButton.getShell());
			int returnCode = variableSelector.open();
			String result = variableSelector.getVariableExpression();
			if (returnCode == 0 && result != null) {
				buildCommandText.append(result);
			}
			setDirty(true);
			updateLaunchConfigurationDialog();
		}));

		defaultExecutablePathButton = new Button(container, SWT.CHECK);
		defaultExecutablePathButton.setText("Use default path to executable");
		defaultExecutablePathButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
		defaultExecutablePathButton.addSelectionListener(
				widgetSelectedAdapter(e -> setDefaultExecutionPath(defaultExecutablePathButton.getSelection())));
		defaultExecutablePathButton.setSelection(true);

		executableLabel = new Label(container, SWT.NONE);
		executableLabel.setText("Executable:");
		executableLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		executablePathText = new Text(container, SWT.BORDER);
		executablePathText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		executablePathText.addModifyListener(e -> {
			setDirty(true);
			executable = new File(executablePathText.getText());
			updateLaunchConfigurationDialog();
		});

		browseExecutableButton = new Button(container, SWT.NONE);
		browseExecutableButton.setText("Browse...");
		browseExecutableButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		browseExecutableButton.addSelectionListener(widgetSelectedAdapter(e -> {
			FileDialog dialog = new FileDialog(browseExecutableButton.getShell());
			String result = dialog.open();
			if (result != null) {
				projectText.setText(result);
				setDirty(true);
				updateLaunchConfigurationDialog();
			}
		}));
		setDefaultExecutionPath(true);
	}

	private void setDefaultExecutionPath(boolean state) {
		defaultExecutablePathButton.setEnabled(state);
		executableLabel.setEnabled(!state);
		executablePathText.setEnabled(!state);
		browseExecutableButton.setEnabled(!state);
		if (state) {
			executablePathText.setText(getDefaultExecutablePath());
		}
	}

	private String getDefaultExecutablePath() {
		if (project == null) {
			return "";
		} else {
			return project.getLocation().toString() + "/target/debug/" + project.getName();
		}
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(RustDebugDelegate.BUILD_COMMAND_ATTRIBUTE, "build");
		configuration.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROJECT_NAME, "");
		configuration.setAttribute(ICDTLaunchConfigurationConstants.ATTR_PROGRAM_NAME, "");
		configuration.setAttribute(ICDTLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY, "");
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		try {
			projectText.setText(configuration.getAttribute(ICDTLaunchConfigurationConstants.ATTR_PROJECT_NAME, ""));
		} catch (CoreException ce) {
			projectText.setText("");
		}
		try {
			buildCommandText.setText(configuration.getAttribute(RustDebugDelegate.BUILD_COMMAND_ATTRIBUTE, "build"));
		} catch (CoreException ce) {
			buildCommandText.setText("");
		}
		try {
			executablePathText
					.setText(configuration.getAttribute(ICDTLaunchConfigurationConstants.ATTR_PROGRAM_NAME, ""));
		} catch (CoreException ce) {
			executablePathText.setText("");
		}
		setDefaultExecutionPath(executablePathText.getText().equals(getDefaultExecutablePath()));
	}

	@Override
	public boolean isValid(ILaunchConfiguration launchConfig) {
		return canSave();
	}

	private static CargoProjectTester tester = new CargoProjectTester();

	@Override
	public boolean canSave() {
		if (buildCommandText.getText().isEmpty()) {
			setErrorMessage("Cargo Build command cannot be empty");
			return false;
		}
		if (project == null || !project.exists() || !tester.test(project, "isCargoProject", null, null)) {
			setErrorMessage("Input a valid cargo project name");
			return false;
		}
		if (executable == null || !executable.exists() || !executable.canExecute()) {
			setErrorMessage("Input a valid project executable path");
			return false;
		}
		if (!executable.canExecute()) {
			setErrorMessage("Executable File is not executable");
			return false;
		}
		setErrorMessage(null);
		return true;
	}

	@Override
	public String getName() {
		return "Main";
	}
}
