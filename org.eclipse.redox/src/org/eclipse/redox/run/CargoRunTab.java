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
package org.eclipse.redox.run;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.util.Arrays;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.redox.CargoProjectTester;
import org.eclipse.redox.RustManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ListSelectionDialog;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class CargoRunTab extends AbstractLaunchConfigurationTab {

	private Text projectText;
	private Text buildCommandText;
	private Combo toolchainCombo;

	private IProject project;

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(CargoRunDelegate.PROJECT_ATTRIBUTE, projectText.getText());
		configuration.setAttribute(CargoRunDelegate.BUILD_COMMAND_ATTRIBUTE, buildCommandText.getText());

		String toolchain = "";
		int toolchainIndex = toolchainCombo.getSelectionIndex();
		if (toolchainIndex != 0) {
			toolchain = toolchainCombo.getItem(toolchainIndex);
		}
		configuration.setAttribute(CargoRunDelegate.TOOLCHAIN_ATTRIBUTE, toolchain);
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

		Label locationLabel = new Label(container, SWT.NONE);
		locationLabel.setText("Toolchain:");
		locationLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

		toolchainCombo = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
		toolchainCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		String defaultString = "Deafult";
		final String defaultToolchain = RustManager.getDefaultToolchain();
		if (!defaultToolchain.isEmpty()) {
			defaultString += "(Currently " + defaultToolchain + ")";
		}
		toolchainCombo.add(defaultString);
		toolchainCombo.select(0);
		for (String toolchain : RustManager.getToolchains()) {
			toolchainCombo.add(toolchain);
		}
		toolchainCombo.addSelectionListener(widgetSelectedAdapter(e -> {
			setDirty(true);
			updateLaunchConfigurationDialog();
		}));
		new Label(container, SWT.NONE);

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
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(CargoRunDelegate.PROJECT_ATTRIBUTE, "");
		configuration.setAttribute(CargoRunDelegate.TOOLCHAIN_ATTRIBUTE, "");
		configuration.setAttribute(CargoRunDelegate.BUILD_COMMAND_ATTRIBUTE, "");
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		try {
			projectText.setText(configuration.getAttribute(CargoRunDelegate.PROJECT_ATTRIBUTE, ""));
		} catch (CoreException ce) {
			projectText.setText("");
		}
		try {
			int initializedIndex = Arrays.asList(toolchainCombo.getItems())
					.indexOf(configuration.getAttribute(CargoRunDelegate.TOOLCHAIN_ATTRIBUTE, ""));
			if (initializedIndex != -1) {
				toolchainCombo.select(initializedIndex);
			}
		} catch (CoreException ce) {
			toolchainCombo.select(0);
		}
		try {
			buildCommandText.setText(configuration.getAttribute(CargoRunDelegate.BUILD_COMMAND_ATTRIBUTE, ""));
		} catch (CoreException ce) {
			buildCommandText.setText("");
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
