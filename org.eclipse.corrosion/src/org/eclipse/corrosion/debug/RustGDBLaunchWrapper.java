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
 *  Lucas Bullen (Red Hat Inc.) - Initial implementation
 *******************************************************************************/
package org.eclipse.corrosion.debug;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

import org.eclipse.cdt.dsf.gdb.launching.GdbLaunch;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.corrosion.CorrosionPreferenceInitializer;
import org.eclipse.debug.core.ILaunch;

public class RustGDBLaunchWrapper extends GdbLaunch {
	public RustGDBLaunchWrapper(ILaunch launch) {
		super(launch.getLaunchConfiguration(), launch.getLaunchMode(), launch.getSourceLocator());
	}

	/**
	 * Appends the cargo bin and GDB locations to the path variable of the wrapped GDBLaunch's environment variables ensuring that they are found when rust-gdb is used. If the launch environment does not
	 * contain a path variable, one is created.
	 *
	 * @return environment variables with required paths appended to the path variable
	 */
	@Override public String[] getLaunchEnvironment() throws CoreException {
		String[] envVariables = super.getLaunchEnvironment();
		final int length = envVariables.length;

		if (length > 0) {
			for (int i = 0; i < length; i++) {
				if (envVariables[i].startsWith("PATH=")) { //$NON-NLS-1$
					envVariables[i] += ':' + getCargoBinLocation() + ':' + getGDBLocation();
					return envVariables;
				}
			}
		}

		envVariables = Arrays.copyOf(envVariables, length + 1);
		envVariables[length] = "PATH=:" + getCargoBinLocation() + ':' + getGDBLocation(); //$NON-NLS-1$
		return envVariables;
	}

	private String getCargoBinLocation() {
		IPath location = Path.fromOSString(CorrosionPlugin.getDefault().getPreferenceStore().getString(CorrosionPreferenceInitializer.cargoPathPreference));
		String parentDirectory = location.toFile().getParent();
		return parentDirectory != null ? parentDirectory : ""; //$NON-NLS-1$
	}

	private String getGDBLocation() {
		String[] command = new String[] { "/bin/bash", "-c", "which gdb" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (Platform.getOS().equals(Platform.OS_WIN32)) {
			command = new String[] { "cmd", "/c", "where gdb" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		try {
			Process process = Runtime.getRuntime().exec(command);
			try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				IPath location = Path.fromOSString(in.readLine());
				String parentDirectory = location.toFile().getParent();
				return parentDirectory != null ? parentDirectory : ""; //$NON-NLS-1$
			}
		} catch (IOException e) {
			return ""; //$NON-NLS-1$
		}
	}

}
