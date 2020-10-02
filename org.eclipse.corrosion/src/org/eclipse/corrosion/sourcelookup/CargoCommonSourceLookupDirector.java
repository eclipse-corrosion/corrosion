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
import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;

/**
 * Director of the common source containers.
 */
public class CargoCommonSourceLookupDirector extends CargoSourceLookupDirector {
	@SuppressWarnings("deprecation")
	@Override
	public void setSourceContainers(ISourceContainer[] containers) {
		try {
			super.setSourceContainers(containers);
			CorrosionPlugin.getDefault().getPluginPreferences().setValue(CorrosionPlugin.PREF_DEFAULT_SOURCE_CONTAINERS,
					getMemento());
			CorrosionPlugin.getDefault().getPluginPreferences().setValue(CorrosionPlugin.PREF_COMMON_SOURCE_CONTAINERS,
					""); //$NON-NLS-1$
			CorrosionPlugin.getDefault().savePluginPreferences();
		} catch (CoreException e) {
			CorrosionPlugin.logError(e.getStatus());
		}
	}
}
