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

import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.corrosion.CorrosionPreferenceInitializer;
import org.eclipse.jface.text.IDocument;
import org.eclipse.lsp4e.LanguageServers;
import org.eclipse.lsp4e.LanguageServersRegistry;
import org.eclipse.lsp4e.LanguageServiceAccessor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.opentest4j.AssertionFailedError;

@SuppressWarnings("restriction")
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
		IDocument document = ((ITextEditor) editor).getDocumentProvider().getDocument(editor.getEditorInput());
		try {
			waitForErrorMarker(display, file);
		} catch (AssertionFailedError e) {
			String facts;
			try {
				facts = describeLanguageServer(file, document);
			} catch (RuntimeException | CoreException ex) {
				// Keep the timeout as the cause of the failure
				facts = "(no facts about the language server: " + ex + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			}
			fail("No error marker of rust-analyzer.\n" + facts, e); //$NON-NLS-1$
		}
	}

	private static void waitForErrorMarker(Display display, IFile file) {
		waitUntil(display, Duration.ofMinutes(1), () -> {
			try {
				return Arrays.stream(file.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO)).anyMatch(marker -> {
					try {
						// projects/basic_errors/src/main.rs has no "}" at the end of "fn main() {".
						// The syntax check of rust-analyzer reports this at once on lines 1 and 2.
						// "cargo check" reports it later on line 3. Any of these errors is
						// sufficient.
						return marker.getType().contains("lsp4e")
								&& marker.getAttribute(IMarker.SEVERITY, -1) == IMarker.SEVERITY_ERROR;
					} catch (CoreException ex) {
						return false;
					}
				});
			} catch (Exception e) {
				return false;
			}
		});
	}

	/** Facts about the language server, to find the cause when the test fails */
	private static String describeLanguageServer(IFile file, IDocument document) throws CoreException {
		String path = CorrosionPlugin.getDefault().getPreferenceStore()
				.getString(CorrosionPreferenceInitializer.RLS_PATH_PREFERENCE);
		String mappings = LanguageServersRegistry.getInstance().getContentTypeToLSPExtensions().stream()
				.filter(mapping -> mapping.getValue().id.startsWith("org.eclipse.corrosion")) //$NON-NLS-1$
				.map(mapping -> "%s (user enabled: %s)".formatted(mapping.getValue().id, mapping.isUserEnabled())) //$NON-NLS-1$
				.collect(joining(", ")); //$NON-NLS-1$
		String servers = LanguageServiceAccessor.getStartedWrappers(capabilities -> true, true).stream()
				.map(wrapper -> "%s (active: %s, startup failed: %s)".formatted(wrapper.serverDefinition.id, //$NON-NLS-1$
						wrapper.isActive(), wrapper.startupFailed()))
				.collect(joining(", ")); //$NON-NLS-1$
		String markers = Arrays.stream(file.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO))
				.map(marker -> "line %d: %s".formatted(marker.getAttribute(IMarker.LINE_NUMBER, -1), //$NON-NLS-1$
						marker.getAttribute(IMarker.MESSAGE, ""))) //$NON-NLS-1$
				.collect(joining(", ")); //$NON-NLS-1$
		return """
				rust-analyzer: %s, version: %s
				language server for the file: %s
				mappings: %s
				started servers: %s
				markers: %s""".formatted(path, CorrosionPlugin.getOutputFromCommand(path, "--version"), //$NON-NLS-1$
				LanguageServers.forDocument(document).anyMatching(), mappings, servers, markers);
	}
}
