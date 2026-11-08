/*********************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.corrosion.test;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.corrosion.launch.RustLaunchDelegateTools;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.unittest.model.ITestElement;
import org.eclipse.unittest.model.ITestRunSession;

/**
 * Gives the Cargo project of a test in the unit test view. Then "Run" and
 * "Debug" (Ctrl+F11 and F11) also work when the unit test view is active, as
 * for JUnit.
 */
public class CargoTestElementAdapterFactory implements IAdapterFactory {

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (!(adaptableObject instanceof ITestElement element)
				|| !(adapterType == IResource.class || adapterType == IProject.class)) {
			return null;
		}
		ITestRunSession session = element.getTestRunSession();
		ILaunch launch = session != null ? session.getLaunch() : null;
		ILaunchConfiguration configuration = launch != null ? launch.getLaunchConfiguration() : null;
		if (configuration == null) {
			return null;
		}
		try {
			if (!CargoTestDelegate.CARGO_TEST_LAUNCH_CONFIG_TYPE_ID.equals(configuration.getType().getIdentifier())) {
				return null;
			}
			String projectName = configuration.getAttribute(RustLaunchDelegateTools.PROJECT_ATTRIBUTE, ""); //$NON-NLS-1$
			if (projectName.isEmpty()) {
				return null;
			}
			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
			return project.exists() ? adapterType.cast(project) : null;
		} catch (CoreException e) {
			CorrosionPlugin.logError(e);
			return null;
		}
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class<?>[] { IResource.class, IProject.class };
	}
}
