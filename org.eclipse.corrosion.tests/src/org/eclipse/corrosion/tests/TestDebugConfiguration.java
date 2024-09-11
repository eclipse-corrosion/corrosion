/*********************************************************************
 * Copyright (c) 2018, 2021 Red Hat Inc. and others.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.corrosion.debug.RustDebugDelegate;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.ui.console.IConsole;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TestDebugConfiguration extends AbstractCorrosionTest {

	private static List<IStatus> errors = new ArrayList<>();
	private static ILogListener listener = (status, plugin) -> {
		if (status.getSeverity() == IStatus.ERROR) {
			errors.add(status);
		}
	};

	@BeforeAll
	public static void setUpListener() {
		CorrosionPlugin.getDefault().getLog().addLogListener(listener);
	}

	@AfterAll
	public static void removeListener() {
		CorrosionPlugin.getDefault().getLog().removeLogListener(listener);
	}

	@BeforeEach
	public void clearErrors() {
		errors.clear();
	}

	@AfterEach
	@BeforeEach
	public void stopLaunchesAndCloseConsoles() throws DebugException {
		for (ILaunch launch : DebugPlugin.getDefault().getLaunchManager().getLaunches()) {
			launch.terminate();
		}
		IConsoleManager consoleManager = ConsolePlugin.getDefault().getConsoleManager();
		consoleManager.removeConsoles(consoleManager.getConsoles());
		IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		for (IViewReference view : activePage.getViewReferences()) {
			if (IConsoleConstants.ID_CONSOLE_VIEW.equals(view.getId())) {
				activePage.hideView(view);
			}
		}
	}

	@Test
	void testDebugLaunch() throws Exception {
		IProject project = getProject(BASIC_PROJECT_NAME);
		IFile file = project.getFile("src/main.rs");
		assertTrue(file.exists());
		RustDebugDelegate delegate = new RustDebugDelegate();
		delegate.launch(new StructuredSelection(project), "debug");
		assertEquals(Collections.emptyList(), errors);
		waitUntil(Display.getCurrent(), Duration.ofSeconds(15), () -> {
			IConsole console = getApplicationConsole("basic");
			return (console != null && console.getDocument().get().contains("5 is positive"))
					|| getErrorPopupMessage() != null;
		});
		assertNull(getErrorPopupMessage());
		assertTrue(getApplicationConsole("basic").getDocument().get().contains("5 is positive"));
	}

	/**
	 *
	 * @return the error message or null if there is no "Unable to Launch" popup
	 */
	static String getErrorPopupMessage() {
		for (Shell shell : Display.getDefault().getShells()) {
			if (shell.getText().equals("Unable to Launch")) {
				String res = findLabel(shell);
				if (res == null || res.isEmpty()) {
					res = "Could not determine error message, but there was an error popup";
				}
				return res;
			}
		}
		return null;
	}

	static IConsole getApplicationConsole(String binaryName) {
		for (org.eclipse.ui.console.IConsole console : ConsolePlugin.getDefault().getConsoleManager().getConsoles()) {
			if (console instanceof IConsole debugConsole && debugConsole.getProcess().getLabel().endsWith(binaryName)) {
				return debugConsole;
			}
		}
		return null;
	}

	static String findLabel(Composite composite) {
		for (Control control : composite.getChildren()) {
			if (control instanceof Label l && !l.getText().isEmpty()) {
				return l.getText();
			} else if (control instanceof Composite c) {
				return findLabel(c);
			}
		}
		return null;
	}
}
