/*********************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.corrosion.sourcelookup;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.debug.core.sourcelookup.ISourceContainerType;
import org.eclipse.debug.core.sourcelookup.containers.AbstractSourceContainer;

public class CargoAbsolutePathSourceContainer extends AbstractSourceContainer {
	/**
	 * Unique identifier for the absolute source container type (value
	 * <code>org.eclipse.corrosion.containerType.absolutePath</code>).
	 */
	public static final String TYPE_ID = CorrosionPlugin.PLUGIN_ID + ".containerType.absolutePath"; //$NON-NLS-1$

	public boolean isValidAbsoluteFilePath(String name) {
		return isValidAbsoluteFilePath(new File(name));
	}

	public boolean isValidAbsoluteFilePath(File file) {
		return file.isAbsolute() && file.exists() && file.isFile();
	}

	@Override
	public Object[] findSourceElements(String name) throws CoreException {
		if (name != null) {
			File file = new File(name);
			if (isValidAbsoluteFilePath(file)) {
				return CargoSourceUtils.findSourceElements(file, getDirector());
			}
		}
		return new Object[0];
	}

	@Override
	public String getName() {
		return CargoSourceLookupMessages.AbsolutePathSourceContainer_0;
	}

	@Override
	public ISourceContainerType getType() {
		return getSourceContainerType(TYPE_ID);
	}

	@Override
	public int hashCode() {
		return TYPE_ID.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof CargoAbsolutePathSourceContainer))
			return false;
		return true;
	}
}
