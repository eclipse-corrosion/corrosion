/*********************************************************************
 * Copyright (c) 2017 Red Hat Inc. and others.
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
package org.eclipse.corrosion.cargo.core;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;

public class CargoProjectTester extends PropertyTester {
	public static final String PROPERTY_NAME = "isCargoProject"; //$NON-NLS-1$

	@Override public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		if (property.equals(PROPERTY_NAME)) {
			IResource resource = toResource(receiver);
			if (resource == null) {
				return false;
			}
			IProject project = resource.getProject();
			return project.getFile("Cargo.toml").exists(); //$NON-NLS-1$
		}
		return false;
	}

	private IResource toResource(Object o) {
		if (o instanceof IResource) {
			return (IResource) o;
		} else if (o instanceof IAdaptable) {
			return ((IAdaptable) o).getAdapter(IResource.class);
		} else {
			return null;
		}
	}

}
