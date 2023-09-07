/*********************************************************************
 * Copyright (c) 2018, 2023 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Lucas Bullen (Red Hat Inc.) - Initial implementation
 *  Max Bureck (Fraunhofer FOKUS) - Working around depricated LSP4E API
 *******************************************************************************/
package org.eclipse.corrosion.snippet;

import java.util.Arrays;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.lsp4e.LanguageServiceAccessor.LSPDocumentInfo;
import org.eclipse.lsp4e.operations.completion.LSCompletionProposal;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.InsertTextFormat;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

@SuppressWarnings("restriction")
public class Snippet {
	private String[] replacementLines;
	private String display;
	private CompletionItemKind kind;

	public Snippet(String display, CompletionItemKind kind, String[] replacementLines) {
		if (replacementLines == null) {
			this.replacementLines = null;
		} else {
			this.replacementLines = Arrays.copyOf(replacementLines, replacementLines.length);
		}
		this.display = display;
		this.kind = kind;
	}

	public ICompletionProposal convertToCompletionProposal(int offset, LSPDocumentInfo info, String lineIndentation,
			Range textRange) {
		CompletionItem item = new CompletionItem();
		item.setLabel(display);
		item.setKind(kind);
		item.setSortText("zzzSnippet" + display); //$NON-NLS-1$
		item.setInsertTextFormat(InsertTextFormat.Snippet);

		IDocument document = info.getDocument();
		// if there is a text selection, take it, since snippets with $TM_SELECTED_TEXT
		// will want to wrap the selection.
		item.setTextEdit(Either.forLeft(new TextEdit(textRange, createReplacement(lineIndentation))));
		return new LSCompletionProposal(document, offset, item, info.getLanguageServerWrapper());
	}

	public boolean matchesPrefix(String prefix) {
		return this.display.startsWith(prefix);
	}

	private String createReplacement(String lineIndentation) {
		StringBuilder responseBuilder = new StringBuilder();

		if (replacementLines.length == 1) {
			return replacementLines[0];
		} else if (replacementLines.length > 1) {
			for (String line : replacementLines) {
				if (responseBuilder.length() == 0) {
					responseBuilder.append(line);
					continue;
				}
				responseBuilder.append('\n');
				responseBuilder.append(lineIndentation);
				responseBuilder.append(line);
			}
		}
		return responseBuilder.toString();
	}
}
