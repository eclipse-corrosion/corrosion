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

import java.io.File;
import java.util.Map;

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
	 * Appends the cargo bin and GDB locations to the path variable of the wrapped
	 * GDBLaunch's environment variables ensuring that they are found when rust-gdb
	 * is used. If the launch environment does not contain a path variable, one is
	 * created.
	 *
	 * @return environment variables with required paths appended to the path
	 *         variable
	 */
	@Override
	public String[] getLaunchEnvironment() throws CoreException {
		String[] envVariables = super.getLaunchEnvironment();
		final int length = envVariables.length;

		if (length > 0) {
			for (int i = 0; i < length; i++) {
				if (envVariables[i].startsWith("PATH=")) { //$NON-NLS-1$
					envVariables[i] += File.pathSeparator + getCargoBinLocation() + File.pathSeparator
							+ getGDBLocation();
					return envVariables;
				}
			}
		} else {
			Map<String, String> env = System.getenv();
			envVariables = new String[env.size()];

			int i = 0;
			for (Map.Entry<String, String> entry : env.entrySet()) {
				envVariables[i] = entry.getKey() + "=" + entry.getValue(); //$NON-NLS-1$
				if (envVariables[i].startsWith("PATH=")) { //$NON-NLS-1$
					envVariables[i] += File.pathSeparator + getCargoBinLocation() + File.pathSeparator
							+ getGDBLocation();
				}
				i++;
			}
		}

		return envVariables;
	}

	private static String getCargoBinLocation() {
		IPath location = Path.fromOSString(CorrosionPlugin.getDefault().getPreferenceStore()
				.getString(CorrosionPreferenceInitializer.CARGO_PATH_PREFERENCE));
		String parentDirectory = location.toFile().getParent();
		return parentDirectory != null ? parentDirectory : ""; //$NON-NLS-1$
	}

	private static String getGDBLocation() {
		IPath location = Path.fromOSString(CorrosionPlugin
				.getOutputFromCommand(Platform.getOS().equals(Platform.OS_WIN32) ? "where gdb" : "which gdb"));//$NON-NLS-1$ //$NON-NLS-2$
		String parentDirectory = location.toFile().getParent();
		return parentDirectory != null ? parentDirectory : ""; //$NON-NLS-1$
	}

}
