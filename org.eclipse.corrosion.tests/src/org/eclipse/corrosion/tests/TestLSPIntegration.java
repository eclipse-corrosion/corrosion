/*********************************************************************
 * Copyright (c) 2017, 2018 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Mickael Istria (Red Hat Inc.) - Source Reference
 *  Lucas Bullen   (Red Hat Inc.) - Initial implementation
 *  Max Bureck (Fraunhofer FOKUS) - Give RLS more time to generate error
 *******************************************************************************/
package org.eclipse.corrosion.tests;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class TestLSPIntegration extends AbstractCorrosionTest {

	@Test
	@Timeout(value = 3, unit = TimeUnit.MINUTES)
	void testLSWorks() throws IOException, CoreException {
		IProject project = getProject(BASIC_ERRORS_PROJECT_NAME);
		IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		IEditorPart editor = null;
		// This is a workaround, since the test is failing occasionally.
		// The RLS may not be fully initialized without this timeout.
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		IFile file = project.getFolder("src").getFile("main.rs");
		editor = IDE.openEditor(activePage, file);
		Display display = editor.getEditorSite().getShell().getDisplay();
		waitUntil(display, Duration.ofMinutes(1), () -> {
			try {
				return Arrays.stream(file.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO)).anyMatch(marker -> {
					try {
						return marker.getType().contains("lsp4e") && marker.getAttribute(IMarker.LINE_NUMBER, -1) == 3;
					} catch (CoreException ex) {
						return false;
					}
				});
			} catch (Exception e) {
				return false;
			}
		});
	}
}
