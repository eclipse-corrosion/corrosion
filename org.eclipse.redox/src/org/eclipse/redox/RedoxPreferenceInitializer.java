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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

public class RedoxPreferenceInitializer extends AbstractPreferenceInitializer {

	private static final IPreferenceStore STORE = RedoxPlugin.getDefault().getPreferenceStore();

	public static String rustSourcePreference = "redox.rustSource";

	public static String defaultPathsPreference = "redox.rustup_defaultPaths";
	public static String rustupPathPreference = "redox.rustup_rustupPath";
	public static String cargoPathPreference = "redox.rustup_cargoPath";
	public static String toolchainIdPreference = "redox.rustup_toolchain_Id";
	public static String toolchainTypePreference = "redox.rustup_toolchain_type";

	public static String rlsPathPreference = "redox.rslPath";
	public static String sysrootPathPreference = "redox.sysrootPath";

	@Override
	public void initializeDefaultPreferences() {
		STORE.setDefault(rustSourcePreference, "rustup");

		STORE.setDefault(defaultPathsPreference, true);
		STORE.setDefault(rustupPathPreference, getRustupPathBestGuess());
		STORE.setDefault(cargoPathPreference, getCargoPathBestGuess());
		setToolchainBestGuesses();

		STORE.setDefault(rlsPathPreference, getRLSPathBestGuess());
		STORE.setDefault(sysrootPathPreference, getSysrootPathBestGuess());
	}

	private String getRustupPathBestGuess() {
		String command = findCommandPath("rustup");
		if (command.isEmpty()) {
			File possibleCommandFile = new File(System.getProperty("user.home") + "/.cargo/bin/rustup");
			if (possibleCommandFile.exists() && possibleCommandFile.isFile() && possibleCommandFile.canExecute()) {
				return possibleCommandFile.getAbsolutePath();
			}
		}
		return command;
	}

	private String getCargoPathBestGuess() {
		String command = findCommandPath("cargo");
		if (command.isEmpty()) {
			File possibleCommandFile = new File(System.getProperty("user.home") + "/.cargo/bin/cargo");
			if (possibleCommandFile.exists() && possibleCommandFile.isFile() && possibleCommandFile.canExecute()) {
				return possibleCommandFile.getAbsolutePath();
			}
		}
		return command;
	}

	private String findCommandPath(String command) {
		try {
			ProcessBuilder builder = new ProcessBuilder("which", command);
			Process process = builder.start();

			if (process.waitFor() == 0) {
				try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
					return in.readLine();
				}
			}
		} catch (IOException | InterruptedException e) {
			// Errors caught with empty return
		}
		return "";
	}

	private void setToolchainBestGuesses() {
		String toolchain = RustManager.getDefaultToolchain();
		if (toolchain == null || toolchain.isEmpty()) {
			STORE.setDefault(toolchainIdPreference, "");
			STORE.setDefault(toolchainTypePreference, "Other");
			return;
		}
		int splitIndex = toolchain.indexOf('-');
		if (splitIndex != -1) {
			String type = toolchain.substring(0, splitIndex);
			if ("nightly".equals(type)) {
				STORE.setDefault(toolchainIdPreference, toolchain);
				STORE.setDefault(toolchainTypePreference, "Nightly");
			} else {
				for (String option : RedoxPreferencePage.RUSTUP_TOOLCHAIN_OPTIONS) {
					if (option.toLowerCase().equals(type)) {
						STORE.setDefault(toolchainIdPreference, type);
						STORE.setDefault(toolchainTypePreference, option);
					}
				}
			}
			return;
		}
		STORE.setDefault(toolchainIdPreference, toolchain.trim());
		STORE.setDefault(toolchainTypePreference, "Other");
	}

	private String getRLSPathBestGuess() {
		String command = findCommandPath("rls");
		if (command.isEmpty()) {
			File possibleCommandFile = new File(System.getProperty("user.home") + "/.cargo/bin/rls");
			if (possibleCommandFile.exists() && possibleCommandFile.isFile() && possibleCommandFile.canExecute()) {
				return possibleCommandFile.getAbsolutePath();
			}
		}
		return command;
	}

	private String getSysrootPathBestGuess() {
		File rustc = new File(findCommandPath("rustc"));
		if (!(rustc.exists() && rustc.isFile() && rustc.canExecute())) {
			rustc = new File(System.getProperty("user.home") + "/.cargo/bin/rustc");
		}
		if (!(rustc.exists() && rustc.isFile() && rustc.canExecute())) {
			return "";
		}
		String[] command = new String[] { rustc.getAbsolutePath(), "--print", "sysroot" };
		try {
			Process process = Runtime.getRuntime().exec(command);
			try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				return in.readLine();
			}
		} catch (IOException e) {
			return "";
		}
	}
}
