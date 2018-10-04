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
package org.eclipse.corrosion;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.corrosion.ui.InputComponent;
import org.eclipse.corrosion.ui.OptionalDefaultInputComponent;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.osgi.framework.Bundle;

public class CorrosionPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	public static final String PAGE_ID = "org.eclipse.corrosion.preferencePage"; //$NON-NLS-1$
	protected static final List<String> RUST_SOURCE_OPTIONS = Arrays.asList("rustup", "other", "disabled"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	protected static final List<String> RUSTUP_TOOLCHAIN_OPTIONS = Arrays.asList("Stable", "Beta", "Nightly", "Other"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

	private IPreferenceStore store;

	private Button rustupRadioButton;
	private Button otherRadioButton;
	private Button disableRadioButton;

	@Override
	public void init(IWorkbench workbench) {
		store = doGetPreferenceStore();
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		container.setLayout(new GridLayout(4, false));

		createCommandPathsPart(container);

		Label rlsLocationLabel = new Label(container, SWT.NONE);
		rlsLocationLabel.setText(Messages.CorrosionPreferencePage_rlsLocation);
		rlsLocationLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1));

		disableRadioButton = new Button(container, SWT.RADIO);
		disableRadioButton.setText(Messages.CorrosionPreferencePage_disableRustEdition);
		disableRadioButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1));
		disableRadioButton.addSelectionListener(widgetSelectedAdapter(e -> {
			setRadioSelection(2);
		}));

		otherRadioButton = new Button(container, SWT.RADIO);
		otherRadioButton.setText(Messages.CorrosionPreferencePage_otherInstallation);
		otherRadioButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1));
		otherRadioButton.addSelectionListener(widgetSelectedAdapter(e -> {
			setRadioSelection(1);
		}));
		createOtherPart(container);

		rustupRadioButton = new Button(container, SWT.RADIO);
		rustupRadioButton.setText(Messages.CorrosionPreferencePage_useRustup);
		rustupRadioButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1));
		rustupRadioButton.addSelectionListener(widgetSelectedAdapter(e -> {
			setRadioSelection(0);
		}));

		createRustupPart(container);

		initializeContent();
		return container;
	}

	private void initializeContent() {
		int sourceIndex = RUST_SOURCE_OPTIONS
				.indexOf(store.getString(CorrosionPreferenceInitializer.RUST_SOURCE_PREFERENCE));
		setRadioSelection(sourceIndex);
		int toolchainIndex = RUSTUP_TOOLCHAIN_OPTIONS
				.indexOf(store.getString(CorrosionPreferenceInitializer.TOOLCHAIN_TYPE_PREFERENCE));
		String toolchainId = store.getString(CorrosionPreferenceInitializer.TOOLCHAIN_ID_PREFERENCE);
		otherIdInput.setValue(toolchainId);
		for (int i = 0; i < RUSTUP_TOOLCHAIN_OPTIONS.size(); i++) {
			if (RUSTUP_TOOLCHAIN_OPTIONS.get(i).equalsIgnoreCase(toolchainId.toLowerCase())) {
				toolchainIndex = i;
				break;
			}
		}
		setToolchainSelection(toolchainIndex);
		rustupInput.setValue(store.getString(CorrosionPreferenceInitializer.RUSTUP_PATHS_PREFERENCE));
		cargoInput.setValue(store.getString(CorrosionPreferenceInitializer.CARGO_PATH_PREFERENCE));
		setDefaultPathsSelection(store.getBoolean(CorrosionPreferenceInitializer.DEFAULT_PATHS_PREFERENCE));
		rlsInput.setValue(store.getString(CorrosionPreferenceInitializer.RLS_PATH_PREFERENCE));
		sysrootInput.setValue(store.getString(CorrosionPreferenceInitializer.SYSROOT_PATH_PREFERENCE));
		workingDirectoryInput.setValue(store.getString(CorrosionPreferenceInitializer.WORKING_DIRECTORY_PREFERENCE));
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return CorrosionPlugin.getDefault().getPreferenceStore();
	}

	private boolean isPageValid() {
		if (!isCommandPathsValid()) {
			return false;
		}
		int radioIndex = getRadioSelection();
		return (radioIndex == 0 && isRustupSectionValid()) || (radioIndex == 1 && isOtherInstallSectionValid())
				|| (radioIndex == 2);
	}

	private boolean isCommandPathsValid() {

		String error = ""; //$NON-NLS-1$
		if ((rustupInput.getValue().isEmpty() || cargoInput.getValue().isEmpty())) {
			error = Messages.CorrosionPreferencePage_emptyRustupCargoPath;
		} else {
			File rustup = new File(varParse(rustupInput.getValue()));
			File cargo = new File(varParse(cargoInput.getValue()));
			if (!rustup.exists() || !rustup.isFile()) {
				error = Messages.CorrosionPreferencePage_invalidRustup;
			} else if (!rustup.canExecute()) {
				error = Messages.CorrosionPreferencePage_rustupNonExecutable;
			} else if (!CorrosionPlugin.validateCommandVersion(varParse(rustupInput.getValue()),
					RustManager.RUSTUP_VERSION_FORMAT_PATTERN)) {
				error = NLS.bind(Messages.CorrosionPreferencePage_invalidVersion, "rustup"); //$NON-NLS-1$
			} else if (!cargo.exists() || !cargo.isFile()) {
				error = Messages.CorrosionPreferencePage_invalidCargo;
			} else if (!cargo.canExecute()) {
				error = Messages.CorrosionPreferencePage_cargoNonExecutable;
			} else if (!CorrosionPlugin.validateCommandVersion(varParse(cargoInput.getValue()),
					RustManager.CARGO_VERSION_FORMAT_PATTERN)) {
				error = NLS.bind(Messages.CorrosionPreferencePage_invalidVersion, "cargo"); //$NON-NLS-1$
			}
		}

		if (!error.isEmpty()) {
			setErrorMessage(error);
			setInstallRequired(true);
			return false;
		}
		File workingDirectory = new File(varParse(workingDirectoryInput.getValue()));
		if (workingDirectoryInput.getValue().isEmpty()) {
			error = Messages.CorrosionPreferencePage_emptyWorkingDirectory;
		} else if (!workingDirectory.isDirectory() || !workingDirectory.exists()) {
			error = Messages.CorrosionPreferencePage_invaildWorkingDirectory;
		}
		if (!error.isEmpty()) {
			setErrorMessage(error);
		}
		return error.isEmpty();
	}

	private boolean isRustupSectionValid() {
		if (rustupToolchainCombo.getSelectionIndex() == 3 && otherIdInput.getValue().isEmpty()) {
			setErrorMessage(Messages.CorrosionPreferencePage_emptyToolchain);
			return false;
		}
		String rlsPath = varParse(rustupInput.getValue()) + " run " + getToolchainId() + " rls"; //$NON-NLS-1$ //$NON-NLS-2$
		if (!CorrosionPlugin.validateCommandVersion(rlsPath, RustManager.RLS_VERSION_FORMAT_PATTERN)) {
			setErrorMessage(Messages.CorrosionPreferencePage_rustupMissingRLS);
			setInstallRequired(true);
			return false;
		}
		setErrorMessage(null);
		return true;
	}

	private boolean isOtherInstallSectionValid() {
		if (rlsInput.getValue().isEmpty() || sysrootInput.getValue().isEmpty()) {
			setErrorMessage(Messages.CorrosionPreferencePage_emptyPath);
			return false;
		}
		File rls = new File(varParse(rlsInput.getValue()));
		if (!rls.exists() || !rls.isFile()) {
			setErrorMessage(Messages.CorrosionPreferencePage_invalidRlsPath);
			return false;
		} else if (!rls.canExecute()) {
			setErrorMessage(Messages.CorrosionPreferencePage_rlsNonExecutable);
			return false;
		}
		if (!CorrosionPlugin.validateCommandVersion(varParse(rlsInput.getValue()),
				RustManager.RLS_VERSION_FORMAT_PATTERN)) {
			setErrorMessage(NLS.bind(Messages.CorrosionPreferencePage_invalidVersion, "rls")); //$NON-NLS-1$
			return false;
		}

		File sysrootPath = new File(varParse(sysrootInput.getValue()));
		boolean a = sysrootPath.exists();
		boolean b = sysrootPath.isDirectory();
		if (!a || !b) {
			setErrorMessage(Messages.CorrosionPreferencePage_invalidSysroot);
			return false;
		}
		setErrorMessage(null);
		return true;
	}

	private static String varParse(String unparsedString) {
		IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
		try {
			return manager.performStringSubstitution(unparsedString);
		} catch (CoreException e) {
			return unparsedString;
		}
	}

	@Override
	protected void performDefaults() {
		int sourceIndex = RUST_SOURCE_OPTIONS
				.indexOf(store.getDefaultString(CorrosionPreferenceInitializer.RUST_SOURCE_PREFERENCE));
		setRadioSelection(sourceIndex);
		int toolchainIndex = RUSTUP_TOOLCHAIN_OPTIONS
				.indexOf(store.getDefaultString(CorrosionPreferenceInitializer.TOOLCHAIN_TYPE_PREFERENCE));
		String toolchainId = store.getDefaultString(CorrosionPreferenceInitializer.TOOLCHAIN_ID_PREFERENCE);
		otherIdInput.setValue(toolchainId);
		for (int i = 0; i < RUSTUP_TOOLCHAIN_OPTIONS.size(); i++) {
			if (RUSTUP_TOOLCHAIN_OPTIONS.get(i).equalsIgnoreCase(toolchainId.toLowerCase())) {
				toolchainIndex = i;
				break;
			}
		}
		setToolchainSelection(toolchainIndex);
		setDefaultPathsSelection(store.getDefaultBoolean(CorrosionPreferenceInitializer.DEFAULT_PATHS_PREFERENCE));
		rustupInput.setValue(store.getDefaultString(CorrosionPreferenceInitializer.RUSTUP_PATHS_PREFERENCE));
		cargoInput.setValue(store.getDefaultString(CorrosionPreferenceInitializer.CARGO_PATH_PREFERENCE));
		rlsInput.setValue(store.getDefaultString(CorrosionPreferenceInitializer.RLS_PATH_PREFERENCE));
		sysrootInput.setValue(store.getDefaultString(CorrosionPreferenceInitializer.SYSROOT_PATH_PREFERENCE));
		workingDirectoryInput
				.setValue(store.getDefaultString(CorrosionPreferenceInitializer.WORKING_DIRECTORY_PREFERENCE));
		super.performDefaults();
	}

	private void setDefaultPathsSelection(boolean selection) {
		useDefaultPathsCheckbox.setSelection(selection);
		if (selection) {
			rustupInput.setValue(store.getDefaultString(CorrosionPreferenceInitializer.RUSTUP_PATHS_PREFERENCE));
			cargoInput.setValue(store.getDefaultString(CorrosionPreferenceInitializer.CARGO_PATH_PREFERENCE));
		}
		setDefaultPathsEnabled(!selection);
	}

	private void setToolchainSelection(int selection) {
		rustupToolchainCombo.select(selection);
		GridData otherIdData = (GridData) otherIdComposite.getLayoutData();
		otherIdData.exclude = selection != 3;
		otherIdComposite.getParent().layout();
		otherIdComposite.setVisible(!otherIdData.exclude);
		otherIdComposite.getParent().getParent().layout(true);
	}

	private int getRadioSelection() {
		if (rustupRadioButton.getSelection()) {
			return 0;
		}
		if (otherRadioButton.getSelection()) {
			return 1;
		}
		return 2;
	}

	private void setRadioSelection(int selection) {
		setOtherEnabled(false);
		setRustupEnabled(false);
		rustupRadioButton.setSelection(false);
		otherRadioButton.setSelection(false);
		disableRadioButton.setSelection(false);

		if (selection == 0) {
			rustupRadioButton.setSelection(true);
			setRustupEnabled(true);
		} else if (selection == 1) {
			otherRadioButton.setSelection(true);
			setOtherEnabled(true);
		} else {
			disableRadioButton.setSelection(true);
		}
		isPageValid();
	}

	@Override
	public boolean performOk() {
		int source = getRadioSelection();
		store.setValue(CorrosionPreferenceInitializer.RUST_SOURCE_PREFERENCE, RUST_SOURCE_OPTIONS.get(source));
		store.setValue(CorrosionPreferenceInitializer.WORKING_DIRECTORY_PREFERENCE, workingDirectoryInput.getValue());
		if (source == 0) {
			store.setValue(CorrosionPreferenceInitializer.TOOLCHAIN_TYPE_PREFERENCE, rustupToolchainCombo.getText());
			store.setValue(CorrosionPreferenceInitializer.DEFAULT_PATHS_PREFERENCE,
					useDefaultPathsCheckbox.getSelection());
			store.setValue(CorrosionPreferenceInitializer.RUSTUP_PATHS_PREFERENCE, rustupInput.getValue());
			store.setValue(CorrosionPreferenceInitializer.CARGO_PATH_PREFERENCE, cargoInput.getValue());

			String id = getToolchainId();
			if (!store.getString(CorrosionPreferenceInitializer.TOOLCHAIN_ID_PREFERENCE).equals(id)) {
				RustManager.setDefaultToolchain(id);
				store.setValue(CorrosionPreferenceInitializer.TOOLCHAIN_ID_PREFERENCE, id);
			}
		} else if (source == 1) {
			store.setValue(CorrosionPreferenceInitializer.RLS_PATH_PREFERENCE, rlsInput.getValue());
			store.setValue(CorrosionPreferenceInitializer.SYSROOT_PATH_PREFERENCE, sysrootInput.getValue());
		}
		return true;
	}

	private String getToolchainId() {
		int index = rustupToolchainCombo.getSelectionIndex();
		if (index == -1) {
			return ""; //$NON-NLS-1$
		} else if (index < 3) {
			return RUSTUP_TOOLCHAIN_OPTIONS.get(index).toLowerCase();
		} else {
			return otherIdInput.getValue();
		}
	}

	private Button installButton;
	private Button useDefaultPathsCheckbox;
	private InputComponent rustupInput;
	private InputComponent cargoInput;
	private InputComponent workingDirectoryInput;

	private void createCommandPathsPart(Composite container) {
		Composite parent = new Composite(container, SWT.NULL);
		parent.setLayout(new GridLayout(4, false));
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));

		useDefaultPathsCheckbox = new Button(parent, SWT.CHECK);
		useDefaultPathsCheckbox.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
		useDefaultPathsCheckbox.setText(Messages.CorrosionPreferencePage_useDefaultPathsRustupCargo);
		useDefaultPathsCheckbox.addSelectionListener(widgetSelectedAdapter(e -> {
			setDefaultPathsEnabled(!useDefaultPathsCheckbox.getSelection());
			if (useDefaultPathsCheckbox.getSelection()) {
				rustupInput.setValue(store.getDefaultString(CorrosionPreferenceInitializer.RUSTUP_PATHS_PREFERENCE));
				cargoInput.setValue(store.getDefaultString(CorrosionPreferenceInitializer.CARGO_PATH_PREFERENCE));
			}
			setValid(isPageValid());
		}));
		installButton = new Button(parent, SWT.NONE);
		installButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		installButton.addSelectionListener(widgetSelectedAdapter(e -> {
			if (Platform.getOS().equals(Platform.OS_WIN32)) {
				Program.launch("https://rustup.rs/"); //$NON-NLS-1$
			} else {
				installCommands();
			}
		}));

		rustupInput = new InputComponent(parent, Messages.CorrosionPreferencePage_Rustup, e -> setValid(isPageValid()));
		rustupInput.createComponent();
		rustupInput.createVariableSelection();
		rustupInput.createFileSelection();

		cargoInput = new InputComponent(parent, Messages.CorrosionPreferencePage_caro, e -> setValid(isPageValid()));
		cargoInput.createComponent();
		cargoInput.createVariableSelection();
		cargoInput.createFileSelection();

		workingDirectoryInput = new OptionalDefaultInputComponent(container, Messages.LaunchUI_workingDirectory,
				e -> setValid(isPageValid()),
				() -> store.getString(CorrosionPreferenceInitializer.WORKING_DIRECTORY_PREFERENCE));
		workingDirectoryInput.createComponent();
		GridData wdTextData = workingDirectoryInput.getTextGridData();
		wdTextData.widthHint = convertWidthInCharsToPixels(40);
		workingDirectoryInput.setTextGridData(wdTextData);
		workingDirectoryInput.createVariableSelection();
		workingDirectoryInput.createFolderSelection();
	}

	private boolean installInProgress = false;

	private void installCommands() {
		installButton.setText(Messages.CorrosionPreferencePage_installing);
		installButton.setEnabled(false);

		String[] command;
		try {
			Bundle bundle = CorrosionPlugin.getDefault().getBundle();
			URL fileURL = FileLocator.toFileURL(bundle.getEntry("scripts/rustup-init.sh")); //$NON-NLS-1$
			File file = new File(new URI(fileURL.getProtocol(), fileURL.getPath(), null));
			if (!file.setExecutable(true)) {
				CorrosionPlugin.showError(Messages.CorrosionPreferencePage_cannotInstallRustupCargo,
						Messages.CorrosionPreferencePage_cannotInstallRustupCargo_details);
				return;
			}
			command = new String[] { file.getAbsolutePath() + " -y" }; //$NON-NLS-1$
		} catch (IOException | URISyntaxException e) {
			CorrosionPlugin.showError(Messages.CorrosionPreferencePage_cannotInstallRustupCargo,
					Messages.CorrosionPreferencePage_cannotInstallRustupCargo_details, e);
			return;
		}

		CommandJob installCommandJob = new CommandJob(command, Messages.CorrosionPreferencePage_installingRustupCargo,
				Messages.CorrosionPreferencePage_cannotInstallRustupCargo,
				Messages.CorrosionPreferencePage_cannotInstallRustupCargo_details, 15);
		String toolchain = getToolchainId();
		installCommandJob.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(final IJobChangeEvent event) {
				if (event.getResult() == Status.OK_STATUS) {
					CorrosionPreferenceInitializer initializer = new CorrosionPreferenceInitializer();
					initializer.initializeDefaultPreferences();
					Job setDefaultToolchainJob = RustManager.setDefaultToolchain(
							toolchain.isEmpty() ? RUSTUP_TOOLCHAIN_OPTIONS.get(0).toLowerCase() : toolchain);
					setDefaultToolchainJob.addJobChangeListener(new JobChangeAdapter() {
						@Override
						public void done(final IJobChangeEvent event) {
							installInProgress = false;
							Display.getDefault().asyncExec(() -> {
								if (installButton.isDisposed()) {
									return;
								}
								setInstallRequired(false);
								performDefaults();
								setValid(isPageValid());
							});
						}
					});
				} else {
					installInProgress = false;
				}
			}
		});
		installInProgress = true;
		installCommandJob.schedule();
	}

	private void setInstallRequired(Boolean required) {
		if (installInProgress)
			return;
		if (required) {
			installButton.setText(Messages.CorrosionPreferencePage_install);
		} else {
			installButton.setText(Messages.CorrosionPreferencePage_installed);
		}
		installButton.setEnabled(required);
	}

	private Label rustupToolchainLabel;
	private Combo rustupToolchainCombo;

	private Composite otherIdComposite;
	private InputComponent otherIdInput;

	private void createRustupPart(Composite container) {

		Composite parent = new Composite(container, SWT.NULL);
		parent.setLayout(new GridLayout(4, false));
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));

		rustupToolchainLabel = new Label(parent, SWT.NONE);
		rustupToolchainLabel.setText(Messages.CorrosionPreferencePage_toolchain);
		GridData rustupToolchainGridData = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
		rustupToolchainGridData.horizontalIndent = 25;
		rustupToolchainLabel.setLayoutData(rustupToolchainGridData);

		rustupToolchainCombo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
		rustupToolchainCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		for (String toolchain : RUSTUP_TOOLCHAIN_OPTIONS) {
			rustupToolchainCombo.add(toolchain);
		}
		rustupToolchainCombo.addSelectionListener(widgetSelectedAdapter(e -> {
			setToolchainSelection(rustupToolchainCombo.getSelectionIndex());
			getShell().pack();
			getShell().layout();
			setValid(isPageValid());
		}));
		new Label(parent, SWT.NONE);

		new Label(parent, SWT.NONE);
		otherIdComposite = new Composite(parent, SWT.NULL);
		otherIdComposite.setLayout(new GridLayout(3, false));
		GridData otherIdData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		otherIdData.exclude = false;
		otherIdComposite.setLayoutData(otherIdData);
		otherIdInput = new InputComponent(otherIdComposite, Messages.CorrosionPreferencePage_id,
				e -> setValid(isPageValid()));
		otherIdInput.createComponent();
		new Label(parent, SWT.NONE);
	}

	private void setDefaultPathsEnabled(boolean enabled) {
		rustupInput.setEnabled(enabled);
		cargoInput.setEnabled(enabled);
	}

	private void setRustupEnabled(boolean enabled) {
		rustupToolchainLabel.setEnabled(enabled);
		rustupToolchainCombo.setEnabled(enabled);
		otherIdInput.setEnabled(enabled);
	}

	private InputComponent rlsInput;
	private InputComponent sysrootInput;

	private void createOtherPart(Composite container) {
		Composite parent = new Composite(container, SWT.NULL);
		parent.setLayout(new GridLayout(4, false));
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));

		GridData labelIndent = new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1);
		labelIndent.horizontalIndent = 25;

		GridData textIndent = new GridData(SWT.FILL, SWT.CENTER, true, false);
		textIndent.horizontalIndent = 50;
		textIndent.widthHint = convertWidthInCharsToPixels(50);

		rlsInput = new InputComponent(parent, Messages.CorrosionPreferencePage_rlsPath, e -> setValid(isPageValid()));
		rlsInput.createComponent();
		rlsInput.createVariableSelection();
		rlsInput.createFileSelection();
		rlsInput.setLabelGridData(labelIndent);
		rlsInput.setTextGridData(textIndent);

		sysrootInput = new InputComponent(parent, Messages.CorrosionPreferencePage_sysrootPath,
				e -> setValid(isPageValid()));
		sysrootInput.createComponent();
		sysrootInput.createVariableSelection();
		sysrootInput.createFileSelection();
		sysrootInput.setLabelGridData(labelIndent);
		sysrootInput.setTextGridData(textIndent);
	}

	private void setOtherEnabled(boolean enabled) {
		rlsInput.setEnabled(enabled);
		sysrootInput.setEnabled(enabled);
	}
}
