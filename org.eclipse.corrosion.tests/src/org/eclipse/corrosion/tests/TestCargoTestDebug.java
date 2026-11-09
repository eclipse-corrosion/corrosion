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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.corrosion.launch.RustLaunchDelegateTools;
import org.eclipse.corrosion.test.CargoTestDelegate;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.swt.widgets.Display;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests the debug of a Cargo test launch: GDB starts each test program of the
 * project, one after the other.
 */
class TestCargoTestDebug extends AbstractCorrosionTest {

	private static final String TWO_TEST_PROGRAMS_PROJECT_NAME = "two_test_programs"; //$NON-NLS-1$

	private final List<ILaunch> launches = new CopyOnWriteArrayList<>();
	private final ILaunchListener listener = new ILaunchListener() {
		@Override
		public void launchAdded(ILaunch launch) {
			launches.add(launch);
		}

		@Override
		public void launchRemoved(ILaunch launch) {
			// keep it in the list
		}

		@Override
		public void launchChanged(ILaunch launch) {
			// not necessary
		}
	};
	private ILaunchConfiguration configuration;

	@BeforeAll
	static void doNotAskForPerspective() {
		neverAskToSwitchPerspective();
	}

	@AfterEach
	void removeLaunchesAndConfigurations() throws CoreException {
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		launchManager.removeLaunchListener(listener);
		for (ILaunch launch : launches) {
			if (launch.canTerminate()) {
				launch.terminate();
			}
			launchManager.removeLaunch(launch);
		}
		if (configuration != null) {
			// The private debug configurations of the test programs have names that start
			// with the name of the test configuration
			for (ILaunchConfiguration config : launchManager.getLaunchConfigurations()) {
				if (config.getName().startsWith(configuration.getName())) {
					config.delete();
				}
			}
		}
	}

	@Test
	void testDebugStartsAllTestPrograms() throws Exception {
		IProject project = getProject(TWO_TEST_PROGRAMS_PROJECT_NAME);
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationWorkingCopy wc = launchManager
				.getLaunchConfigurationType(CargoTestDelegate.CARGO_TEST_LAUNCH_CONFIG_TYPE_ID)
				.newInstance(null, launchManager.generateLaunchConfigurationName(getClass().getSimpleName()));
		wc.setAttribute(RustLaunchDelegateTools.PROJECT_ATTRIBUTE, project.getName());
		configuration = wc.doSave();
		launchManager.addLaunchListener(listener);

		ILaunch testLaunch = configuration.launch(ILaunchManager.DEBUG_MODE, new NullProgressMonitor());
		waitUntil(Display.getCurrent(), Duration.ofSeconds(180), testLaunch::isTerminated);

		// The lib unit tests and tests/other.rs are two test programs. Eclipse removes
		// terminated launches when it adds a launch. If it removes the test launch at
		// the start of the first debug launch, the debug run stops after the first
		// test program.
		List<String> debuggedPrograms = launches.stream().filter(launch -> launch != testLaunch)
				.map(launch -> launch.getLaunchConfiguration().getName()).toList();
		assertEquals(2, debuggedPrograms.size(), "debug launches: " + debuggedPrograms); //$NON-NLS-1$
	}
}
