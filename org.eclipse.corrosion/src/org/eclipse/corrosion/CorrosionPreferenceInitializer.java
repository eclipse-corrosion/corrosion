/*********************************************************************
 * Copyright (c) 2017, 2020 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Lucas Bullen (Red Hat Inc.) - Initial implementation
 *  Holger Voormann - Set correct default locations on Windows (https://github.com/eclipse/corrosion/issues/86)
 *  Nicola Orru - Added support for external RLS startup configuration
 *******************************************************************************/
package org.eclipse.corrosion;

import java.io.File;
import java.nio.file.Paths;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

public class CorrosionPreferenceInitializer extends AbstractPreferenceInitializer {

	private static final IPreferenceStore STORE = CorrosionPlugin.getDefault().getPreferenceStore();
	private static final String CARGO_DEFAULT_HOME = System.getProperty("user.home") + "/.cargo/bin/"; //$NON-NLS-1$ //$NON-NLS-2$
	private static final String DEFAULT_DEBUGGER = "rust-gdb"; //$NON-NLS-1$
	private static final boolean IS_WINDOWS = Platform.getOS().equals(Platform.OS_WIN32);

	public static final String RUSTUP_PATHS_PREFERENCE = "corrosion.rustup_rustupPath"; //$NON-NLS-1$
	public static final String CARGO_PATH_PREFERENCE = "corrosion.rustup_cargoPath"; //$NON-NLS-1$
	public static final String TOOLCHAIN_ID_PREFERENCE = "corrosion.rustup_toolchain_Id"; //$NON-NLS-1$

	public static final String RLS_PATH_PREFERENCE = "corrosion.rslPath"; //$NON-NLS-1$
	public static final String RLS_CONFIGURATION_PATH_PREFERENCE = "corrosion.rls_configurationPath"; //$NON-NLS-1$
	public static final String SYSROOT_PATH_PREFERENCE = "corrosion.sysrootPath"; //$NON-NLS-1$

	public static final String WORKING_DIRECTORY_PREFERENCE = "corrosion.workingDirectory"; //$NON-NLS-1$

	/**
	 * Preferences key for default debugger executable to use for Rust
	 */
	public static final String DEFAULT_GDB_PREFERENCE = "corrosion.defaultGdb"; //$NON-NLS-1$

	// Editor format on save preferences
	public static final String EDIT_FORMAT_ON_SAVE_PREFERENCE = "corrosion.edit.formatOnSave"; //$NON-NLS-1$
	public static final String EDIT_FORMAT_EDITED_ON_SAVE_PREFERENCE = "corrosion.edit.formatEditedOnSave"; //$NON-NLS-1$
	public static final String EDIT_FORMAT_ALL_ON_SAVE_PREFERENCE = "corrosion.edit.formatAllOnSave"; //$NON-NLS-1$

	@Override
	public void initializeDefaultPreferences() {
		STORE.setDefault(RUSTUP_PATHS_PREFERENCE, getRustupPathBestGuess());
		STORE.setDefault(CARGO_PATH_PREFERENCE, getCargoPathBestGuess());
		setToolchainBestGuesses();
		STORE.setDefault(SYSROOT_PATH_PREFERENCE, getSysrootPathBestGuess(STORE));

		STORE.setDefault(RLS_PATH_PREFERENCE, getLanguageServerPathBestGuess().getAbsolutePath());

		STORE.setDefault(WORKING_DIRECTORY_PREFERENCE, getWorkingDirectoryBestGuess());
		STORE.setDefault(DEFAULT_GDB_PREFERENCE, DEFAULT_DEBUGGER);

		STORE.setDefault(EDIT_FORMAT_ON_SAVE_PREFERENCE, false);
		STORE.setDefault(EDIT_FORMAT_EDITED_ON_SAVE_PREFERENCE, false);
		STORE.setDefault(EDIT_FORMAT_ALL_ON_SAVE_PREFERENCE, true);
	}

