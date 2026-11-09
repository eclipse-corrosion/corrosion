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

import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.corrosion.CorrosionPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.lsp4e.ContentTypeToLanguageServerDefinition;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.lsp4e.LanguageServerWrapper;
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
			} catch (RuntimeException ex) {
				// Keep the timeout as the cause of the failure
				facts = "(no facts about the language server: " + ex + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			}
			fail("No error marker of rust-analyzer. " + facts, e); //$NON-NLS-1$
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
	private static String describeLanguageServer(IFile file, IDocument document) {
		StringBuilder text = new StringBuilder();
		IPreferenceStore store = CorrosionPlugin.getDefault().getPreferenceStore();
		String path = store.getString(CorrosionPreferenceInitializer.RLS_PATH_PREFERENCE);
		File executable = new File(path);
		text.append("rust-analyzer path: \"").append(path).append("\", exists: ").append(executable.exists()) //$NON-NLS-1$ //$NON-NLS-2$
				.append(", executable: ").append(executable.canExecute()); //$NON-NLS-1$
		text.append("; --version: \"").append(CorrosionPlugin.getOutputFromCommand(path, "--version")).append('"'); //$NON-NLS-1$ //$NON-NLS-2$
		text.append("; sysroot: \"") //$NON-NLS-1$
				.append(store.getString(CorrosionPreferenceInitializer.SYSROOT_PATH_PREFERENCE)).append('"');
		text.append("; language server for the document: ") //$NON-NLS-1$
				.append(LanguageServers.forDocument(document).anyMatching());
		text.append("; content types: ").append(LSPEclipseUtils.getDocumentContentTypes(document).stream() //$NON-NLS-1$
				.map(IContentType::getId).toList());
		URI uri = LSPEclipseUtils.toUri(document);
		for (ContentTypeToLanguageServerDefinition mapping : LanguageServersRegistry.getInstance()
				.getContentTypeToLSPExtensions()) {
			if (mapping.getValue().id.startsWith("org.eclipse.corrosion")) { //$NON-NLS-1$
				text.append("; mapping ").append(mapping.getKey().getId()).append(" -> ") //$NON-NLS-1$ //$NON-NLS-2$
						.append(mapping.getValue().id).append(" user enabled: ").append(mapping.isUserEnabled()) //$NON-NLS-1$
						.append(", enabled for file: ").append(uri != null && mapping.isEnabled(uri)); //$NON-NLS-1$
			}
		}
		text.append("; started servers:"); //$NON-NLS-1$
		for (LanguageServerWrapper wrapper : LanguageServiceAccessor.getStartedWrappers(capabilities -> true, true)) {
			text.append(" [").append(wrapper.serverDefinition.id).append(" active: ").append(wrapper.isActive()) //$NON-NLS-1$ //$NON-NLS-2$
					.append(", startup failed: ").append(wrapper.startupFailed()).append(']'); //$NON-NLS-1$
		}
		text.append("; markers:"); //$NON-NLS-1$
		try {
			for (IMarker marker : file.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO)) {
				text.append(" [").append(marker.getType()).append(" line ") //$NON-NLS-1$ //$NON-NLS-2$
						.append(marker.getAttribute(IMarker.LINE_NUMBER, -1)).append(" severity ") //$NON-NLS-1$
						.append(marker.getAttribute(IMarker.SEVERITY, -1)).append(": ") //$NON-NLS-1$
						.append(marker.getAttribute(IMarker.MESSAGE, "")).append(']'); //$NON-NLS-1$
			}
		} catch (CoreException e) {
			text.append(' ').append(e);
		}
		return text.toString();
	}
}
