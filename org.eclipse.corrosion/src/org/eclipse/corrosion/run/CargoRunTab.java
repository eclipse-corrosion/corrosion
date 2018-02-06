/*********************************************************************
 * Copyright (c) 2017 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Lucas Bullen   (Red Hat Inc.) - Initial implementation
 *******************************************************************************/
package org.eclipse.corrosion.run;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.corrosion.CargoProjectTester;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ListSelectionDialog;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class CargoRunTab extends AbstractLaunchConfigurationTab {

	private Text projectText;
	private Text runCommandText;

	private IProject project;

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(CargoRunDelegate.PROJECT_ATTRIBUTE, projectText.getText());
		configuration.setAttribute(CargoRunDelegate.RUN_COMMAND_ATTRIBUTE, runCommandText.getText());
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
				projectText.setText(((IProject) results[0]).getName());
				setDirty(true);
				updateLaunchConfigurationDialog();
			}
		}));

		Group runCommandGroup = new Group(container, SWT.NONE);
		runCommandGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		runCommandGroup.setLayout(new GridLayout(2, false));
		runCommandGroup.setText("Run Command");

		runCommandText = new Text(runCommandGroup, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		GridData buildCommandGridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		buildCommandGridData.heightHint = 100;
		runCommandText.setLayoutData(buildCommandGridData);
		runCommandText.addModifyListener(e -> {
			setDirty(true);
			updateLaunchConfigurationDialog();
		});

		Button variableButton = new Button(runCommandGroup, SWT.NONE);
		variableButton.setText("Variables");
		variableButton.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
		variableButton.addSelectionListener(widgetSelectedAdapter(e -> {
			StringVariableSelectionDialog variableSelector = new StringVariableSelectionDialog(
					variableButton.getShell());
			int returnCode = variableSelector.open();
			String result = variableSelector.getVariableExpression();
			if (returnCode == 0 && result != null) {
				runCommandText.append(result);
			}
			setDirty(true);
			updateLaunchConfigurationDialog();
		}));
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(CargoRunDelegate.PROJECT_ATTRIBUTE, "");
		configuration.setAttribute(CargoRunDelegate.RUN_COMMAND_ATTRIBUTE, "run");
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		try {
			projectText.setText(configuration.getAttribute(CargoRunDelegate.PROJECT_ATTRIBUTE, ""));
		} catch (CoreException ce) {
			projectText.setText("");
		}
		try {
			runCommandText.setText(configuration.getAttribute(CargoRunDelegate.RUN_COMMAND_ATTRIBUTE, "run"));
		} catch (CoreException ce) {
			runCommandText.setText("");
		}
	}

	@Override
	public boolean isValid(ILaunchConfiguration launchConfig) {
		return canSave();
	}

	private static CargoProjectTester tester = new CargoProjectTester();

	@Override
	public boolean canSave() {
		if (project != null && project.exists() && tester.test(project, "isCargoProject", null, null)) {
			setErrorMessage(null);
			return true;
		}
		setErrorMessage("Input a valid cargo project name");
		return false;
	}

	@Override
	public String getName() {
		return "Main";
	}
}
