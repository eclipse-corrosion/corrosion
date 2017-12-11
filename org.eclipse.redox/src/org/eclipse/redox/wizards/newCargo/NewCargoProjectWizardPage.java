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
 *  Lucas Bullen (Red Hat Inc.) - Initial implementation
 *******************************************************************************/
package org.eclipse.redox.wizards.newCargo;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.redox.RedoxPlugin;
import org.eclipse.redox.RedoxPreferenceInitializer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.dialogs.WorkingSetGroup;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class NewCargoProjectWizardPage extends WizardPage {

	private Set<IWorkingSet> workingSets;
	private File directory;
	private String projectName;
	private Boolean isDirectoryAndProjectLinked = true;

	protected NewCargoProjectWizardPage() {
		super(NewCargoProjectWizardPage.class.getName());
		setTitle("Create a Cargo Based Rust Project");
		setDescription("Create a new Rust project, using the `cargo init` command");

		Bundle bundle = FrameworkUtil.getBundle(this.getClass());
		URL url = bundle.getEntry("images/cargo.png");
		ImageDescriptor imageDescriptor = ImageDescriptor.createFromURL(url);
		setImageDescriptor(imageDescriptor);
	}

	public void setDirectory(File directory) {
		this.directory = directory;
	}

	public File getDirectory() {
		if (isDirectoryAndProjectLinked) {
			return directory;
		} else {
			return new File(directory.toString() + "/" + projectName);
		}
	}

	public String getProjectName() {
		return projectName;
	}

	public IWorkingSet[] getWorkingSets() {
		return workingSetsGroup.getSelectedWorkingSets();
	}

	public void setWorkingSets(Set<IWorkingSet> workingSets) {
		this.workingSets = workingSets;
	}

	public boolean isBinaryTemplate() {
		return binCheckBox.getSelection();
	}

	public String getVCS() {
		if (vcsCheckBox.getSelection()) {
			if (gitRadioButton.getSelection()) {
				return "git";
			}
			if (hgRadioButton.getSelection()) {
				return "hg";
			}
			if (pijulRadioButton.getSelection()) {
				return "pijul";
			}
			if (fossilRadioButton.getSelection()) {
				return "fossil";
			}
		}
		return "none";
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		setControl(container);
		container.setLayout(new GridLayout(4, false));

		createLocationPart(container);
		createTemplatePart(container);
		createVcsPart(container);
		createWorkingSetPart(container);

		if (directory != null) {
			updateDirectory(directory.getAbsolutePath());
		}
	}

	private IPreferenceStore store = RedoxPlugin.getDefault().getPreferenceStore();

	@Override
	public boolean isPageComplete() {
		String locationError = "";
		String projectNameError = "";
		String cargoError = "";

		File cargo = new File(store.getDefaultString(RedoxPreferenceInitializer.cargoPathPreference));
		if (!(cargo.exists() && cargo.isFile() && cargo.canExecute())) {
			cargoError = "Cargo command not found. Fix path in the Rust Preferences Page.";
		} else if (directory == null || directory.getPath().isEmpty()) {
			locationError = "Please specify a directory";
		} else if (projectName == null || projectName.isEmpty()) {
			projectNameError = "Please specify project name";
		} else if (directory.isFile()) {
			locationError = "Invalid location: it is an existing file.";
		} else if (directory.getParentFile() == null
				|| (!directory.exists() && !directory.getParentFile().canWrite())) {
			locationError = "Unable to create such directory";
		} else if (directory.exists() && !directory.canWrite()) {
			locationError = "Cannot write in this directory";
		} else {
			File cargoProject = new File(directory, IProjectDescription.DESCRIPTION_FILE_NAME);
			if (cargoProject.exists()) {
				try {
					IProjectDescription desc = ResourcesPlugin.getWorkspace()
							.loadProjectDescription(Path.fromOSString(cargoProject.getAbsolutePath()));
					if (!desc.getName().equals(projectName)) {
						projectNameError = "Project name must match one in .project file: " + desc.getName();
					}
				} catch (CoreException e) {
					projectNameError = "Invalid .project file in directory";
				}
			} else {
				IProject project = null;
				try {
					project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
					if (project.exists() && (project.getLocation() == null
							|| !directory.getAbsoluteFile().equals(project.getLocation().toFile().getAbsoluteFile()))) {
						projectNameError = "Another project with same name already exists in workspace.";
					}
				} catch (IllegalArgumentException ex) {
					projectNameError = "Invalid project name";
				}
			}
		}

		String error = locationError + projectNameError + cargoError;

		if (error.isEmpty()) {
			setErrorMessage(null);
			projectNameControlDecoration.hide();
			locationControlDecoration.hide();
		} else {
			if (!locationError.isEmpty()) {
				locationControlDecoration.showHoverText(locationError);
				locationControlDecoration.show();
				projectNameControlDecoration.hide();
			} else if (!projectNameError.isEmpty()) {
				projectNameControlDecoration.showHoverText(projectNameError);
				projectNameControlDecoration.show();
				locationControlDecoration.hide();
			}
			setErrorMessage(error);
		}
		return error.isEmpty();
	}

	private Text locationText;
	private Label projectNameLabel;
	private Text projectNameText;
	private Image linkImage;
	private Button linkButton;
	private ControlDecoration locationControlDecoration;
	private ControlDecoration projectNameControlDecoration;

	private void createLocationPart(Composite container) {
		Label locationLabel = new Label(container, SWT.NONE);
		locationLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		locationLabel.setText("Location");

		Image errorImage = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR)
				.getImage();

		locationText = new Text(container, SWT.BORDER);
		GridData locationGridData = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		locationGridData.widthHint = convertWidthInCharsToPixels(50);
		locationText.setLayoutData(locationGridData);
		locationControlDecoration = new ControlDecoration(locationText, SWT.TOP | SWT.LEFT);
		locationControlDecoration.setImage(errorImage);
		locationControlDecoration.setShowOnlyOnFocus(true);
		locationText.addModifyListener(e -> {
			updateDirectory(locationText.getText());
			setPageComplete(isPageComplete());
		});

		Button browseButton = new Button(container, SWT.NONE);
		browseButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		browseButton.setText("Browse...");
		browseButton.addSelectionListener(widgetSelectedAdapter(e -> {
			DirectoryDialog dialog = new DirectoryDialog(browseButton.getShell());
			String path = dialog.open();
			if (path != null) {
				updateDirectory(path);
			}
			setPageComplete(isPageComplete());
		}));
		Composite linesAboveLink = new Composite(container, SWT.NONE);
		GridData linesAboveLinkLayoutData = new GridData(SWT.FILL, SWT.FILL);
		linesAboveLinkLayoutData.heightHint = linesAboveLinkLayoutData.widthHint = 30;
		linesAboveLink.setLayoutData(linesAboveLinkLayoutData);
		linesAboveLink.addPaintListener(e -> {
			e.gc.setForeground(((Control) e.widget).getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
			e.gc.drawLine(0, e.height / 2, e.width / 2, e.height / 2);
			e.gc.drawLine(e.width / 2, e.height / 2, e.width / 2, e.height);
		});

		new Label(container, SWT.NONE);
		new Label(container, SWT.NONE);
		new Label(container, SWT.NONE);

		linkButton = new Button(container, SWT.TOGGLE);
		linkButton.setToolTipText("Link project name and folder name");
		linkButton.setSelection(true);
		try (InputStream iconStream = getClass().getResourceAsStream("/icons/link_obj.png")) {
			linkImage = new Image(linkButton.getDisplay(), iconStream);
			linkButton.setImage(linkImage);
		} catch (IOException e) {
			RedoxPlugin.logError(e);
		}
		linkButton.addSelectionListener(widgetSelectedAdapter(s -> {
			isDirectoryAndProjectLinked = linkButton.getSelection();
			projectNameText.setEnabled(!linkButton.getSelection());
			projectNameLabel.setEnabled(!linkButton.getSelection());
			updateProjectName();
		}));

		projectNameLabel = new Label(container, SWT.NONE);
		projectNameLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		projectNameLabel.setText("Project name");

		projectNameText = new Text(container, SWT.BORDER);
		projectNameText.setEnabled(false);
		projectNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		projectNameControlDecoration = new ControlDecoration(projectNameText, SWT.TOP | SWT.LEFT);
		projectNameControlDecoration.setImage(errorImage);
		projectNameControlDecoration.setShowOnlyOnFocus(true);
		projectNameText.addModifyListener(e -> {
			updateProjectName();
			setPageComplete(isPageComplete());
		});
		Composite linesBelowLink = new Composite(container, SWT.NONE);
		GridData linesBelowLinkLayoutData = new GridData(SWT.FILL, SWT.FILL);
		linesBelowLinkLayoutData.heightHint = linesBelowLinkLayoutData.widthHint = 30;
		linesBelowLink.setLayoutData(linesAboveLinkLayoutData);
		linesBelowLink.addPaintListener(e -> {
			e.gc.setForeground(((Control) e.widget).getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
			e.gc.drawLine(0, e.height / 2, e.width / 2, e.height / 2);
			e.gc.drawLine(e.width / 2, e.height / 2, e.width / 2, 0);
		});
	}

	private void updateProjectName() {
		if (!isDirectoryAndProjectLinked) {
			projectName = projectNameText.getText();
		} else if (projectName == null || (directory != null && !projectName.equals(directory.getName()))) {
			projectName = directory.getName();
			projectNameText.setText(projectName);
		}
	}

	private void updateDirectory(String directoryPath) {
		directory = new File(directoryPath);
		if (!locationText.getText().equals(directoryPath)) {
			locationText.setText(directoryPath);
		} else if (isDirectoryAndProjectLinked) {
			updateProjectName();
		}
	}

	private Button binCheckBox;

	private void createTemplatePart(Composite container) {
		new Label(container, SWT.NONE);
		binCheckBox = new Button(container, SWT.CHECK);
		binCheckBox.setText("Use a binary application template");
		binCheckBox.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
	}

	private Button vcsCheckBox;
	private Button gitRadioButton;
	private Button hgRadioButton;
	private Button pijulRadioButton;
	private Button fossilRadioButton;

	private void createVcsPart(Composite container) {
		new Label(container, SWT.NONE);
		vcsCheckBox = new Button(container, SWT.CHECK);
		vcsCheckBox.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
		vcsCheckBox.setText("Initialize with version control system");
		vcsCheckBox.addSelectionListener(widgetSelectedAdapter(s -> {
			Boolean shouldBeEnabled = vcsCheckBox.getSelection();
			gitRadioButton.setEnabled(shouldBeEnabled);
			hgRadioButton.setEnabled(shouldBeEnabled);
			pijulRadioButton.setEnabled(shouldBeEnabled);
			fossilRadioButton.setEnabled(shouldBeEnabled);
		}));

		new Label(container, SWT.NONE);
		Composite radioGroup = new Composite(container, SWT.NONE);
		radioGroup.setLayout(new GridLayout(4, false));
		GridData radioGroupdGridData = new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1);
		radioGroupdGridData.horizontalIndent = 15;
		radioGroup.setLayoutData(radioGroupdGridData);
		gitRadioButton = new Button(radioGroup, SWT.RADIO);
		gitRadioButton.setText("Git");
		gitRadioButton.setEnabled(false);
		gitRadioButton.setSelection(true);
		hgRadioButton = new Button(radioGroup, SWT.RADIO);
		hgRadioButton.setText("Mercurial (hg)");
		hgRadioButton.setEnabled(false);
		pijulRadioButton = new Button(radioGroup, SWT.RADIO);
		pijulRadioButton.setText("Pijul");
		pijulRadioButton.setEnabled(false);
		fossilRadioButton = new Button(radioGroup, SWT.RADIO);
		fossilRadioButton.setText("Fossil");
		fossilRadioButton.setEnabled(false);
	}

	private WorkingSetGroup workingSetsGroup;

	private void createWorkingSetPart(Composite container) {
		Composite workingSetComposite = new Composite(container, SWT.NONE);
		GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1);
		workingSetComposite.setLayoutData(layoutData);
		workingSetComposite.setLayout(new GridLayout(1, false));
		String[] workingSetIds = new String[] { "org.eclipse.ui.resourceWorkingSetPage" };
		IStructuredSelection wsSel = null;
		if (this.workingSets != null) {
			wsSel = new StructuredSelection(this.workingSets.toArray());
		}
		this.workingSetsGroup = new WorkingSetGroup(workingSetComposite, wsSel, workingSetIds);
	}
}
