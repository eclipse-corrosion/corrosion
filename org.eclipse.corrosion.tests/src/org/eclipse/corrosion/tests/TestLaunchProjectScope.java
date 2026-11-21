/*********************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.corrosion.tests;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.corrosion.launch.ProjectMappingMigrationDelegate;
import org.eclipse.corrosion.launch.RustLaunchDelegateTools;
import org.eclipse.corrosion.run.CargoRunDelegate;
import org.eclipse.corrosion.test.CargoTestDelegate;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.junit.jupiter.api.Test;

/**
 * Tests that a Cargo launch configuration tells the platform its project, and
 * that a Cargo launch builds only that project.
 */
class TestLaunchProjectScope extends AbstractCorrosionTest {

	/** Gives the tests access to the build order of the run delegate. */
	private static class RunDelegate extends CargoRunDelegate {
		IProject[] buildOrder(ILaunchConfiguration configuration) throws CoreException {
			return getBuildOrder(configuration, ILaunchManager.RUN_MODE);
		}
	}

	/** Gives the tests access to the build order of the test delegate. */
	private static class TestDelegate extends CargoTestDelegate {
		IProject[] buildOrder(ILaunchConfiguration configuration) throws CoreException {
			return getBuildOrder(configuration, ILaunchManager.RUN_MODE);
		}
	}

	private static ILaunchConfigurationWorkingCopy newConfiguration(String type) throws CoreException {
		return DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationType(type).newInstance(null,
				"projectScope"); //$NON-NLS-1$
	}

	@Test
	void testConfigurationMapsItsProject() throws Exception {
		IProject project = getProject(BASIC_PROJECT_NAME);
		ILaunchConfigurationWorkingCopy wc = newConfiguration(CargoRunDelegate.CARGO_RUN_LAUNCH_CONFIG_TYPE);
		RustLaunchDelegateTools.setProject(wc, project.getName());

		assertArrayEquals(new IResource[] { project }, wc.getMappedResources());
		assertEquals(project, RustLaunchDelegateTools.getProject(wc));
	}

	@Test
	void testConfigurationWithoutValidProjectMapsNothing() throws Exception {
		IProject project = getProject(BASIC_PROJECT_NAME);
		ILaunchConfigurationWorkingCopy wc = newConfiguration(CargoRunDelegate.CARGO_RUN_LAUNCH_CONFIG_TYPE);
		for (String name : new String[] { "", "a/b" }) { //$NON-NLS-1$ //$NON-NLS-2$
			RustLaunchDelegateTools.setProject(wc, project.getName());
			RustLaunchDelegateTools.setProject(wc, name);
			assertNull(wc.getMappedResources(), name);
			assertNull(RustLaunchDelegateTools.getProject(wc), name);
		}
	}

	@Test
	void testBuildOrderIsTheProjectOfTheLaunch() throws Exception {
		IProject project = getProject(BASIC_PROJECT_NAME);
		ILaunchConfigurationWorkingCopy run = newConfiguration(CargoRunDelegate.CARGO_RUN_LAUNCH_CONFIG_TYPE);
		ILaunchConfigurationWorkingCopy test = newConfiguration(CargoTestDelegate.CARGO_TEST_LAUNCH_CONFIG_TYPE_ID);
		RustLaunchDelegateTools.setProject(run, project.getName());
		RustLaunchDelegateTools.setProject(test, project.getName());

		assertArrayEquals(new IProject[] { project }, new RunDelegate().buildOrder(run));
		assertArrayEquals(new IProject[] { project }, new TestDelegate().buildOrder(test));
	}

	@Test
	void testBuildOrderIsEmptyForMissingOrClosedProject() throws Exception {
		ILaunchConfigurationWorkingCopy wc = newConfiguration(CargoRunDelegate.CARGO_RUN_LAUNCH_CONFIG_TYPE);
		RustLaunchDelegateTools.setProject(wc, "missing"); //$NON-NLS-1$
		assertArrayEquals(new IProject[0], new RunDelegate().buildOrder(wc));

		IProject project = getProject(BASIC_PROJECT_NAME);
		RustLaunchDelegateTools.setProject(wc, project.getName());
		project.close(null);
		try {
			assertArrayEquals(new IProject[0], new RunDelegate().buildOrder(wc));
		} finally {
			project.open(null);
		}
	}

	@Test
	void testMigrationMapsAnOldConfiguration() throws Exception {
		IProject project = getProject(BASIC_PROJECT_NAME);
		// an old configuration: only the project attribute
		ILaunchConfigurationWorkingCopy wc = newConfiguration(CargoRunDelegate.CARGO_RUN_LAUNCH_CONFIG_TYPE);
		wc.setAttribute(RustLaunchDelegateTools.PROJECT_ATTRIBUTE, project.getName());
		ILaunchConfiguration configuration = wc.doSave();
		try {
			ProjectMappingMigrationDelegate migration = new ProjectMappingMigrationDelegate();
			assertTrue(migration.isCandidate(configuration));
			migration.migrate(configuration);
			assertArrayEquals(new IResource[] { project }, configuration.getMappedResources());
			assertFalse(migration.isCandidate(configuration));
		} finally {
			configuration.delete();
		}
	}

}
