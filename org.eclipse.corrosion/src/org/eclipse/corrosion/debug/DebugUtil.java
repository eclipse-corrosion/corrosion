/*********************************************************************
 * Copyright (c) 2018 Fraunhofer FOKUS and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Max Bureck (Fraunhofer FOKUS) - Initial implementation
 *******************************************************************************/
package org.eclipse.corrosion.debug;

import java.io.InputStream;
import java.util.Properties;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Platform;
import org.eclipse.corrosion.CorrosionPlugin;

/**
 * This class contains static helper methods and fields to be used for the Debuging functionality in Eclipse Corrosion
 */
class DebugUtil {

	private static final boolean IS_WINDOWS = Platform.getOS().equals(Platform.OS_WIN32);

	/**
	 * Returns the default workspace path to the executable produced for the given project. This location is a guess based on the default rust/cargo project layout and the operating system running
	 * eclipse.
	 *
	 * @param project
	 *            Rust project for which the executable workspace path is computed.
	 * @return default workspace path to the executable created for the given Rust {@code project}.
	 */
	static String getDefaultExecutablePath(IProject project) {
		if (project == null) {
			return ""; //$NON-NLS-1$
		}
		IFile cargoFile = project.getFile("Cargo.toml"); //$NON-NLS-1$
		if (!cargoFile.exists()) {
			return ""; //$NON-NLS-1$
		}
		StringBuilder builder = new StringBuilder(project.getName());
		builder.append("/target/debug/"); //$NON-NLS-1$
		try (InputStream file = cargoFile.getContents()) {
			Properties properties = new Properties();
			properties.load(file);
			String name = properties.getProperty("name"); //$NON-NLS-1$
			if (!name.isEmpty()) {
				name = name.substring(name.indexOf('"') + 1, name.lastIndexOf('"'));
			}
			builder.append(name);
		} catch (Exception e) {
			CorrosionPlugin.logError(e);
		}
		if (IS_WINDOWS) {
			builder.append(".exe"); //$NON-NLS-1$
		}
		return builder.toString();
	}

}
