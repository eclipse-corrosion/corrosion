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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceContainerType;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputerDelegate;

/**
 * Computes the default source lookup path for a launch configuration.
 */
public class CargoSourcePathComputerDelegate implements ISourcePathComputerDelegate {

	/**
	 * Constructor for CargoSourcePathComputerDelegate.
	 */
	public CargoSourcePathComputerDelegate() {
		super();
	}

	@Override
	public ISourceContainer[] computeSourceContainers(ILaunchConfiguration configuration, IProgressMonitor monitor)
			throws CoreException {
		ISourceContainer[] common = CorrosionPlugin.getDefault().getCommonSourceLookupDirector().getSourceContainers();
		ISourceContainer[] containers = new ISourceContainer[common.length];

		for (int i = 0; i < common.length; i++) {
			ISourceContainer container = common[i];
			ISourceContainerType type = container.getType();
			// Clone the container to make sure that the original can be safely disposed.
			container = type.createSourceContainer(type.getMemento(container));
			containers[i] = container;
		}
		return containers;
	}
}
