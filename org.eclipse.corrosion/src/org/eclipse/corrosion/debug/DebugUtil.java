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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Platform;

/**
 * This class contains static helper methods and fields to be used for the
 * Debuging functionality in Eclipse Corrosion
 */
class DebugUtil {

	private static final boolean IS_WINDOWS = Platform.getOS().equals(Platform.OS_WIN32);

	/**
	 * Returns the default workspace path to the executable produced for the given
	 * project. This location is a guess based on the default rust/cargo project
	 * layout and the operating system running eclipse.
	 *
	 * @param project Rust project for which the executable workspace path is
	 *                computed.
	 * @return default workspace path to the executable created for the given Rust
	 *         {@code project}.
	 */
	static String getDefaultExecutablePath(IProject project) {
		if (project == null) {
			return ""; //$NON-NLS-1$
		}
		return project.getName() + "/target/debug/" + project.getName() + (IS_WINDOWS ? ".exe" : ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

}
