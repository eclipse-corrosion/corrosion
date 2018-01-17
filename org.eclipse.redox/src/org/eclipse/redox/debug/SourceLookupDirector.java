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
package org.eclipse.redox.debug;

import org.eclipse.cdt.debug.internal.core.sourcelookup.CSourceLookupDirector;
import org.eclipse.cdt.debug.internal.core.sourcelookup.CSourcePathComputerDelegate;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputer;
import org.eclipse.redox.RedoxPlugin;

public class SourceLookupDirector extends CSourceLookupDirector {
	@Override
	public ISourcePathComputer getSourcePathComputer() {
		ISourcePathComputer computer = super.getSourcePathComputer();
		if (computer != null) {
			return computer;
		}
		return new ISourcePathComputer() {

			CSourcePathComputerDelegate langSourcePathComputer = new CSourcePathComputerDelegate();

			@Override
			public ISourceContainer[] computeSourceContainers(ILaunchConfiguration configuration,
					IProgressMonitor monitor) throws CoreException {
				return langSourcePathComputer.computeSourceContainers(configuration, monitor);
			}

			@Override
			public String getId() {
				return RedoxPlugin.PLUGIN_ID + ".SourceLocator";
			}
		};
	}
}
