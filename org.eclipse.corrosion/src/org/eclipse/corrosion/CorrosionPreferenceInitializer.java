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
 *  Holger Voormann - Set correct default locations on Windows (https://github.com/eclipse/corrosion/issues/86)
 *******************************************************************************/
package org.eclipse.corrosion;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

public class CorrosionPreferenceInitializer extends AbstractPreferenceInitializer {

	private static final IPreferenceStore STORE = CorrosionPlugin.getDefault().getPreferenceStore();
	private static final String CARGO_DEFAULT_HOME = System.getProperty("user.home") + "/.cargo/bin/"; //$NON-NLS-1$ //$NON-NLS-2$
	private static final boolean IS_WINDOWS = Platform.getOS().startsWith("win"); //$NON-NLS-1$

	public static final String RUST_SOURCE_PREFERENCE = "corrosion.rustSource"; //$NON-NLS-1$

	public static final String DEFAULT_PATHS_PREFERENCE = "corrosion.rustup_defaultPaths"; //$NON-NLS-1$
	public static final String RUSTUP_PATHS_PREFERENCE = "corrosion.rustup_rustupPath"; //$NON-NLS-1$
	public static final String CARGO_PATH_PREFERENCE = "corrosion.rustup_cargoPath"; //$NON-NLS-1$
	public static final String TOOLCHAIN_ID_PREFERENCE = "corrosion.rustup_toolchain_Id"; //$NON-NLS-1$
	public static final String TOOLCHAIN_TYPE_PREFERENCE = "corrosion.rustup_toolchain_type"; //$NON-NLS-1$

	public static final String RLS_PATH_PREFERENCE = "corrosion.rslPath"; //$NON-NLS-1$
	public static final String SYSROOT_PATH_PREFERENCE = "corrosion.sysrootPath"; //$NON-NLS-1$

	@Override
	public void initializeDefaultPreferences() {
		STORE.setDefault(RUST_SOURCE_PREFERENCE, "rustup"); //$NON-NLS-1$

		STORE.setDefault(DEFAULT_PATHS_PREFERENCE, true);
		STORE.setDefault(RUSTUP_PATHS_PREFERENCE, getRustupPathBestGuess());
		STORE.setDefault(CARGO_PATH_PREFERENCE, getCargoPathBestGuess());
		setToolchainBestGuesses();

		STORE.setDefault(RLS_PATH_PREFERENCE, getRLSPathBestGuess());
		STORE.setDefault(SYSROOT_PATH_PREFERENCE, getSysrootPathBestGuess());
	}

	private String getRustupPathBestGuess() {
		String command = findCommandPath("rustup"); //$NON-NLS-1$
		if (command.isEmpty()) {
			File possibleCommandFile = getExectuableFileOfCargoDefaultHome("rustup"); //$NON-NLS-1$
			if (possibleCommandFile.exists() && possibleCommandFile.isFile() && possibleCommandFile.canExecute()) {
				return possibleCommandFile.getAbsolutePath();
			}
		}
		return command;
	}

	private String getCargoPathBestGuess() {
		String command = findCommandPath("cargo"); //$NON-NLS-1$
		if (command.isEmpty()) {
			File possibleCommandFile = getExectuableFileOfCargoDefaultHome("cargo"); //$NON-NLS-1$
			if (possibleCommandFile.exists() && possibleCommandFile.isFile() && possibleCommandFile.canExecute()) {
				return possibleCommandFile.getAbsolutePath();
			}
		}
		return command;
	}

	private String findCommandPath(String command) {
		try {
			ProcessBuilder builder = new ProcessBuilder(IS_WINDOWS ? "where" : "which", command); //$NON-NLS-1$ //$NON-NLS-2$
			Process process = builder.start();

			if (process.waitFor() == 0) {
				try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
					return in.readLine();
				}
			}
		} catch (IOException e) { // Errors caught with empty return
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return ""; //$NON-NLS-1$
	}

	private void setToolchainBestGuesses() {
		String toolchain = RustManager.getDefaultToolchain();
		if (toolchain == null || toolchain.isEmpty()) {
			STORE.setDefault(TOOLCHAIN_ID_PREFERENCE, ""); //$NON-NLS-1$
			STORE.setDefault(TOOLCHAIN_TYPE_PREFERENCE, "Other"); //$NON-NLS-1$
			return;
		}
		int splitIndex = toolchain.indexOf('-');
		if (splitIndex != -1) {
			String type = toolchain.substring(0, splitIndex);
			if ("nightly".equals(type)) { //$NON-NLS-1$
				STORE.setDefault(TOOLCHAIN_ID_PREFERENCE, toolchain);
				STORE.setDefault(TOOLCHAIN_TYPE_PREFERENCE, "Nightly"); //$NON-NLS-1$
			} else {
				for (String option : CorrosionPreferencePage.RUSTUP_TOOLCHAIN_OPTIONS) {
					if (option.equalsIgnoreCase(type)) {
						STORE.setDefault(TOOLCHAIN_ID_PREFERENCE, type);
						STORE.setDefault(TOOLCHAIN_TYPE_PREFERENCE, option);
					}
				}
			}
			return;
		}
		STORE.setDefault(TOOLCHAIN_ID_PREFERENCE, toolchain.trim());
		STORE.setDefault(TOOLCHAIN_TYPE_PREFERENCE, "Other"); //$NON-NLS-1$
	}

	private String getRLSPathBestGuess() {
		String command = findCommandPath("rls"); //$NON-NLS-1$
		if (command.isEmpty()) {
			File possibleCommandFile = getExectuableFileOfCargoDefaultHome("rls"); //$NON-NLS-1$
			if (possibleCommandFile.exists() && possibleCommandFile.isFile() && possibleCommandFile.canExecute()) {
				return possibleCommandFile.getAbsolutePath();
			}
		}
		return command;
	}

	private String getSysrootPathBestGuess() {
		File rustc = new File(findCommandPath("rustc")); //$NON-NLS-1$
		if (!(rustc.exists() && rustc.isFile() && rustc.canExecute())) {
			rustc = getExectuableFileOfCargoDefaultHome("rustc"); //$NON-NLS-1$
		}
		if (!(rustc.exists() && rustc.isFile() && rustc.canExecute())) {
			return ""; //$NON-NLS-1$
		}
		String[] command = new String[] { rustc.getAbsolutePath(), Messages.CorrosionPreferenceInitializer_29,
				Messages.CorrosionPreferenceInitializer_30 };
		try {
			Process process = Runtime.getRuntime().exec(command);
			try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				return in.readLine();
			}
		} catch (IOException e) {
			return Messages.CorrosionPreferenceInitializer_31;
		}
	}

	private static File getExectuableFileOfCargoDefaultHome(final String executable) {
		return new File(CARGO_DEFAULT_HOME + executable + (IS_WINDOWS ? ".exe" : "")); //$NON-NLS-1$//$NON-NLS-2$
	}

}
