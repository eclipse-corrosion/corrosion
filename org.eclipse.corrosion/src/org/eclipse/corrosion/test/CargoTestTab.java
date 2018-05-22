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
package org.eclipse.corrosion.test;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.corrosion.cargo.core.CargoProjectTester;
import org.eclipse.corrosion.cargo.core.CargoTools;
import org.eclipse.corrosion.cargo.ui.OptionSelector;
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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ListSelectionDialog;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class CargoTestTab extends AbstractLaunchConfigurationTab {

	private Text projectText;
	private Text testnameText;
	private Text optionsText;
	private Text argsText;

	private IProject project;

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(CargoTestDelegate.PROJECT_ATTRIBUTE, projectText.getText());
		configuration.setAttribute(CargoTestDelegate.TEST_OPTIONS_ATTRIBUTE, optionsText.getText());
		configuration.setAttribute(CargoTestDelegate.TEST_ARGUMENTS_ATTRIBUTE, argsText.getText());
		configuration.setAttribute(CargoTestDelegate.TEST_NAME_ATTRIBUTE, testnameText.getText());
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
		createTestCommandGroup(container);
	}

	private void createTestCommandGroup(Composite container) {
		Group testCommandGroup = new Group(container, SWT.NONE);
		testCommandGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		testCommandGroup.setLayout(new GridLayout(4, false));
		testCommandGroup.setText("cargo test [options] [test name] [--] [arguments]");

		Label optionLabel = new Label(testCommandGroup, SWT.NONE);
		optionLabel.setText("Options:");
		optionLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		optionsText = new Text(testCommandGroup, SWT.BORDER);
		optionsText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		optionsText.addModifyListener(e -> {
			setDirty(true);
			updateLaunchConfigurationDialog();
		});

		Button optionButton = new Button(testCommandGroup, SWT.NONE);
		optionButton.setText("Options");
		optionButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		optionButton.addSelectionListener(widgetSelectedAdapter(e -> {
			OptionSelector dialog = new OptionSelector(optionButton.getShell(), CargoTools.getOptions("test").stream()
					.filter(o -> !o.getFlag().equals("--manifest-path")).collect(Collectors.toList()));
			dialog.open();
			String result = dialog.returnOptionSelection();
			if (result != null) {
				optionsText.append(" " + result);
				setDirty(true);
				updateLaunchConfigurationDialog();
			}
		}));
		createVariablesButton(testCommandGroup, optionsText);

		Label testnameLabel = new Label(testCommandGroup, SWT.NONE);
		testnameLabel.setText("Test name:");
		testnameLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		testnameText = new Text(testCommandGroup, SWT.BORDER);
		testnameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		testnameText.addModifyListener(e -> {
			setDirty(true);
			updateLaunchConfigurationDialog();
		});
		new Label(testCommandGroup, SWT.NONE);

		new Label(testCommandGroup, SWT.NONE);
		Label testnameExplanation = new Label(testCommandGroup, SWT.NONE);
		testnameExplanation.setText("If specified, only run tests containing this string in their names");
		testnameExplanation.setEnabled(false);
		testnameExplanation.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));

		Label argsLabel = new Label(testCommandGroup, SWT.NONE);
		argsLabel.setText("Arguments:");
		argsLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		argsText = new Text(testCommandGroup, SWT.BORDER);
		argsText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		argsText.addModifyListener(e -> {
			setDirty(true);
			updateLaunchConfigurationDialog();
		});
		createVariablesButton(testCommandGroup, argsText);
	}

	private void createVariablesButton(Composite composite, Text resultText) {
		Button variableButton = new Button(composite, SWT.NONE);
		variableButton.setText("Variables");
		variableButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		variableButton.addSelectionListener(widgetSelectedAdapter(e -> {
			StringVariableSelectionDialog variableSelector = new StringVariableSelectionDialog(
					variableButton.getShell());
			int returnCode = variableSelector.open();
			String result = variableSelector.getVariableExpression();
			if (returnCode == 0 && result != null) {
				resultText.append(result);
			}
			setDirty(true);
			updateLaunchConfigurationDialog();
		}));
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(CargoTestDelegate.PROJECT_ATTRIBUTE, "");
		configuration.setAttribute(CargoTestDelegate.TEST_OPTIONS_ATTRIBUTE, "");
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		try {
			projectText.setText(configuration.getAttribute(CargoTestDelegate.PROJECT_ATTRIBUTE, ""));
		} catch (CoreException ce) {
			projectText.setText("");
		}
		try {
			optionsText.setText(configuration.getAttribute(CargoTestDelegate.TEST_OPTIONS_ATTRIBUTE, ""));
		} catch (CoreException ce) {
			optionsText.setText("");
		}
		try {
			testnameText.setText(configuration.getAttribute(CargoTestDelegate.TEST_NAME_ATTRIBUTE, ""));
		} catch (CoreException ce) {
			testnameText.setText("");
		}
		try {
			argsText.setText(configuration.getAttribute(CargoTestDelegate.TEST_ARGUMENTS_ATTRIBUTE, ""));
		} catch (CoreException ce) {
			argsText.setText("");
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
