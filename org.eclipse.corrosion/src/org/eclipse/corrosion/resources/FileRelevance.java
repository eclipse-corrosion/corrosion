/*********************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.corrosion.resources;

import java.net.URI;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.IPath;

/**
 * This class computes a relevance for files in case we have to select from
 * multiple files for the same file-system location.
 */
public class FileRelevance {
	private static final int PREFERRED_PROJECT = 0x40;

	// Penalty for undesirable attributes
	private static final int LINK_PENALTY = 1;
	private static final int INACCESSIBLE_SHIFT = 4;

	/**
	 * Compute a relevance for the given file. The higher the score the more
	 * relevant the file. It is determined by the following criteria: <br>
	 * - file belongs to preferred project <br>
	 * - file is accessible - file is not a link
	 *
	 * @param f the file to compute the relevance for
	 * @return integer representing file relevance. Larger numbers are more relevant
	 */
	public static int getRelevance(IFile f, IProject preferredProject) {
		return getRelevance(f, preferredProject, true, null);
	}

	/**
	 * Compute a relevance for the given file. The higher the score the more
	 * relevant the file. It is determined by the following criteria: <br>
	 * - file belongs to preferred project <br>
	 * - file is accessible - file is not a link - file matches the original
	 * location
	 *
	 * @param f the file to compute the relevance for
	 * @return integer representing file relevance. Larger numbers are more relevant
	 */
	public static int getRelevance(IFile f, IProject preferredProject, boolean degradeSymLinks,
			Object originalLocation) {
		int result = 0;
		IProject p = f.getProject();
		if (p.equals(preferredProject))
			result += PREFERRED_PROJECT;

		if (!f.isAccessible()) {
			result >>= INACCESSIBLE_SHIFT;
		} else if (f.isLinked()) {
			result -= LINK_PENALTY;
		} else if (degradeSymLinks) {
			ResourceAttributes ra = f.getResourceAttributes();
			if (ra != null && ra.isSymbolicLink())
				result -= LINK_PENALTY;
		} else {
			// Symbolic links are not degraded, prefer the original location
			if (originalLocation instanceof URI) {
				if (originalLocation.equals(f.getLocationURI()))
					result += LINK_PENALTY;
			} else if (originalLocation instanceof IPath) {
				if (originalLocation.equals(f.getLocation()))
					result += LINK_PENALTY;
			}
		}
		return result;
	}
}
