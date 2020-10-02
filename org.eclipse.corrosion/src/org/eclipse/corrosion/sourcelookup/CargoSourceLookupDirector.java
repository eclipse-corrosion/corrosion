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

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceContainerType;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;

public class CargoSourceLookupDirector extends AbstractSourceLookupDirector {
	private static Set<String> fSupportedTypes;
	private static Object fSupportedTypesLock = new Object();

	@Override
	public void initializeParticipants() {
		addParticipants(new ISourceLookupParticipant[] { new CargoSourceLookupParticipant() });
	}

	@Override
	public boolean supportsSourceContainerType(ISourceContainerType type) {
		readSupportedContainerTypes();
		return fSupportedTypes.contains(type.getId());
	}

	/**
	 * Loads and cache the source container types which are supported for CDT
	 * debugging.
	 */
	private static void readSupportedContainerTypes() {
		synchronized (fSupportedTypesLock) {
			if (fSupportedTypes == null) {
				fSupportedTypes = new HashSet<>();
				String name = CorrosionPlugin.PLUGIN_ID + ".supportedSourceContainerTypes"; //$NON-NLS-1$ ;
				IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(name);
				if (extensionPoint != null) {
					for (IExtension extension : extensionPoint.getExtensions()) {
						for (IConfigurationElement configurationElements : extension.getConfigurationElements()) {
							String id = configurationElements.getAttribute("id"); //$NON-NLS-1$ ;
							if (id != null)
								fSupportedTypes.add(id);
						}
					}
				}
			}
		}
	}
}