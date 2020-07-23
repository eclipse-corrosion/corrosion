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
 *  Lucas Bullen   (Red Hat Inc.) - Initial implementation
 *******************************************************************************/
package org.eclipse.corrosion.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.corrosion.snippet.SnippetContentAssistProcessor;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.lsp4e.operations.completion.LSCompletionProposal;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;
import org.junit.jupiter.api.Test;

@SuppressWarnings("restriction")
public class TestSnippetContentAssistProcessor extends AbstractCorrosionTest {

	private static final String[] PRINT_COMPLETION_PROPOSALS = new String[] { "println!(\"$1\", $0)" };

	@Test
	public void testCompletionsPresent() throws IOException, CoreException {
		validateCompletionProposals("println", PRINT_COMPLETION_PROPOSALS);
	}

	@Test
	public void testCompletionsLimited() throws IOException, CoreException {
		validateCompletionProposals("noExpectedCompletion", null);
	}

	@Test
	public void testSingleLineComment() throws IOException, CoreException {
		validateCompletionProposals("// println", null);
		// With prefix
		validateCompletionProposals("not a comment // println", null);
		// With multiline
		validateCompletionProposals("not a comment \n also not a comment // println", null);
		// Comment on an above line
		validateCompletionProposals("// a comment \n println", PRINT_COMPLETION_PROPOSALS);
	}

	@Test
	public void testMultiLineComment() throws IOException, CoreException {
		validateCompletionProposals("/* println", null);
		// With prefix
		validateCompletionProposals("not a comment /* println", null);
		// With multiline
		validateCompletionProposals("not a comment \n not a comment /* println", null);
		// With mulitple opens
		validateCompletionProposals("not a comment \n not a comment /* in /* /* println", null);
		// Previous closure
		validateCompletionProposals("/* in a comment */ not a comment /* println", null);
		// Previous closue no open
		validateCompletionProposals("/* in a comment */ println", PRINT_COMPLETION_PROPOSALS);
		// Previous closue new line
		validateCompletionProposals("/* in a comment */ \n  println", PRINT_COMPLETION_PROPOSALS);

	}

	private void validateCompletionProposals(String text, String[] expectedProposalTexts)
			throws IOException, CoreException {
		IProject project = getProject(BASIC_PROJECT_NAME);
		IFile file = project.getFolder("src").getFile("main.rs");
		IEditorPart editor = IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), file);
		((ITextEditor) editor).getDocumentProvider().getDocument(editor.getEditorInput()).set(text);

		ICompletionProposal[] proposals = (new SnippetContentAssistProcessor()).computeCompletionProposals(
				(ITextViewer) editor.getAdapter(ITextOperationTarget.class), text.length() - 1);
		if (expectedProposalTexts == null) {
			assertTrue(proposals == null || proposals.length == 0);
			return;
		}
		assertNotNull(proposals);
		for (int i = 0; i < proposals.length; i++) {
			assertEquals(((LSCompletionProposal) proposals[i]).getItem().getTextEdit().getNewText(),
					expectedProposalTexts[i]);
		}
	}

}