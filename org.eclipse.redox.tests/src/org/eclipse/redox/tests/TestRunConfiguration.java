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
 *  Lucas Bullen   (Red Hat Inc.) - Initial implementation
 *******************************************************************************/
package org.eclipse.redox.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.tests.harness.util.DisplayHelper;
import org.junit.Test;

public class TestRunConfiguration extends AbstractRedoxTest {

	@Test
	public void testBasicRun() throws Exception {
		IProject project = getProject("basic");
		ILaunchConfiguration launchConfiguration = createLaunchConfiguration(project, "launch");
		confirmLaunchConfiguration(launchConfiguration, 0);
	}

	// TODO: Unable to test multiple failing runs do to bug
	// computer crashes if multiple popups are made one after another
	// a popup is made if the configuration is unable to run
	// @Test
	// public void testFailOnFakeProjectName() throws Exception {
	// IProject project = getProject("basic");
	// ILaunchConfigurationWorkingCopy launchConfiguration =
	// createLaunchConfiguration(project, "launch");
	// launchConfiguration.setAttribute("PROJECT", "fakeProjectName");
	// confirmLaunchConfiguration(launchConfiguration, 1);
	// }
	//
	// @Test
	// public void testFailOnDeletedProject() throws Exception {
	// IProject project = getProject("basic");
	// ILaunchConfigurationWorkingCopy launchConfiguration =
	// createLaunchConfiguration(project, "launch");
	// project.delete(true, new NullProgressMonitor());
	// confirmLaunchConfiguration(launchConfiguration, 1);
	// }
	//
	// @Test
	// public void testFailOnNonCargoProject() throws Exception {
	// IProject project = getProject("not_cargo");
	// ILaunchConfigurationWorkingCopy launchConfiguration =
	// createLaunchConfiguration(project, "launch");
	// confirmLaunchConfiguration(launchConfiguration, 1);
	// }

	@Test
	public void testTranslateVariablesInBuildCommand() throws Exception {
		IProject project = getProject("basic");
		ILaunchConfigurationWorkingCopy launchConfiguration = createLaunchConfiguration(project, "launch");
		launchConfiguration.setAttribute("BUILD_COMMAND", "-- ${workspace_loc}");
		ILaunch launch = launchConfiguration.launch(ILaunchManager.RUN_MODE, new NullProgressMonitor());

		new DisplayHelper() {
			@Override
			protected boolean condition() {
				return launch.getProcesses().length != 0;
			}
		}.waitForCondition(Display.getDefault(), 15000);

		for (IProcess process : launch.getProcesses()) {
			if (process.getLabel().equals("cargo run")) {
				while (!process.isTerminated()) {
					Thread.sleep(50);
				}
				String command = process.getAttribute(IProcess.ATTR_CMDLINE);
				// confirm ${workspace_loc} has been replaced with its actual value
				assertTrue(command
						.matches(".*" + ResourcesPlugin.getWorkspace().getRoot().getLocation().toString() + ".*"));
				assertEquals(0, process.getExitValue());
				return;
			}
		}
		fail();
	}

	private ILaunchConfigurationWorkingCopy createLaunchConfiguration(IProject project, String name)
			throws CoreException {
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType configType = launchManager
				.getLaunchConfigurationType("org.eclipse.redox.run.CargoRunDelegate");
		ILaunchConfigurationWorkingCopy wc = configType.newInstance(project, name);
		wc.setAttribute("PROJECT", project.getName());
		return wc;
	}

	private void confirmLaunchConfiguration(ILaunchConfiguration configuration, int expectedExitValue)
			throws Exception {
		ILaunch launch = configuration.launch(ILaunchManager.RUN_MODE, new NullProgressMonitor());

		new DisplayHelper() {
			@Override
			protected boolean condition() {
				return launch.getProcesses().length != 0;
			}
		}.waitForCondition(Display.getDefault(), 15000);

		for (IProcess process : launch.getProcesses()) {
			if (process.getLabel().equals("cargo run")) {
				while (!process.isTerminated()) {
					Thread.sleep(50);
				}
				assertEquals(expectedExitValue, process.getExitValue());
				return;
			}
		}
		assertEquals(expectedExitValue, 1);
	}
}
