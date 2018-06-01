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
package org.eclipse.corrosion;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

public class CorrosionPreferenceInitializer extends AbstractPreferenceInitializer {

	private static final IPreferenceStore STORE = CorrosionPlugin.getDefault().getPreferenceStore();

	public static String rustSourcePreference = "corrosion.rustSource"; //$NON-NLS-1$

	public static String defaultPathsPreference = "corrosion.rustup_defaultPaths"; //$NON-NLS-1$
	public static String rustupPathPreference = "corrosion.rustup_rustupPath"; //$NON-NLS-1$
	public static String cargoPathPreference = "corrosion.rustup_cargoPath"; //$NON-NLS-1$
	public static String toolchainIdPreference = "corrosion.rustup_toolchain_Id"; //$NON-NLS-1$
	public static String toolchainTypePreference = "corrosion.rustup_toolchain_type"; //$NON-NLS-1$

	public static String rlsPathPreference = "corrosion.rslPath"; //$NON-NLS-1$
	public static String sysrootPathPreference = "corrosion.sysrootPath"; //$NON-NLS-1$

	public static String enableTemplates = "corrosion.templates"; //$NON-NLS-1$

	@Override
	public void initializeDefaultPreferences() {
		STORE.setDefault(rustSourcePreference, "rustup"); //$NON-NLS-1$

		STORE.setDefault(defaultPathsPreference, true);
		STORE.setDefault(rustupPathPreference, getRustupPathBestGuess());
		STORE.setDefault(cargoPathPreference, getCargoPathBestGuess());
		setToolchainBestGuesses();

		STORE.setDefault(rlsPathPreference, getRLSPathBestGuess());
		STORE.setDefault(sysrootPathPreference, getSysrootPathBestGuess());

		STORE.setDefault(enableTemplates, true);
	}

	private String getRustupPathBestGuess() {
		String command = findCommandPath("rustup"); //$NON-NLS-1$
		if (command.isEmpty()) {
			File possibleCommandFile = new File(System.getProperty("user.home") + "/.cargo/bin/rustup"); //$NON-NLS-1$ //$NON-NLS-2$
			if (possibleCommandFile.exists() && possibleCommandFile.isFile() && possibleCommandFile.canExecute()) {
				return possibleCommandFile.getAbsolutePath();
			}
		}
		return command;
	}

	private String getCargoPathBestGuess() {
		String command = findCommandPath("cargo"); //$NON-NLS-1$
		if (command.isEmpty()) {
			File possibleCommandFile = new File(System.getProperty("user.home") + "/.cargo/bin/cargo"); //$NON-NLS-1$ //$NON-NLS-2$
			if (possibleCommandFile.exists() && possibleCommandFile.isFile() && possibleCommandFile.canExecute()) {
				return possibleCommandFile.getAbsolutePath();
			}
		}
		return command;
	}

	private String findCommandPath(String command) {
		try {
			ProcessBuilder builder = new ProcessBuilder("which", command); //$NON-NLS-1$
			Process process = builder.start();

			if (process.waitFor() == 0) {
				try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
					return in.readLine();
				}
			}
		} catch (IOException | InterruptedException e) {
			// Errors caught with empty return
		}
		return ""; //$NON-NLS-1$
	}

	private void setToolchainBestGuesses() {
		String toolchain = RustManager.getDefaultToolchain();
		if (toolchain == null || toolchain.isEmpty()) {
			STORE.setDefault(toolchainIdPreference, ""); //$NON-NLS-1$
			STORE.setDefault(toolchainTypePreference, "Other"); //$NON-NLS-1$
			return;
		}
		int splitIndex = toolchain.indexOf('-');
		if (splitIndex != -1) {
			String type = toolchain.substring(0, splitIndex);
			if ("nightly".equals(type)) { //$NON-NLS-1$
				STORE.setDefault(toolchainIdPreference, toolchain);
				STORE.setDefault(toolchainTypePreference, "Nightly"); //$NON-NLS-1$
			} else {
				for (String option : CorrosionPreferencePage.RUSTUP_TOOLCHAIN_OPTIONS) {
					if (option.toLowerCase().equals(type)) {
						STORE.setDefault(toolchainIdPreference, type);
						STORE.setDefault(toolchainTypePreference, option);
					}
				}
			}
			return;
		}
		STORE.setDefault(toolchainIdPreference, toolchain.trim());
		STORE.setDefault(toolchainTypePreference, "Other"); //$NON-NLS-1$
	}

	private String getRLSPathBestGuess() {
		String command = findCommandPath("rls"); //$NON-NLS-1$
		if (command.isEmpty()) {
			File possibleCommandFile = new File(System.getProperty("user.home") + "/.cargo/bin/rls"); //$NON-NLS-1$ //$NON-NLS-2$
			if (possibleCommandFile.exists() && possibleCommandFile.isFile() && possibleCommandFile.canExecute()) {
				return possibleCommandFile.getAbsolutePath();
			}
		}
		return command;
	}

	private String getSysrootPathBestGuess() {
		File rustc = new File(findCommandPath("rustc")); //$NON-NLS-1$
		if (!(rustc.exists() && rustc.isFile() && rustc.canExecute())) {
			rustc = new File(System.getProperty("user.home") + "/.cargo/bin/rustc"); //$NON-NLS-1$ //$NON-NLS-2$
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
}
