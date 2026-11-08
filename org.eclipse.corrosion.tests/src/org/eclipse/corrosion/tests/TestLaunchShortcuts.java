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

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.corrosion.debug.RustDebugDelegate;
import org.eclipse.corrosion.launch.RustLaunchDelegateTools;
import org.eclipse.corrosion.launch.TestAtCursor;
import org.eclipse.corrosion.run.CargoRunDelegate;
import org.eclipse.corrosion.test.CargoTestDelegate;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.unittest.model.ITestElement;
import org.eclipse.unittest.model.ITestRunSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Tests how the Rust launch shortcuts give their launch configurations to
 * Eclipse, so that "Run" (Ctrl+F11) and "Debug" (F11) launch the last used
 * configuration.
 */
@SuppressWarnings("restriction")
class TestLaunchShortcuts extends AbstractCorrosionTest {

	private final List<ILaunchConfiguration> configurations = new ArrayList<>();
	private final List<ILaunch> launches = new ArrayList<>();

	@AfterEach
	void deleteConfigurations() throws CoreException {
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		for (ILaunch launch : launches) {
			launchManager.removeLaunch(launch);
		}
		for (ILaunchConfiguration configuration : configurations) {
			configuration.delete();
		}
	}

	@Test
	void testShortcutGivesConfigurationsOfSelectedProjectOnly() throws IOException, CoreException {
		IProject project = getProject(BASIC_PROJECT_NAME);
		ILaunchConfiguration config = save(CargoRunDelegate.CARGO_RUN_LAUNCH_CONFIG_TYPE, project.getName(), false);
		save(CargoRunDelegate.CARGO_RUN_LAUNCH_CONFIG_TYPE, "otherProject", false); //$NON-NLS-1$
		save(CargoRunDelegate.CARGO_RUN_LAUNCH_CONFIG_TYPE, project.getName(), true); // private

		assertArrayEquals(new ILaunchConfiguration[] { config },
				new CargoRunDelegate().getLaunchConfigurations(new StructuredSelection(project)));
		// A file of the project gives the configurations of its project
		assertArrayEquals(new ILaunchConfiguration[] { config }, new CargoRunDelegate()
				.getLaunchConfigurations(new StructuredSelection(project.getFile("src/main.rs")))); //$NON-NLS-1$
		assertEquals(0, new CargoRunDelegate().getLaunchConfigurations(StructuredSelection.EMPTY).length);
	}

