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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.AbstractSourceContainerTypeDelegate;

public class CargoAbsolutePathSourceContainerType extends AbstractSourceContainerTypeDelegate {

	@Override
	public ISourceContainer createSourceContainer(String memento) throws CoreException {
		return new CargoAbsolutePathSourceContainer();
	}

	@Override
	public String getMemento(ISourceContainer container) throws CoreException {
		return "CargoAbsolutePath"; //$NON-NLS-1$
	}

}
