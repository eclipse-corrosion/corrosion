/*********************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.corrosion.launch;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationMigrationDelegate;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;

/**
 * Maps a launch configuration that was saved before Corrosion mapped launch
 * configurations to their project. Run/Debug > Launching > Launch
 * Configurations > Migrate uses it.
 */
public class ProjectMappingMigrationDelegate implements ILaunchConfigurationMigrationDelegate {

	@Override
	public boolean isCandidate(ILaunchConfiguration candidate) throws CoreException {
		IResource[] mapped = candidate.getMappedResources();
		return (mapped == null || mapped.length == 0) && RustLaunchDelegateTools.getProject(candidate) != null;
	}

	@Override
	public void migrate(ILaunchConfiguration candidate) throws CoreException {
		ILaunchConfigurationWorkingCopy wc = candidate.getWorkingCopy();
		RustLaunchDelegateTools.setProject(wc,
				candidate.getAttribute(RustLaunchDelegateTools.PROJECT_ATTRIBUTE, "")); //$NON-NLS-1$
		wc.doSave();
	}
}