	@Test
	void testProjectTestConfigurationIsNotASingleTestConfiguration() throws IOException, CoreException {
		IProject project = getProject(BASIC_PROJECT_NAME);
		ILaunchConfiguration singleTest = CargoTestDelegate.getSingleTestConfiguration(project,
				new TestAtCursor("tests::it_works", "--lib", "--exact")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		configurations.add(singleTest);

		ILaunchConfiguration forProject = RustLaunchDelegateTools.getLaunchConfiguration(project,
				CargoTestDelegate.CARGO_TEST_LAUNCH_CONFIG_TYPE_ID);
		assertTrue(forProject.isWorkingCopy(), "a new configuration for all tests is expected"); //$NON-NLS-1$

		ILaunchConfiguration allTests = save(CargoTestDelegate.CARGO_TEST_LAUNCH_CONFIG_TYPE_ID, project.getName(),
				false);
		assertEquals(allTests, RustLaunchDelegateTools.getLaunchConfiguration(project,
				CargoTestDelegate.CARGO_TEST_LAUNCH_CONFIG_TYPE_ID));
	}

	@Test
	void testSingleTestConfiguration() throws IOException, CoreException {
		IProject project = getProject(BASIC_PROJECT_NAME);
		TestAtCursor test = new TestAtCursor("tests::it_works", "--package basic --lib", "--exact"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		ILaunchConfiguration config = CargoTestDelegate.getSingleTestConfiguration(project, test);
		configurations.add(config);

		assertFalse(config.isWorkingCopy(), "the configuration must be saved for the launch history"); //$NON-NLS-1$
		assertEquals(project.getName(), config.getAttribute(RustLaunchDelegateTools.PROJECT_ATTRIBUTE, "")); //$NON-NLS-1$
		assertEquals("tests::it_works", config.getAttribute(CargoTestDelegate.TEST_NAME_ATTRIBUTE, "")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("--package basic --lib", config.getAttribute(RustLaunchDelegateTools.OPTIONS_ATTRIBUTE, "")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("--exact", config.getAttribute(RustLaunchDelegateTools.ARGUMENTS_ATTRIBUTE, "")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse(config.getName().contains("::"), "\"::\" is not permitted in a configuration name"); //$NON-NLS-1$ //$NON-NLS-2$

		// The same test gives the same configuration
		assertEquals(config, CargoTestDelegate.getSingleTestConfiguration(project, test));
	}

	@Test
	void testDebugShortcutDebugsProgramIfNoTestRanLast() throws IOException, CoreException {
		IProject project = getProject(BASIC_PROJECT_NAME);
		ILaunchConfiguration debugConfig = save(RustLaunchDelegateTools.CORROSION_DEBUG_LAUNCH_CONFIG_TYPE,
				project.getName(), false);
		ILaunchConfiguration runConfig = save(CargoRunDelegate.CARGO_RUN_LAUNCH_CONFIG_TYPE, project.getName(),
				false);
		addToLaunchHistory(runConfig);

		assertArrayEquals(new ILaunchConfiguration[] { debugConfig },
				new RustDebugDelegate().getLaunchConfigurations(new StructuredSelection(project)));
	}

	@Test
	void testDebugShortcutDebugsTestIfTestRanLast() throws IOException, CoreException {
		IProject project = getProject(BASIC_PROJECT_NAME);
		save(RustLaunchDelegateTools.CORROSION_DEBUG_LAUNCH_CONFIG_TYPE, project.getName(), false);
		ILaunchConfiguration runConfig = save(CargoRunDelegate.CARGO_RUN_LAUNCH_CONFIG_TYPE, project.getName(),
				false);
		ILaunchConfiguration testConfig = save(CargoTestDelegate.CARGO_TEST_LAUNCH_CONFIG_TYPE_ID, project.getName(),
				false);
		addToLaunchHistory(runConfig);
		addToLaunchHistory(testConfig);

		assertEquals(testConfig, RustLaunchDelegateTools.lastRunOrTestConfiguration(project));
		assertArrayEquals(new ILaunchConfiguration[] { testConfig },
				new RustDebugDelegate().getLaunchConfigurations(new StructuredSelection(project)));
	}

	@Test
	void testUnitTestViewElementGivesCargoProject() throws IOException, CoreException {
		IProject project = getProject(BASIC_PROJECT_NAME);
		ILaunchConfiguration testConfig = save(CargoTestDelegate.CARGO_TEST_LAUNCH_CONFIG_TYPE_ID, project.getName(),
				false);
		assertEquals(project, Adapters.adapt(testElement(testConfig), IResource.class));

		// The adapter is only for Cargo tests
		ILaunchConfiguration runConfig = save(CargoRunDelegate.CARGO_RUN_LAUNCH_CONFIG_TYPE, project.getName(),
				false);
		assertNull(Adapters.adapt(testElement(runConfig), IResource.class));
	}

	private ILaunchConfiguration save(String type, String projectName, boolean isPrivate) throws CoreException {
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationWorkingCopy wc = launchManager.getLaunchConfigurationType(type).newInstance(null,
				launchManager.generateLaunchConfigurationName(getClass().getSimpleName()));
		wc.setAttribute(RustLaunchDelegateTools.PROJECT_ATTRIBUTE, projectName);
		if (isPrivate) {
			wc.setAttribute(IDebugUIConstants.ATTR_PRIVATE, true);
		}
		ILaunchConfiguration config = wc.doSave();
		configurations.add(config);
		return config;
	}

	/**
	 * Adds a launch of the configuration to the launch manager without a process.
	 * The launch history of the "Run" group then has the configuration first.
	 */
	private void addToLaunchHistory(ILaunchConfiguration config) {
		// Make sure that the launch history exists and listens to launches
		DebugUIPlugin.getDefault().getLaunchConfigurationManager()
				.getLaunchHistory(IDebugUIConstants.ID_RUN_LAUNCH_GROUP);
		ILaunch launch = new Launch(config, ILaunchManager.RUN_MODE, null);
		launches.add(launch);
		DebugPlugin.getDefault().getLaunchManager().addLaunch(launch);
		waitUntil(Display.getDefault(), Duration.ofSeconds(5),
				() -> config.equals(DebugUIPlugin.getDefault().getLaunchConfigurationManager()
						.getLaunchHistory(IDebugUIConstants.ID_RUN_LAUNCH_GROUP).getRecentLaunch()));
	}

	/** A test of the unit test view, with a test run of the configuration */
	private static ITestElement testElement(ILaunchConfiguration config) {
		ILaunch launch = new Launch(config, ILaunchManager.RUN_MODE, null);
		Object[] session = new Object[1];
		session[0] = Proxy.newProxyInstance(TestLaunchShortcuts.class.getClassLoader(),
				new Class<?>[] { ITestRunSession.class }, (proxy, method, args) -> switch (method.getName()) {
				case "getLaunch" -> launch; //$NON-NLS-1$
				case "getTestRunSession" -> session[0]; //$NON-NLS-1$
				case "hashCode" -> System.identityHashCode(proxy); //$NON-NLS-1$
				case "equals" -> proxy == args[0]; //$NON-NLS-1$
				case "toString" -> "test run of " + config.getName(); //$NON-NLS-1$ //$NON-NLS-2$
				default -> null;
				});
		return (ITestElement) session[0];
	}
}
