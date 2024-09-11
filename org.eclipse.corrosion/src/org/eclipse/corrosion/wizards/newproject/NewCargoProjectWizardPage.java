/*********************************************************************
 * Copyright (c) 2017, 2024 Red Hat Inc. and others.
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
package org.eclipse.corrosion.wizards.newproject;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.corrosion.CorrosionPreferenceInitializer;
import org.eclipse.corrosion.Messages;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.dialogs.WorkingSetGroup;

public class NewCargoProjectWizardPage extends WizardPage {

	private Set<IWorkingSet> workingSets;
	private File directory;
	private String projectName;
	private boolean isDirectoryAndProjectLinked = true;

	protected NewCargoProjectWizardPage() {
		super(NewCargoProjectWizardPage.class.getName());
		setTitle(Messages.NewCargoProjectWizardPage_title);
		setDescription(Messages.NewCargoProjectWizardPage_description);

		setImageDescriptor(CorrosionPlugin.getDefault().getImageRegistry().getDescriptor("images/cargo.png")); //$NON-NLS-1$
	}

	public void setDirectory(File directory) {
		this.directory = directory;
	}

	public File getDirectory() {
		if (isDirectoryAndProjectLinked) {
			return directory;
		}
		return new File(directory.toString(), projectName);
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
				return "git"; //$NON-NLS-1$
			}
			if (hgRadioButton.getSelection()) {
				return "hg"; //$NON-NLS-1$
			}
			if (pijulRadioButton.getSelection()) {
				return "pijul"; //$NON-NLS-1$
			}
			if (fossilRadioButton.getSelection()) {
				return "fossil"; //$NON-NLS-1$
			}
		}
		return "none"; //$NON-NLS-1$
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

	private IPreferenceStore store = CorrosionPlugin.getDefault().getPreferenceStore();

	@Override
	public boolean isPageComplete() {
		String locationError = ""; //$NON-NLS-1$
		String projectNameError = ""; //$NON-NLS-1$
		String cargoError = ""; //$NON-NLS-1$

		File cargo = new File(store.getString(CorrosionPreferenceInitializer.CARGO_PATH_PREFERENCE));
		if (!(cargo.exists() && cargo.isFile() && cargo.canExecute())) {
			cargoError = Messages.NewCargoProjectWizardPage_cargoCommandNotFound;
		} else if (directory == null || directory.getPath().isEmpty()) {
			locationError = Messages.NewCargoProjectWizardPage_emptyDirectory;
		} else if (projectName == null || projectName.isEmpty()) {
			projectNameError = Messages.NewCargoProjectWizardPage_emptyProjectName;
		} else if (directory.isFile()) {
			locationError = Messages.NewCargoProjectWizardPage_fileExisting;
		} else if (directory.getParentFile() == null
				|| (!directory.exists() && !directory.getParentFile().canWrite())) {
			locationError = Messages.NewCargoProjectWizardPage_cannotCreateDirectory;
		} else if (directory.exists() && !directory.canWrite()) {
			locationError = Messages.NewCargoProjectWizardPage_cannotWriteInDirectory;
		} else {
			File cargoProject = new File(directory, IProjectDescription.DESCRIPTION_FILE_NAME);
			if (cargoProject.exists()) {
				try {
					IProjectDescription desc = ResourcesPlugin.getWorkspace()
							.loadProjectDescription(Path.fromOSString(cargoProject.getAbsolutePath()));
					if (!desc.getName().equals(projectName)) {
						projectNameError = Messages.NewCargoProjectWizardPage_projectNameDoesntMatchDotProject
								+ desc.getName();
					}
				} catch (CoreException e) {
					projectNameError = Messages.NewCargoProjectWizardPage_InvalidDotProjectInDirectory;
				}
			} else {
				try {
					IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
					if (project.exists() && (project.getLocation() == null
							|| !directory.getAbsoluteFile().equals(project.getLocation().toFile().getAbsoluteFile()))) {
						projectNameError = Messages.NewCargoProjectWizardPage_projectNameAlreadyUsed;
					}
				} catch (IllegalArgumentException ex) {
					projectNameError = Messages.NewCargoProjectWizardPage_invalidProjectName;
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
	private ControlDecoration locationControlDecoration;
	private ControlDecoration projectNameControlDecoration;

	private void createLocationPart(Composite container) {
		Label locationLabel = new Label(container, SWT.NONE);
		locationLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		locationLabel.setText(Messages.NewCargoProjectWizardPage_location);

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
		browseButton.setText(Messages.NewCargoProjectWizardPage_browse);
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
			e.gc.setForeground(e.widget.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
			e.gc.drawLine(0, e.height / 2, e.width / 2, e.height / 2);
			e.gc.drawLine(e.width / 2, e.height / 2, e.width / 2, e.height);
		});

		new Label(container, SWT.NONE);
		new Label(container, SWT.NONE);
		new Label(container, SWT.NONE);

		Button linkButton = new Button(container, SWT.TOGGLE);
		linkButton.setToolTipText(Messages.NewCargoProjectWizardPage_linkNameAndFolder);
		linkButton.setSelection(true);
		try (InputStream iconStream = getClass().getResourceAsStream("/icons/link_obj.png")) { //$NON-NLS-1$
			Image linkImage = new Image(linkButton.getDisplay(), iconStream);
			linkButton.setImage(linkImage);
		} catch (IOException e) {
			CorrosionPlugin.logError(e);
		}
		linkButton.addSelectionListener(widgetSelectedAdapter(s -> {
			isDirectoryAndProjectLinked = linkButton.getSelection();
			projectNameText.setEnabled(!linkButton.getSelection());
			projectNameLabel.setEnabled(!linkButton.getSelection());
			updateProjectName();
		}));

		projectNameLabel = new Label(container, SWT.NONE);
		projectNameLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		projectNameLabel.setText(Messages.NewCargoProjectWizardPage_projectName);

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
			e.gc.setForeground(e.widget.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
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
		binCheckBox.setText(Messages.NewCargoProjectWizardPage_useTemplate);
		binCheckBox.setSelection(true);
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
		vcsCheckBox.setText(Messages.NewCargoProjectWizardPage_useVCS);
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
		gitRadioButton.setText("Git"); //$NON-NLS-1$
		gitRadioButton.setEnabled(false);
		gitRadioButton.setSelection(true);
		hgRadioButton = new Button(radioGroup, SWT.RADIO);
		hgRadioButton.setText("Mercurial (hg)"); //$NON-NLS-1$
		hgRadioButton.setEnabled(false);
		pijulRadioButton = new Button(radioGroup, SWT.RADIO);
		pijulRadioButton.setText("Pijul"); //$NON-NLS-1$
		pijulRadioButton.setEnabled(false);
		fossilRadioButton = new Button(radioGroup, SWT.RADIO);
		fossilRadioButton.setText("Fossil"); //$NON-NLS-1$
		fossilRadioButton.setEnabled(false);
	}

	private WorkingSetGroup workingSetsGroup;

	private void createWorkingSetPart(Composite container) {
		Composite workingSetComposite = new Composite(container, SWT.NONE);
		GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1);
		workingSetComposite.setLayoutData(layoutData);
		workingSetComposite.setLayout(new GridLayout(1, false));
		String[] workingSetIds = new String[] { "org.eclipse.ui.resourceWorkingSetPage" }; //$NON-NLS-1$
		IStructuredSelection wsSel = null;
		if (this.workingSets != null) {
			wsSel = new StructuredSelection(this.workingSets.toArray());
		}
		this.workingSetsGroup = new WorkingSetGroup(workingSetComposite, wsSel, workingSetIds);
	}
}
