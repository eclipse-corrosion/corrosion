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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
import org.junit.Test;

public class TestRunConfiguration extends AbstractCorrosionTest {

	@Test
	public void testBasicRun() throws Exception {
		CargoRunDelegate delegate = new CargoRunDelegate();
		IProject project = getProject("basic");
		delegate.launch(new StructuredSelection(project), "run");
		new DisplayHelper() {
			@Override
			protected boolean condition() {
				return DebugPlugin.getDefault().getLaunchManager().getProcesses().length != 0 || errorPopupExists();
			}
		}.waitForCondition(Display.getCurrent(), 15000);
		assertFalse(errorPopupExists());
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

	// TODO: Unable to test multiple failing runs do to bug
	// computer crashes if multiple popups are made one after another
	// a popup is made if the configuration is unable to run
	@Test
	public void testFailOnFakeProjectName() throws Exception {
		IProject project = getProject("basic");
		ILaunchConfigurationWorkingCopy launchConfiguration = createLaunchConfiguration(project, "launch");
		launchConfiguration.setAttribute("PROJECT", "fakeProjectName");
		confirmErrorPopup(launchConfiguration);
	}

	@Test
	public void testFailOnDeletedProject() throws Exception {
		IProject project = getProject("basic");
		ILaunchConfigurationWorkingCopy launchConfiguration = createLaunchConfiguration(project, "launch");
		project.delete(true, new NullProgressMonitor());
		confirmErrorPopup(launchConfiguration);
	}

	@Test
	public void testFailOnNonCargoProject() throws Exception {
		IProject project = getProject("not_cargo");
		ILaunchConfigurationWorkingCopy launchConfiguration = createLaunchConfiguration(project, "launch");
		confirmErrorPopup(launchConfiguration);
	}

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
				.getLaunchConfigurationType("org.eclipse.corrosion.run.CargoRunDelegate");
		ILaunchConfigurationWorkingCopy wc = configType.newInstance(project, name);
		wc.setAttribute("PROJECT", project.getName());
		return wc;
	}

	private void confirmErrorPopup(ILaunchConfiguration configuration) throws Exception {
		configuration.launch(ILaunchManager.RUN_MODE, new NullProgressMonitor());
		new DisplayHelper() {
			@Override
			protected boolean condition() {
				return errorPopupExists();
			}
		}.waitForCondition(Display.getDefault(), 15000);

		assertTrue(errorPopupExists());
	}

	private boolean errorPopupExists() {
		for (Shell shell : Display.getDefault().getShells()) {
			if (shell.getText().equals("Unable to Launch")) {
				return true;
			}
		}
		return false;
	}
}
