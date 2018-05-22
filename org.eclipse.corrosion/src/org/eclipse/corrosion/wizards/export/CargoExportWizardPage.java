/*********************************************************************
 * Copyright (c) 2017, 2018 Red Hat Inc. and others.
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
package org.eclipse.corrosion.wizards.export;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.io.File;
import java.net.URL;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.corrosion.CorrosionPreferenceInitializer;
import org.eclipse.corrosion.RustManager;
import org.eclipse.corrosion.cargo.core.CargoProjectTester;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ListSelectionDialog;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class CargoExportWizardPage extends WizardPage {
	private IProject project;

	private Text projectText;
	private Label outputLocationLabel;
	private ControlDecoration projectControlDecoration;
	private Combo toolchainCombo;
	private Button allowDirtyCheckbox;
	private Button noVerifyCheckbox;
	private Button noMetadataCheckbox;

	public IProject getProject() {
		return project;
	}

	public String getToolchain() {
		int toolchainIndex = toolchainCombo.getSelectionIndex();
		if (toolchainIndex != 0) {
			return toolchainCombo.getItem(toolchainIndex);
		}
		return "";
	}

	public Boolean noVerify() {
		return noVerifyCheckbox.getSelection();
	}

	public Boolean noMetadata() {
		return noMetadataCheckbox.getSelection();
	}

	public Boolean allowDirty() {
		return allowDirtyCheckbox.getSelection();
	}

	protected CargoExportWizardPage(IProject project) {
		super(CargoExportWizardPage.class.getName());
		setTitle("Package Cargo Based Rust Project");
		setDescription("Package a Cargo based Rust project into a crate, using the `cargo package` command");
		Bundle bundle = FrameworkUtil.getBundle(this.getClass());
		URL url = bundle.getEntry("images/cargo.png");
		ImageDescriptor imageDescriptor = ImageDescriptor.createFromURL(url);
		setImageDescriptor(imageDescriptor);

		if (project != null) {
			this.project = project;
		}
	}

	@Override
	public void createControl(Composite parent) {
		Image errorImage = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR)
				.getImage();

		Composite container = new Composite(parent, SWT.BORDER);
		setControl(container);
		GridLayoutFactory.swtDefaults().numColumns(3).applyTo(container);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Label projectLabelLabel = new Label(container, SWT.NONE);
		projectLabelLabel.setText("Project:");
		projectLabelLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		projectText = new Text(container, SWT.BORDER);
		projectText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		if (project != null) {
			projectText.setText(project.getName());
		}
		projectText.addModifyListener(e -> {
			if (!projectText.getText().isEmpty()) {
				project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectText.getText());
			} else {
				project = null;
			}
			setPageComplete(isPageComplete());
		});
		projectControlDecoration = new ControlDecoration(projectText, SWT.TOP | SWT.LEFT);
		projectControlDecoration.setImage(errorImage);

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
				project = (IProject) results[0];
				projectText.setText(project.getName());
			}
		}));

		new Label(container, SWT.NONE);
		outputLocationLabel = new Label(container, SWT.NONE);
		outputLocationLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

		Label toolchainLabel = new Label(container, SWT.NONE);
		toolchainLabel.setText("Toolchain:");
		toolchainLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

		toolchainCombo = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
		toolchainCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		String defaultString = "Default";
		final String defaultToolchain = RustManager.getDefaultToolchain();
		if (!defaultToolchain.isEmpty()) {
			defaultString += "(Currently " + defaultToolchain + ")";
		}
		toolchainCombo.add(defaultString);
		toolchainCombo.select(0);
		for (String toolchain : RustManager.getToolchains()) {
			toolchainCombo.add(toolchain);
		}
		new Label(container, SWT.NONE);

		noVerifyCheckbox = new Button(container, SWT.CHECK);
		noVerifyCheckbox.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
		noVerifyCheckbox.setText("Don't verify the contents by building them");

		noMetadataCheckbox = new Button(container, SWT.CHECK);
		noMetadataCheckbox.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
		noMetadataCheckbox.setText("Ignore warnings about a lack of human-usable metadata");

		allowDirtyCheckbox = new Button(container, SWT.CHECK);
		allowDirtyCheckbox.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
		allowDirtyCheckbox.setText("Allow dirty working directories to be packaged");
		setPageComplete(isPageComplete());
	}

	private IPreferenceStore store = CorrosionPlugin.getDefault().getPreferenceStore();
	private static CargoProjectTester tester = new CargoProjectTester();

	@Override
	public boolean isPageComplete() {
		File cargo = new File(store.getString(CorrosionPreferenceInitializer.cargoPathPreference));
		if (!(cargo.exists() && cargo.isFile() && cargo.canExecute())) {
			setErrorMessage("Cargo command not found. Fix path in the Rust Preferences Page.");
			return false;
		}
		if (!(project != null && project.exists() && tester.test(project, "isCargoProject", null, null))) {
			setErrorMessage("Please specify a valid cargo project");
			outputLocationLabel.setText("");
			projectControlDecoration.show();
			return false;
		}
		outputLocationLabel.setText("Crate will be created in: " + project.getName() + "/target/package/");
		projectControlDecoration.hide();
		setErrorMessage(null);
		return true;
	}
}
