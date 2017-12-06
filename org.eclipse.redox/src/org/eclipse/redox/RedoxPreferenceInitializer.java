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

	public static String rustSourcePreference = "redox.rustSource";

	public static String defaultPathsPreference = "redox.rustup_defaultPaths";
	public static String rustupPathPreference = "redox.rustup_rustupPath";
	public static String cargoPathPreference = "redox.rustup_cargoPath";
	public static String toolchainTypePreference = "redox.rustup_toolchain_type";
	public static String toolchainIdPreference = "redox.rustup_toolchain_Id";

	public static String rlsPathPreference = "redox.rslPath";
	public static String sysrootPathPreference = "redox.sysrootPath";

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = RedoxPlugin.getDefault().getPreferenceStore();
		store.setDefault(rustSourcePreference, "rustup");

		store.setDefault(defaultPathsPreference, true);
		store.setDefault(rustupPathPreference, getRustupPathBestGuess());
		store.setDefault(cargoPathPreference, getCargoPathBestGuess());
		store.setDefault(toolchainTypePreference, "Beta");
		store.setDefault(toolchainIdPreference, "beta");

		store.setDefault(rlsPathPreference, getRLSPathBestGuess());
		store.setDefault(sysrootPathPreference, getSysrootPathBestGuess());
	}

	private String getRustupPathBestGuess() {
		File rustup = new File(System.getProperty("user.home") + "/.rustup");
		if (!(rustup.exists() && rustup.isDirectory())) {
			return "";
		}
		return rustup.getAbsolutePath();
	}

	private String getCargoPathBestGuess() {
		File cargo = new File(System.getProperty("user.home") + "/.cargo");
		if (!(cargo.exists() && cargo.isDirectory())) {
			return "";
		}
		return cargo.getAbsolutePath();
	}

	private String getRLSPathBestGuess() {
		File rls = new File(getCargoPathBestGuess() + "/bin/rls");
		if (!(rls.exists() && rls.isFile() && rls.canExecute())) {
			return "";
		}
		return rls.getAbsolutePath();

	}

	private String getSysrootPathBestGuess() {
		File rustc = new File(getCargoPathBestGuess() + "/bin/rustc");
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