	private static String getRustupPathBestGuess() {
		String command = findCommandPath("rustup"); //$NON-NLS-1$
		if (command.isEmpty()) {
			File possibleCommandFile = getExectuableFileOfCargoDefaultHome("rustup"); //$NON-NLS-1$
			if (possibleCommandFile.exists() && possibleCommandFile.isFile() && possibleCommandFile.canExecute()) {
				return possibleCommandFile.getAbsolutePath();
			}
		}
		return command;
	}

	private static String getCargoPathBestGuess() {
		String command = findCommandPath("cargo"); //$NON-NLS-1$
		if (command.isEmpty()) {
			File possibleCommandFile = getExectuableFileOfCargoDefaultHome("cargo"); //$NON-NLS-1$
			if (possibleCommandFile.exists() && possibleCommandFile.isFile() && possibleCommandFile.canExecute()) {
				return possibleCommandFile.getAbsolutePath();
			}
		}
		return command;
	}

	private static String findCommandPath(String command) {
		return CorrosionPlugin.getOutputFromCommand(IS_WINDOWS ? "where" : "which", command); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static void setToolchainBestGuesses() {
		String toolchain = RustManager.getDefaultToolchain();
		if (toolchain == null || toolchain.isEmpty()) {
			STORE.setDefault(TOOLCHAIN_ID_PREFERENCE, ""); //$NON-NLS-1$
			return;
		}
		int splitIndex = toolchain.indexOf('-');
		if (splitIndex != -1) {
			toolchain = toolchain.substring(0, splitIndex);
		}
		STORE.setDefault(TOOLCHAIN_ID_PREFERENCE, toolchain.trim());
	}

	private static File getLanguageServerPathBestGuess() {
		// first PATH
		String command = findCommandPath("rust-analyzer"); //$NON-NLS-1$
		if (command.isEmpty()) {
			// then Cargo installation
			File possibleCommandFile = getExectuableFileOfCargoDefaultHome("rust-analyzer"); //$NON-NLS-1$
			if (possibleCommandFile.exists() && possibleCommandFile.isFile() && possibleCommandFile.canExecute()) {
				return possibleCommandFile;
			}
			// then manual installation
			if (RustManager.RUST_ANALYZER_DEFAULT_LOCATION.exists()
					&& RustManager.RUST_ANALYZER_DEFAULT_LOCATION.isFile()
					&& RustManager.RUST_ANALYZER_DEFAULT_LOCATION.canExecute()) {
				return RustManager.RUST_ANALYZER_DEFAULT_LOCATION;
			}
		}
		return new File(command);
	}

	private static String getSysrootPathBestGuess(IPreferenceStore preferenceStore) {
		String rustup = preferenceStore.getString(CorrosionPreferenceInitializer.RUSTUP_PATHS_PREFERENCE);
		String toolchain = preferenceStore.getString(CorrosionPreferenceInitializer.TOOLCHAIN_ID_PREFERENCE);
		if (!(rustup.isEmpty() || toolchain.isEmpty())) {
			String[] command = new String[] { rustup, "run", toolchain, "rustc", "--print", "sysroot" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			return CorrosionPlugin.getOutputFromCommand(command);
		}
		File rustc = new File(findCommandPath("rustc")); //$NON-NLS-1$
		if (!(rustc.exists() && rustc.isFile() && rustc.canExecute())) {
			rustc = getExectuableFileOfCargoDefaultHome("rustc"); //$NON-NLS-1$
		}
		if (!(rustc.exists() && rustc.isFile() && rustc.canExecute())) {
			return ""; //$NON-NLS-1$
		}
		String[] command = new String[] { rustc.getAbsolutePath(), "--print", "sysroot" }; //$NON-NLS-1$ //$NON-NLS-2$
		return CorrosionPlugin.getOutputFromCommand(command);
	}

	private static File getExectuableFileOfCargoDefaultHome(final String executable) {
		return new File(CARGO_DEFAULT_HOME + executable + (IS_WINDOWS ? ".exe" : "")); //$NON-NLS-1$//$NON-NLS-2$
	}

	private static String getWorkingDirectoryBestGuess() {
		return Paths.get("").toAbsolutePath().toString(); //$NON-NLS-1$
	}
}
