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
package org.eclipse.redox;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.osgi.framework.Bundle;

public class RedoxPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	public static final String PAGE_ID = "org.eclipse.redox.preferencePage";
	public static final List<String> RUST_SOURCE_OPTIONS = Arrays.asList("rustup", "other", "disabled");
	public static final List<String> RUSTUP_TOOLCHAIN_OPTIONS = Arrays.asList("Stable", "Beta", "Nightly", "Other");

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
		container.setLayout(new GridLayout(2, false));

		createCommandPathsPart(container);

		Label rlsLocationLabel = new Label(container, SWT.NONE);
		rlsLocationLabel.setText("Rust Language Server Location:");
		rlsLocationLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));

		disableRadioButton = new Button(container, SWT.RADIO);
		disableRadioButton.setText("Disable Rust editor");
		disableRadioButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		disableRadioButton.addSelectionListener(widgetSelectedAdapter(e -> {
			setRadioSelection(2);
		}));

		otherRadioButton = new Button(container, SWT.RADIO);
		otherRadioButton.setText("Other installation");
		otherRadioButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		otherRadioButton.addSelectionListener(widgetSelectedAdapter(e -> {
			setRadioSelection(1);
		}));
		createOtherPart(container);

		rustupRadioButton = new Button(container, SWT.RADIO);
		rustupRadioButton.setText("Use Rustup");
		rustupRadioButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		rustupRadioButton.addSelectionListener(widgetSelectedAdapter(e -> {
			setRadioSelection(0);
		}));

		createRustupPart(container);

		initializeContent();
		return container;
	}

	private void initializeContent() {
		int sourceIndex = RUST_SOURCE_OPTIONS.indexOf(store.getString(RedoxPreferenceInitializer.rustSourcePreference));
		setRadioSelection(sourceIndex);
		int toolchainIndex = RUSTUP_TOOLCHAIN_OPTIONS
				.indexOf(store.getString(RedoxPreferenceInitializer.toolchainTypePreference));
		String toolchainId = store.getString(RedoxPreferenceInitializer.toolchainIdPreference);
		otherIdText.setText(toolchainId);
		for (int i = 0; i < RUSTUP_TOOLCHAIN_OPTIONS.size(); i++) {
			if (RUSTUP_TOOLCHAIN_OPTIONS.get(i).toLowerCase().equals(toolchainId.toLowerCase())) {
				toolchainIndex = i;
				break;
			}
		}
		setToolchainSelection(toolchainIndex);
		rustupPathText.setText(store.getString(RedoxPreferenceInitializer.rustupPathPreference));
		cargoPathText.setText(store.getString(RedoxPreferenceInitializer.cargoPathPreference));
		setDefaultPathsSelection(store.getBoolean(RedoxPreferenceInitializer.defaultPathsPreference));
		rlsPathText.setText(store.getString(RedoxPreferenceInitializer.rlsPathPreference));
		sysrootPathText.setText(store.getString(RedoxPreferenceInitializer.sysrootPathPreference));
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return RedoxPlugin.getDefault().getPreferenceStore();
	}

	private boolean isPageValid() {
		int radioIndex = getRadioSelection();
		if (!isCommandPathsValid()) {
			return false;
		}
		if (radioIndex == 0 && isRustupSectionValid()) {
			return true;
		} else if (radioIndex == 1 && isOtherInstallSectionValid()) {
			return true;
		} else if (radioIndex == 2) {
			return true;
		}
		return false;
	}

	private boolean isCommandPathsValid() {
		String error = "";
		if ((rustupPathText.getText().isEmpty() || cargoPathText.getText().isEmpty())) {
			error = "Rustup and Cargo paths cannot be empty";
		} else {
			File rustup = new File(rustupPathText.getText());
			File cargo = new File(cargoPathText.getText());
			if (!rustup.exists() || !rustup.isFile()) {
				error = "Input a valid `rustup` command path";
			} else if (!rustup.canExecute()) {
				error = "Inputted `rustup` command is not executable";
			} else if (!cargo.exists() || !cargo.isFile()) {
				error = "Input a valid `cargo` command path";
			} else if (!cargo.canExecute()) {
				error = "Inputted `cargo` command is not executable";
			}
		}

		if (error.isEmpty()) {
			setErrorMessage(null);
		} else {
			setErrorMessage(error);
		}
		setInstallRequired(!error.isEmpty());
		return error.isEmpty();
	}

	private boolean isRustupSectionValid() {
		if (rustupToolchainCombo.getSelectionIndex() == 3 && otherIdText.getText().isEmpty()) {
			setErrorMessage("Toolchain ID cannot be empty");
			return false;
		}
		setErrorMessage(null);
		return true;
	}

	private boolean isOtherInstallSectionValid() {
		if (rlsPathText.getText().isEmpty() || sysrootPathText.getText().isEmpty()) {
			setErrorMessage("Paths cannot be empty");
			return false;
		}
		File rls = new File(rlsPathText.getText());
		if (!rls.exists() || !rls.isFile()) {
			setErrorMessage("Input a valid path to the Rust Language Server (rls)");
			return false;
		} else if (!rls.canExecute()) {
			setErrorMessage("Inputted `rls` command is not executable");
			return false;
		}

		File sysrootPath = new File(sysrootPathText.getText());
		boolean a = sysrootPath.exists();
		boolean b = sysrootPath.isDirectory();
		if (!a || !b) {
			setErrorMessage("Input a valid path to the sysroot directory");
			return false;
		}
		setErrorMessage(null);
		return true;
	}

	@Override
	protected void performDefaults() {
		int sourceIndex = RUST_SOURCE_OPTIONS
				.indexOf(store.getDefaultString(RedoxPreferenceInitializer.rustSourcePreference));
		setRadioSelection(sourceIndex);
		int toolchainIndex = RUSTUP_TOOLCHAIN_OPTIONS
				.indexOf(store.getDefaultString(RedoxPreferenceInitializer.toolchainTypePreference));
		String toolchainId = store.getDefaultString(RedoxPreferenceInitializer.toolchainIdPreference);
		otherIdText.setText(toolchainId);
		for (int i = 0; i < RUSTUP_TOOLCHAIN_OPTIONS.size(); i++) {
			if (RUSTUP_TOOLCHAIN_OPTIONS.get(i).toLowerCase().equals(toolchainId.toLowerCase())) {
				toolchainIndex = i;
				break;
			}
		}
		setToolchainSelection(toolchainIndex);
		useDefaultPathsCheckbox
				.setSelection(store.getDefaultBoolean(RedoxPreferenceInitializer.defaultPathsPreference));
		rustupPathText.setText(store.getDefaultString(RedoxPreferenceInitializer.rustupPathPreference));
		cargoPathText.setText(store.getDefaultString(RedoxPreferenceInitializer.cargoPathPreference));
		rlsPathText.setText(store.getDefaultString(RedoxPreferenceInitializer.rlsPathPreference));
		sysrootPathText.setText(store.getDefaultString(RedoxPreferenceInitializer.sysrootPathPreference));
		super.performDefaults();
	}

	private void setDefaultPathsSelection(boolean selection) {
		useDefaultPathsCheckbox.setSelection(selection);
		if (selection) {
			rustupPathText.setText(store.getDefaultString(RedoxPreferenceInitializer.rustupPathPreference));
			cargoPathText.setText(store.getDefaultString(RedoxPreferenceInitializer.cargoPathPreference));
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
		store.setValue(RedoxPreferenceInitializer.rustSourcePreference, RUST_SOURCE_OPTIONS.get(source));
		if (source == 0) {
			String id = getToolchainId();
			store.setValue(RedoxPreferenceInitializer.toolchainTypePreference, rustupToolchainCombo.getText());
			store.setValue(RedoxPreferenceInitializer.toolchainIdPreference, id);
			store.setValue(RedoxPreferenceInitializer.defaultPathsPreference, useDefaultPathsCheckbox.getSelection());
			store.setValue(RedoxPreferenceInitializer.rustupPathPreference, rustupPathText.getText());
			store.setValue(RedoxPreferenceInitializer.cargoPathPreference, cargoPathText.getText());
			RustManager.setDefaultToolchain(id);
		} else if (source == 1) {
			store.setValue(RedoxPreferenceInitializer.rlsPathPreference, rlsPathText.getText());
			store.setValue(RedoxPreferenceInitializer.sysrootPathPreference, sysrootPathText.getText());
		}
		return true;
	}

	private String getToolchainId() {
		int index = rustupToolchainCombo.getSelectionIndex();
		if (index == -1) {
			return "";
		} else if (index < 3) {
			return RUSTUP_TOOLCHAIN_OPTIONS.get(index).toLowerCase();
		} else {
			return otherIdText.getText();
		}
	}

	private Button installButton;

	private Button useDefaultPathsCheckbox;

	private Label rustupLabel;
	private Text rustupPathText;
	private Button rustupBrowseButton;

	private Label cargoLabel;
	private Text cargoPathText;
	private Button cargoBrowseButton;

	private void createCommandPathsPart(Composite container) {
		Composite parent = new Composite(container, SWT.NULL);
		parent.setLayout(new GridLayout(3, false));
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

		GridData textGridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		GridData buttonGridData = new GridData(SWT.LEFT, SWT.CENTER, false, false);

		useDefaultPathsCheckbox = new Button(parent, SWT.CHECK);
		useDefaultPathsCheckbox.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		useDefaultPathsCheckbox.setText("Use default paths (rustup and cargo required for most features)");
		useDefaultPathsCheckbox.addSelectionListener(widgetSelectedAdapter(e -> {
			setDefaultPathsEnabled(!useDefaultPathsCheckbox.getSelection());
			if (useDefaultPathsCheckbox.getSelection()) {
				rustupPathText.setText(store.getDefaultString(RedoxPreferenceInitializer.rustupPathPreference));
				cargoPathText.setText(store.getDefaultString(RedoxPreferenceInitializer.cargoPathPreference));
			}
			setValid(isPageValid());
		}));
		installButton = new Button(parent, SWT.NONE);
		installButton.setLayoutData(buttonGridData);
		installButton.addSelectionListener(widgetSelectedAdapter(e -> {
			installCommands();
		}));

		rustupLabel = new Label(parent, SWT.NONE);
		rustupLabel.setText("Rustup:");
		rustupLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		rustupPathText = new Text(parent, SWT.BORDER);
		rustupPathText.setLayoutData(textGridData);
		rustupPathText.addModifyListener(e -> {
			setValid(isPageValid());
		});

		rustupBrowseButton = new Button(parent, SWT.NONE);
		rustupBrowseButton.setLayoutData(buttonGridData);
		rustupBrowseButton.setText("Browse...");
		rustupBrowseButton.addSelectionListener(widgetSelectedAdapter(e -> {
			FileDialog dialog = new FileDialog(rustupBrowseButton.getShell());
			String path = dialog.open();
			if (path != null) {
				rustupPathText.setText(path);
			}
		}));

		cargoLabel = new Label(parent, SWT.NONE);
		cargoLabel.setText("Cargo:");
		cargoLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		cargoPathText = new Text(parent, SWT.BORDER);
		cargoPathText.setLayoutData(textGridData);
		cargoPathText.addModifyListener(e -> {
			setValid(isPageValid());
		});

		cargoBrowseButton = new Button(parent, SWT.NONE);
		cargoBrowseButton.setLayoutData(buttonGridData);
		cargoBrowseButton.setText("Browse...");
		cargoBrowseButton.addSelectionListener(widgetSelectedAdapter(e -> {
			FileDialog dialog = new FileDialog(cargoBrowseButton.getShell());
			String path = dialog.open();
			if (path != null) {
				cargoPathText.setText(path);
			}
		}));
	}

	private void installCommands() {
		installButton.setText("Installling");
		installButton.setEnabled(false);
		Job.create("Installing Rustup and Cargo", (ICoreRunnable) monitor -> {
			try {
				Bundle bundle = Platform.getBundle("org.eclipse.redox");
				URL fileURL = bundle.getEntry("scripts/rustup.sh");
				File file = new File(FileLocator.resolve(fileURL).toURI());
				String[] command = new String[] { "/bin/sh", file.getAbsolutePath() };
				ProcessBuilder builder = new ProcessBuilder(command);
				Process process = builder.start();
				if (process.waitFor() == 0) {
					RedoxPreferenceInitializer initializer = new RedoxPreferenceInitializer();
					initializer.initializeDefaultPreferences();
					Display.getDefault().asyncExec(() -> {
						setInstallRequired(false);
						performDefaults();
						setValid(isPageValid());
					});
					RustManager.setDefaultToolchain("beta");
					return;
				}
			} catch (InterruptedException | URISyntaxException | IOException e) {
				// will be caught with dialog
			}
			setInstallRequired(true);
			Display.getDefault().asyncExec(() -> {
				MessageDialog.openError(getShell(), "Cannot install Rust and Cargo",
						"We were unable to perform the install, you can do so manually by going to doc.crates.io");
			});
		}).schedule();
	}

	private void setInstallRequired(Boolean required) {
		if (required) {
			installButton.setText("Install");
		} else {
			installButton.setText("Installed");
		}
		installButton.setEnabled(required);
	}

	private Label rustupToolchainLabel;
	private Combo rustupToolchainCombo;

	private Composite otherIdComposite;
	private Label otherIdLabel;
	private Text otherIdText;

	private void createRustupPart(Composite container) {

		Composite parent = new Composite(container, SWT.NULL);
		parent.setLayout(new GridLayout(3, false));
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

		rustupToolchainLabel = new Label(parent, SWT.NONE);
		rustupToolchainLabel.setText("Toolchain:");
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
		otherIdLabel = new Label(otherIdComposite, SWT.NONE);
		otherIdLabel.setText("ID:");
		otherIdLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		otherIdText = new Text(otherIdComposite, SWT.BORDER);
		otherIdText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		otherIdText.addModifyListener(e -> {
			setValid(isPageValid());
		});
		new Label(parent, SWT.NONE);
	}

	private void setDefaultPathsEnabled(boolean enabled) {
		rustupLabel.setEnabled(enabled);
		rustupPathText.setEnabled(enabled);
		rustupBrowseButton.setEnabled(enabled);
		cargoLabel.setEnabled(enabled);
		cargoPathText.setEnabled(enabled);
		cargoBrowseButton.setEnabled(enabled);
	}

	private void setRustupEnabled(boolean enabled) {
		rustupToolchainLabel.setEnabled(enabled);
		rustupToolchainCombo.setEnabled(enabled);
		otherIdLabel.setEnabled(enabled);
		otherIdText.setEnabled(enabled);
	}

	private Label rlsLabel;
	private Text rlsPathText;
	private Button rlsBrowseButton;

	private Label sysrootLabel;
	private Text sysrootPathText;
	private Button sysrootBrowseButton;

	private void createOtherPart(Composite container) {
		Composite parent = new Composite(container, SWT.NULL);
		parent.setLayout(new GridLayout(3, false));
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

		GridData labelIndent = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
		labelIndent.horizontalIndent = 25;

		GridData textIndent = new GridData(SWT.FILL, SWT.CENTER, true, false);
		textIndent.horizontalIndent = 50;
		textIndent.widthHint = convertWidthInCharsToPixels(50);

		rlsLabel = new Label(parent, SWT.NONE);
		rlsLabel.setText("Path to the Rust Language Server (rls):");
		rlsLabel.setLayoutData(labelIndent);

		rlsPathText = new Text(parent, SWT.BORDER);
		rlsPathText.setLayoutData(textIndent);
		rlsPathText.addModifyListener(e -> {
			setValid(isPageValid());
		});

		rlsBrowseButton = new Button(parent, SWT.NONE);
		rlsBrowseButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		rlsBrowseButton.setText("Browse...");
		rlsBrowseButton.addSelectionListener(widgetSelectedAdapter(e -> {
			FileDialog dialog = new FileDialog(rlsBrowseButton.getShell());
			String path = dialog.open();
			if (path != null) {
				rlsPathText.setText(path);
			}
		}));

		sysrootLabel = new Label(parent, SWT.NONE);
		sysrootLabel.setText("Path to `sysroot` of the Rust installation:");
		sysrootLabel.setLayoutData(labelIndent);

		sysrootPathText = new Text(parent, SWT.BORDER);
		sysrootPathText.setLayoutData(textIndent);
		sysrootPathText.addModifyListener(e -> {
			setValid(isPageValid());
		});

		sysrootBrowseButton = new Button(parent, SWT.NONE);
		sysrootBrowseButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		sysrootBrowseButton.setText("Browse...");
		sysrootBrowseButton.addSelectionListener(widgetSelectedAdapter(e -> {
			FileDialog dialog = new FileDialog(sysrootBrowseButton.getShell());
			String path = dialog.open();
			if (path != null) {
				sysrootPathText.setText(path);
			}
		}));
	}

	private void setOtherEnabled(boolean enabled) {
		rlsLabel.setEnabled(enabled);
		rlsPathText.setEnabled(enabled);
		rlsBrowseButton.setEnabled(enabled);

		sysrootLabel.setEnabled(enabled);
		sysrootPathText.setEnabled(enabled);
		sysrootBrowseButton.setEnabled(enabled);
	}
}
