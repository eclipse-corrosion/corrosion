/*********************************************************************
 * Copyright (c) 2017, 2021 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Lucas Bullen (Red Hat Inc.) - Initial implementation
 *  Nicola Orru - Added support for external RLS startup configuration
 *  Max Bureck (Fraunhofer FOKUS) - Install rls when installing rustup
 *******************************************************************************/
package org.eclipse.corrosion;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

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
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
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

	private IPreferenceStore store;

	private Combo rustupToolchainCombo;
	private InputComponent rlsInput;
	private InputComponent sysrootInput;
	private InputComponent rlsConfigurationPathInput;

	@Override
	public void init(IWorkbench workbench) {
		store = doGetPreferenceStore();
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		container.setLayout(new GridLayout(4, false));
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));

		rustupInput = new InputComponent(container, Messages.CorrosionPreferencePage_Rustup,
				e -> setValid(isPageValid()));
		rustupInput.createComponent();
		rustupInput.createVariableSelection();
		rustupInput.createFileSelection();

		Label rustupToolchainLabel = new Label(container, SWT.NONE);
		rustupToolchainLabel.setText(Messages.CorrosionPreferencePage_toolchain);
		GridData rustupToolchainGridData = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
		rustupToolchainLabel.setLayoutData(rustupToolchainGridData);

		Composite toolchainParent = new Composite(container, SWT.NONE);
		toolchainParent.setLayout(new GridLayout(2, false));
		toolchainParent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));

		rustupToolchainCombo = new Combo(toolchainParent, SWT.DROP_DOWN);
		rustupToolchainCombo.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		rustupToolchainCombo.add("stable"); //$NON-NLS-1$
		rustupToolchainCombo.add("beta"); //$NON-NLS-1$
		rustupToolchainCombo.add("nightly"); //$NON-NLS-1$
		rustupToolchainCombo.addSelectionListener(widgetSelectedAdapter(e -> setValid(isPageValid())));
		rustupToolchainCombo.addModifyListener(e -> setValid(isPageValid()));

		cargoInput = new InputComponent(container, Messages.CorrosionPreferencePage_caro, e -> setValid(isPageValid()));
		cargoInput.createComponent();
		cargoInput.createVariableSelection();
		cargoInput.createFileSelection();

		sysrootInput = new InputComponent(container, Messages.CorrosionPreferencePage_sysrootPath,
				e -> setValid(isPageValid()));
		sysrootInput.createComponent();
		sysrootInput.createVariableSelection();
		sysrootInput.createFileSelection();

		new Composite(container, SWT.NONE).setLayoutData(new GridData(0, 0));
		installButton = new Button(container, SWT.PUSH);
		installButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 3, 1));
		installButton.addSelectionListener(widgetSelectedAdapter(e -> {
			if (Platform.getOS().equals(Platform.OS_WIN32)) {
				Program.launch("https://rustup.rs/"); //$NON-NLS-1$
			} else {
				installRustupCommands();
			}
		}));

		workingDirectoryInput = new InputComponent(container, Messages.LaunchUI_workingDirectory,
				e -> setValid(isPageValid()));
		workingDirectoryInput.createComponent();
		workingDirectoryInput.createVariableSelection();
		workingDirectoryInput.createFolderSelection();

		new Composite(container, SWT.NONE).setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 4, 1));

		rlsInput = new InputComponent(container, Messages.CorrosionPreferencePage_rlsPath,
				e -> setValid(isPageValid()));
		rlsInput.createComponent();
		rlsInput.createVariableSelection();
		rlsInput.createFileSelection();
		new Composite(container, SWT.NONE).setLayoutData(new GridData(0, 0));
		Button dlAndInstallRustAnalyzerButton = new Button(container, SWT.PUSH);
		dlAndInstallRustAnalyzerButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
		dlAndInstallRustAnalyzerButton.setText(Messages.CorrosionPreferencePage_downloadRustAnalyzer);
		dlAndInstallRustAnalyzerButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			dlAndInstallRustAnalyzerButton.setEnabled(false);
			dlAndInstallRustAnalyzerButton.setText(Messages.CorrosionPreferencePage_downloadingRustAnalyzer);
			dlAndInstallRustAnalyzerButton.getParent().layout(new Control[] { dlAndInstallRustAnalyzerButton });
			rlsInput.setEnabled(false);
			RustManager.downloadAndInstallRustAnalyzer(
					progress -> dlAndInstallRustAnalyzerButton.getDisplay().asyncExec(() -> {
						dlAndInstallRustAnalyzerButton.setText(NLS.bind(
								Messages.CorrosionPreferencePage_downloadingRustAnalyzer, (int) (100. * progress)));
						dlAndInstallRustAnalyzerButton.requestLayout();
					})).thenAccept(file -> {
						if (!dlAndInstallRustAnalyzerButton.isDisposed()) {
							dlAndInstallRustAnalyzerButton.getDisplay().asyncExec(() -> {
								rlsInput.setValue(file.getAbsolutePath());
								rlsInput.setEnabled(true);
								dlAndInstallRustAnalyzerButton
										.setText(Messages.CorrosionPreferencePage_downloadRustAnalyzer);
								setValid(isPageValid());
							});
						}
					}).exceptionally(ex -> {
						dlAndInstallRustAnalyzerButton.getDisplay().asyncExec(() -> {
							dlAndInstallRustAnalyzerButton.setText("🛑" + ex.getMessage()); //$NON-NLS-1$
							dlAndInstallRustAnalyzerButton.getParent().requestLayout();
						});
						return null;
					});
		}));

		rlsConfigurationPathInput = new InputComponent(container, Messages.CorrosionPreferencePage_rlsConfigurationPath,
				e -> setValid(isPageValid()));
		rlsConfigurationPathInput.createComponent();
		rlsConfigurationPathInput.createVariableSelection();
		rlsConfigurationPathInput.createFileSelection();

		initializeContent();
		return container;

	}

	private void initializeContent() {
		String toolchainId = store.getString(CorrosionPreferenceInitializer.TOOLCHAIN_ID_PREFERENCE);
		rustupToolchainCombo.setText(toolchainId);
		rustupInput.setValue(store.getString(CorrosionPreferenceInitializer.RUSTUP_PATHS_PREFERENCE));
		cargoInput.setValue(store.getString(CorrosionPreferenceInitializer.CARGO_PATH_PREFERENCE));
		rlsInput.setValue(store.getString(CorrosionPreferenceInitializer.RLS_PATH_PREFERENCE));
		sysrootInput.setValue(store.getString(CorrosionPreferenceInitializer.SYSROOT_PATH_PREFERENCE));
		workingDirectoryInput.setValue(store.getString(CorrosionPreferenceInitializer.WORKING_DIRECTORY_PREFERENCE));
		rlsConfigurationPathInput
				.setValue(store.getString(CorrosionPreferenceInitializer.RLS_CONFIGURATION_PATH_PREFERENCE));
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return CorrosionPlugin.getDefault().getPreferenceStore();
	}

	private boolean isPageValid() {
		setMessage(null);
		return isCommandPathsValid() && isRustupSectionValid() && isLanguageServerInstallSectionValid();
	}

	private boolean isCommandPathsValid() {

		String error = ""; //$NON-NLS-1$
		if (cargoInput.getValue().isEmpty()) {
			error = Messages.CorrosionPreferencePage_emptyRustupCargoPath;
		} else if ((rustupInput.getValue().isEmpty())) {
			setMessage(Messages.CorrosionPreferencePage_emptyRustupCargoPath, IMessageProvider.WARNING);
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
		if (rustupToolchainCombo.getText().isBlank()) {
			setMessage(Messages.CorrosionPreferencePage_emptyToolchain);
		}
		return true;
	}

	private boolean isLanguageServerInstallSectionValid() {
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
		String toolchainId = store.getDefaultString(CorrosionPreferenceInitializer.TOOLCHAIN_ID_PREFERENCE);
		rustupToolchainCombo.setText(toolchainId);
		rustupInput.setValue(store.getDefaultString(CorrosionPreferenceInitializer.RUSTUP_PATHS_PREFERENCE));
		cargoInput.setValue(store.getDefaultString(CorrosionPreferenceInitializer.CARGO_PATH_PREFERENCE));
		rlsInput.setValue(store.getDefaultString(CorrosionPreferenceInitializer.RLS_PATH_PREFERENCE));
		sysrootInput.setValue(store.getDefaultString(CorrosionPreferenceInitializer.SYSROOT_PATH_PREFERENCE));
		workingDirectoryInput
				.setValue(store.getDefaultString(CorrosionPreferenceInitializer.WORKING_DIRECTORY_PREFERENCE));
		super.performDefaults();
	}

	@Override
	public boolean performOk() {
		store.setValue(CorrosionPreferenceInitializer.WORKING_DIRECTORY_PREFERENCE, workingDirectoryInput.getValue());
		store.setValue(CorrosionPreferenceInitializer.RLS_CONFIGURATION_PATH_PREFERENCE,
				rlsConfigurationPathInput.getValue());

		store.setValue(CorrosionPreferenceInitializer.RUSTUP_PATHS_PREFERENCE, rustupInput.getValue());
		store.setValue(CorrosionPreferenceInitializer.CARGO_PATH_PREFERENCE, cargoInput.getValue());

		String id = rustupToolchainCombo.getText();
		if (!store.getString(CorrosionPreferenceInitializer.TOOLCHAIN_ID_PREFERENCE).equals(id)) {
			RustManager.setDefaultToolchain(id);
			store.setValue(CorrosionPreferenceInitializer.TOOLCHAIN_ID_PREFERENCE, id);
		}
		store.setValue(CorrosionPreferenceInitializer.RLS_PATH_PREFERENCE, rlsInput.getValue());
		store.setValue(CorrosionPreferenceInitializer.SYSROOT_PATH_PREFERENCE, sysrootInput.getValue());
		return true;
	}

	private Button installButton;
	private InputComponent rustupInput;
	private InputComponent cargoInput;
	private InputComponent workingDirectoryInput;

	private boolean installInProgress = false;

	private void installRustupCommands() {
		installButton.setText(Messages.CorrosionPreferencePage_installing);
		installButton.setEnabled(false);
		setMessage(Messages.CorrosionPreferencePage_installingRustupCargo);

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
			command = new String[] { file.getAbsolutePath(), "-y" }; //$NON-NLS-1$
		} catch (IOException | URISyntaxException e) {
			CorrosionPlugin.showError(Messages.CorrosionPreferencePage_cannotInstallRustupCargo,
					Messages.CorrosionPreferencePage_cannotInstallRustupCargo_details, e);
			return;
		}

		CommandJob installCommandJob = new CommandJob(command, Messages.CorrosionPreferencePage_installingRustupCargo,
				Messages.CorrosionPreferencePage_cannotInstallRustupCargo,
				Messages.CorrosionPreferencePage_cannotInstallRustupCargo_details, 15);
		String toolchain = rustupToolchainCombo.getText();
		installCommandJob.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(final IJobChangeEvent event) {
				if (event.getResult() == Status.OK_STATUS) {
					CorrosionPreferenceInitializer initializer = new CorrosionPreferenceInitializer();
					initializer.initializeDefaultPreferences();
					Job setDefaultToolchainJob = RustManager
							.setDefaultToolchain(toolchain.isEmpty() ? "stable" : toolchain); //$NON-NLS-1$
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

	private void setInstallRequired(boolean required) {
		if (installInProgress)
			return;
		if (required) {
			installButton.setText(Messages.CorrosionPreferencePage_install);
		} else {
			installButton.setText(Messages.CorrosionPreferencePage_installed);
		}
		installButton.setEnabled(required);
	}

}
