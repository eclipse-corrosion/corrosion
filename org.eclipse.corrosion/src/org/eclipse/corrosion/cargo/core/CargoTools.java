/*********************************************************************
 * Copyright (c) 2018 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Lucas Bullen   (Red Hat Inc.) - Initial implementation
 *******************************************************************************/
package org.eclipse.corrosion.cargo.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.corrosion.CorrosionPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

public class CargoTools {

	public static List<CLIOption> getOptions(String subCommand) {
		String[] command = new String[] { getCargoCommand(), subCommand, "--help" }; //$NON-NLS-1$
		try {
			Process process = Runtime.getRuntime().exec(command);
			try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line = in.readLine();
				while (line != null) {
					if ("options:".equalsIgnoreCase(line)) { //$NON-NLS-1$
						break;
					}
					line = in.readLine();
				}
				if (line == null) {
					return Collections.emptyList();
				}
				List<CLIOption> options = new ArrayList<>();
				List<String> currentOptionLines = new ArrayList<>();
				while (line != null) {
					if (line.matches("\\s*")) { //$NON-NLS-1$
						break;
					} else if (line.matches("\\s*-+.*")) { //$NON-NLS-1$
						if (currentOptionLines.size() > 0) {
							options.add(new CLIOption(currentOptionLines));
							currentOptionLines.clear();
						}
						currentOptionLines.add(line);
					} else if (currentOptionLines.size() > 0) {
						currentOptionLines.add(line);
					}
					line = in.readLine();
				}
				if (currentOptionLines.size() > 0) {
					options.add(new CLIOption(currentOptionLines));
					currentOptionLines.clear();
				}
				return options;
			}
		} catch (IOException e) {
			return Collections.emptyList();
		}
	}

	public static String getCargoCommand() {
		IPreferenceStore store = CorrosionPlugin.getDefault().getPreferenceStore();
		return store.getString(CorrosionPreferenceInitializer.cargoPathPreference);
	}
}
