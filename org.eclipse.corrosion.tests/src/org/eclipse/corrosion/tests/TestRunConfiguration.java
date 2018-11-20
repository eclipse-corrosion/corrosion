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
package org.eclipse.corrosion.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.corrosion.run.CargoRunDelegate;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.tests.harness.util.DisplayHelper;
import org.junit.After;
import org.junit.Test;

public class TestRunConfiguration extends AbstractCorrosionTest {

	@After
	public void testErrorPopup() {
		Shell errorPopup = getErrorPopup();
		if (errorPopup != null) {
			errorPopup.close();
		}
	}

	@Test
	public void testBasicRun() throws IOException, CoreException, InterruptedException {
		CargoRunDelegate delegate = new CargoRunDelegate();
		IProject project = getProject(BASIC_PROJECT_NAME);
		delegate.launch(new StructuredSelection(project), "run");
		new DisplayHelper() {
			@Override
			protected boolean condition() {
				return DebugPlugin.getDefault().getLaunchManager().getProcesses().length != 0 || getErrorPopup() != null;
			}
		}.waitForCondition(Display.getCurrent(), 15000);
		assertNull(getErrorPopup());
		assertTrue(DebugPlugin.getDefault().getLaunchManager().getProcesses().length != 0);
		for (IProcess process : DebugPlugin.getDefault().getLaunchManager().getProcesses()) {
			if (process.getLabel().equals("cargo run")) {
				while (!process.isTerminated()) {
					Thread.sleep(50);
				}
				assertEquals(0, process.getExitValue());
				return;
			}
		}
	}

	@Test
	public void testFailOnFakeProjectName() throws IOException, CoreException {
		IProject project = getProject(BASIC_PROJECT_NAME);
		ILaunchConfigurationWorkingCopy launchConfiguration = createLaunchConfiguration(project);
		launchConfiguration.setAttribute("PROJECT", "fakeProjectName");
		confirmErrorPopup(launchConfiguration);
	}

	@Test
	public void testFailOnDeletedProject() throws IOException, CoreException {
		IProject project = getProject(BASIC_PROJECT_NAME);
		ILaunchConfigurationWorkingCopy launchConfiguration = createLaunchConfiguration(project);
		project.delete(true, new NullProgressMonitor());
		confirmErrorPopup(launchConfiguration);
	}

	@Test
	public void testFailOnNonCargoProject() throws IOException, CoreException {
		IProject project = getProject(NOT_CARGO_PROJECT_NAME);
		ILaunchConfigurationWorkingCopy launchConfiguration = createLaunchConfiguration(project);
		confirmErrorPopup(launchConfiguration);
	}

	@Test
	public void testTranslateVariablesInBuildCommand() throws InterruptedException, IOException, CoreException {
		IProject project = getProject(BASIC_PROJECT_NAME);
		ILaunchConfigurationWorkingCopy launchConfiguration = createLaunchConfiguration(project);
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

	private static ILaunchConfigurationWorkingCopy createLaunchConfiguration(IProject project) throws CoreException {
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType configType = launchManager
				.getLaunchConfigurationType("org.eclipse.corrosion.run.CargoRunDelegate");
		ILaunchConfigurationWorkingCopy wc = configType.newInstance(project, "launch");
		wc.setAttribute("PROJECT", project.getName());
		return wc;
	}

	private static void confirmErrorPopup(ILaunchConfiguration configuration) throws CoreException {
		configuration.launch(ILaunchManager.RUN_MODE, new NullProgressMonitor());
		new DisplayHelper() {
			@Override
			protected boolean condition() {
				return getErrorPopup() != null;
			}
		}.waitForCondition(Display.getDefault(), 15000);

		assertNotNull(getErrorPopup());
	}

	private static Shell getErrorPopup() {
		for (Shell shell : Display.getDefault().getShells()) {
			if (shell.getText().equals("Unable to Launch")) {
				return shell;
			}
		}
		return null;
	}
}
