/*********************************************************************
 * Copyright (c) 2018 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Mickael Istria (Red Hat Inc.) - Initial implementation
 *******************************************************************************/
package org.eclipse.corrosion.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.corrosion.debug.RustDebugDelegate;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.tests.harness.util.DisplayHelper;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestDebugConfiguration extends AbstractCorrosionTest {

	private static List<IStatus> errors = new ArrayList<>();
	private static ILogListener listener = (status, plugin) -> {
		if (status.getSeverity() == IStatus.ERROR) {
			errors.add(status);
		}
	};

	@BeforeClass
	public static void setUpListener() {
		CorrosionPlugin.getDefault().getLog().addLogListener(listener);
	}

	@AfterClass
	public static void removeListener() {
		CorrosionPlugin.getDefault().getLog().removeLogListener(listener);
	}

	@Before
	public void clearErrors() {
		errors.clear();
	}

	@Test
	public void testDebugLaunch() throws Exception {
		IProject project = getProject(BASIC_PROJECT_NAME);
		IFile file = project.getFile("src/main.rs");
		assertTrue(file.exists());
		RustDebugDelegate delegate = new RustDebugDelegate();
		delegate.launch(new StructuredSelection(project), "debug");
		assertEquals(Collections.emptyList(), errors);
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

	private static boolean errorPopupExists() {
		for (Shell shell : Display.getDefault().getShells()) {
			if (shell.getText().equals("Unable to Launch")) {
				return true;
			}
		}
		return false;
	}
}
