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

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.corrosion.debug.RustDebugDelegate;
import org.eclipse.corrosion.launch.RustLaunchDelegateTools;
import org.eclipse.corrosion.run.CargoRunDelegate;
import org.eclipse.corrosion.test.CargoTestDelegate;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Tests how the Rust launch shortcuts give their launch configurations to
 * Eclipse, so that "Run" (Ctrl+F11) and "Debug" (F11) launch the last used
 * configuration.
 */
class TestLaunchShortcuts extends AbstractCorrosionTest {

	private final List<ILaunchConfiguration> configurations = new ArrayList<>();

	@AfterEach
	void deleteConfigurations() throws CoreException {
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		for (ILaunch launch : launchManager.getLaunches()) {
			if (configurations.contains(launch.getLaunchConfiguration())) {
				launchManager.removeLaunch(launch);
			}
		}
		for (ILaunchConfiguration configuration : configurations) {
			configuration.delete();
		}
	}

	@Test
	void testShortcutGivesConfigurationsOfSelectedProjectOnly() throws IOException, CoreException {
		IProject project = getProject(BASIC_PROJECT_NAME);
		ILaunchConfiguration config = save(CargoRunDelegate.CARGO_RUN_LAUNCH_CONFIG_TYPE, project.getName());
		save(CargoRunDelegate.CARGO_RUN_LAUNCH_CONFIG_TYPE, "otherProject"); //$NON-NLS-1$

		assertArrayEquals(new ILaunchConfiguration[] { config },
				new CargoRunDelegate().getLaunchConfigurations(new StructuredSelection(project)));
		// A file of the project gives the configurations of its project
		assertArrayEquals(new ILaunchConfiguration[] { config }, new CargoRunDelegate()
				.getLaunchConfigurations(new StructuredSelection(project.getFile("src/main.rs")))); //$NON-NLS-1$
		assertEquals(0, new CargoRunDelegate().getLaunchConfigurations(StructuredSelection.EMPTY).length);
	}

	@Test
	void testEachShortcutGivesItsOwnType() throws IOException, CoreException {
		IProject project = getProject(BASIC_PROJECT_NAME);
		ILaunchConfiguration run = save(CargoRunDelegate.CARGO_RUN_LAUNCH_CONFIG_TYPE, project.getName());
		ILaunchConfiguration test = save(CargoTestDelegate.CARGO_TEST_LAUNCH_CONFIG_TYPE_ID, project.getName());
		ILaunchConfiguration debug = save(RustLaunchDelegateTools.CORROSION_DEBUG_LAUNCH_CONFIG_TYPE,
				project.getName());
		StructuredSelection selection = new StructuredSelection(project);

		assertArrayEquals(new ILaunchConfiguration[] { run },
				new CargoRunDelegate().getLaunchConfigurations(selection));
		assertArrayEquals(new ILaunchConfiguration[] { test },
				new CargoTestDelegate().getLaunchConfigurations(selection));
		assertArrayEquals(new ILaunchConfiguration[] { debug },
				new RustDebugDelegate().getLaunchConfigurations(selection));
	}

	@Test
	void testLaunchSavesTheConfiguration() throws IOException, CoreException {
		IProject project = getProject(BASIC_PROJECT_NAME);
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationWorkingCopy wc = launchManager
				.getLaunchConfigurationType(CargoRunDelegate.CARGO_RUN_LAUNCH_CONFIG_TYPE)
				.newInstance(null, launchManager.generateLaunchConfigurationName(getClass().getSimpleName()));
		wc.setAttribute(RustLaunchDelegateTools.PROJECT_ATTRIBUTE, project.getName());

		RustLaunchDelegateTools.launch(wc, ILaunchManager.RUN_MODE);

		// Eclipse adds only saved configurations to the launch history
		ILaunchConfiguration[] saved = RustLaunchDelegateTools.findLaunchConfigurations(project,
				CargoRunDelegate.CARGO_RUN_LAUNCH_CONFIG_TYPE);
		configurations.addAll(Arrays.asList(saved));
		assertEquals(1, saved.length);
		assertEquals(wc.getName(), saved[0].getName());
		assertFalse(saved[0].isWorkingCopy());
		waitForLaunchEnd(saved[0]);
	}

	@Test
	void testTestShortcutSavesNewConfiguration() throws IOException, CoreException {
		IProject project = getProject(BASIC_PROJECT_NAME);

		// The test shortcut launches a copy of a new working copy
		new CargoTestDelegate().launch(new StructuredSelection(project), ILaunchManager.RUN_MODE);

		ILaunchConfiguration[] saved = RustLaunchDelegateTools.findLaunchConfigurations(project,
				CargoTestDelegate.CARGO_TEST_LAUNCH_CONFIG_TYPE_ID);
		configurations.addAll(Arrays.asList(saved));
		assertEquals(1, saved.length);
		assertFalse(saved[0].isWorkingCopy());
		waitForLaunchEnd(saved[0]);
	}

	/** Lets "cargo" end before the project is deleted */
	private static void waitForLaunchEnd(ILaunchConfiguration config) {
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		waitUntil(Display.getDefault(), Duration.ofSeconds(60), () -> Arrays.stream(launchManager.getLaunches())
				.filter(launch -> config.equals(launch.getLaunchConfiguration())).allMatch(ILaunch::isTerminated));
	}

	private ILaunchConfiguration save(String type, String projectName) throws CoreException {
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationWorkingCopy wc = launchManager.getLaunchConfigurationType(type).newInstance(null,
				launchManager.generateLaunchConfigurationName(getClass().getSimpleName()));
		wc.setAttribute(RustLaunchDelegateTools.PROJECT_ATTRIBUTE, projectName);
		ILaunchConfiguration config = wc.doSave();
		configurations.add(config);
		return config;
	}
}
