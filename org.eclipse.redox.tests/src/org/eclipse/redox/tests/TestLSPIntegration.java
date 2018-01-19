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
 *  Mickael Istria (Red Hat Inc.) - Source Reference
 *  Lucas Bullen   (Red Hat Inc.) - Initial implementation
 *******************************************************************************/
package org.eclipse.redox.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.lsp4e.LanguageServiceAccessor;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.tests.harness.util.DisplayHelper;
import org.junit.Assert;
import org.junit.Test;

public class TestLSPIntegration extends AbstractRedoxTest {

	@Test
	public void testLSFound() throws Exception {
		IProject project = getProject("basic");
		IFile rustFile = project.getFolder("src").getFile("main.rs");
		LanguageServer languageServer = LanguageServiceAccessor
				.getLanguageServers(rustFile, capabilities -> capabilities.getHoverProvider() != null).iterator()
				.next();
		String uri = rustFile.getLocationURI().toString();
		Either<List<CompletionItem>, CompletionList> completionItems = languageServer.getTextDocumentService()
				.completion(new TextDocumentPositionParams(new TextDocumentIdentifier(uri), new Position(1, 4)))
				.get(1, TimeUnit.MINUTES);
		Assert.assertNotNull(completionItems);
	}

	@Test
	public void testLSWorks() throws Exception {
		IProject project = getProject("basic_errors");
		IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		IEditorPart editor = null;
		IFile file = project.getFolder("src").getFile("main.rs");
		editor = IDE.openEditor(activePage, file);
		new DisplayHelper() {
			@Override
			protected boolean condition() {
				try {
					return file.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO)[0]
							.getAttribute(IMarker.LINE_NUMBER, -1) == 3;
				} catch (Exception e) {
					return false;
				}
			}
		}.waitForCondition(editor.getEditorSite().getShell().getDisplay(), 30000);
		IMarker marker = file.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO)[0];
		assertTrue(marker.getType().contains("lsp4e"));
		assertEquals(3, marker.getAttribute(IMarker.LINE_NUMBER, -1));
	}
}
