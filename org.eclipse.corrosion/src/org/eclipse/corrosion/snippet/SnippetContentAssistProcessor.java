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
 *  Lucas Bullen (Red Hat Inc.) - Initial implementation
 *  Max Bureck (Fraunhofer FOKUS) - Implemented "surround with" style snippets
 *******************************************************************************/
package org.eclipse.corrosion.snippet;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension9;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.lsp4e.LanguageServiceAccessor;
import org.eclipse.lsp4e.LanguageServiceAccessor.LSPDocumentInfo;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@SuppressWarnings("restriction")
public class SnippetContentAssistProcessor implements IContentAssistProcessor {
	private static final String SINGLE_LINE_COMMENT = "(?s).*(\\/\\/[^\\n]*)"; //$NON-NLS-1$
	private static final String MULTI_LINE_COMMENT = "(?s).*\\/\\*(.(?!\\*\\/))*"; //$NON-NLS-1$
	private static final String ENDS_WITH_WORD = "(?<indent>\\s*).*?(?<prefix>\\w*)"; //$NON-NLS-1$
	private static final Pattern ENDS_WITH_WORD_PATTERN = Pattern.compile(ENDS_WITH_WORD);
	private static final Pattern SINGLE_LINE_COMMENT_PATTERN = Pattern.compile(SINGLE_LINE_COMMENT);
	private static final Pattern MULTI_LINE_COMMENT_PATTERN = Pattern.compile(MULTI_LINE_COMMENT);
	private static final List<Snippet> snippets = new ArrayList<>();
	static {
		JsonArray snippetArray = null;
		try {
			URL url = FileLocator.toFileURL(FileLocator.find(CorrosionPlugin.getDefault().getBundle(),
					Path.fromPortableString("snippets/rust.json"), Collections.emptyMap())); //$NON-NLS-1$
			StringBuilder snippetsBuilder = new StringBuilder();
			Files.readAllLines(new File(url.getFile()).toPath()).forEach(snippetsBuilder::append);
			snippetArray = JsonParser.parseString(snippetsBuilder.toString()).getAsJsonArray();
		} catch (IOException e) {
			// Caught with null snippetArray
		}

		if (snippetArray != null) {
			for (JsonElement jsonElement : snippetArray) {
				JsonObject snippet = jsonElement.getAsJsonObject();

				String name = snippet.get("display").getAsString(); //$NON-NLS-1$
				int completionItemKind = snippet.get("completionItemKind").getAsInt(); //$NON-NLS-1$

				JsonArray replacementLines = snippet.get("replacementLines").getAsJsonArray(); //$NON-NLS-1$
				String[] lines = new String[replacementLines.size()];
				for (int i = 0; i < replacementLines.size(); i++) {
					lines[i] = replacementLines.get(i).getAsString();
				}

				addSnippet(new Snippet(name, CompletionItemKind.forValue(completionItemKind), lines));
			}
		}
	}

	private static void addSnippet(Snippet snippet) {
		snippets.add(snippet);
	}

	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		IDocument document = viewer.getDocument();
		String textToOffset = document.get().substring(0, offset);
		if (isOffsetInComment(textToOffset)) {
			return new ICompletionProposal[0];
		}

		Matcher matcher = ENDS_WITH_WORD_PATTERN.matcher(textToOffset.substring(textToOffset.lastIndexOf('\n') + 1));
		matcher.matches();
		String indent = matcher.group("indent"); //$NON-NLS-1$
		String prefix = matcher.group("prefix"); //$NON-NLS-1$

		// Use range from selection (if available) to support "surround with" style
		// completions
		Range range = getRangeFromSelection(document, viewer).orElseGet(() -> {
			// no selection available: get range from prefix
			try {
				int line = document.getLineOfOffset(offset);
				int lineOffset = offset - document.getLineOffset(line);
				Position start = new Position(line, lineOffset - prefix.length());
				Position end = new Position(line, lineOffset);
				return new Range(start, end);
			} catch (BadLocationException e) {
				return null;
			}
		});
		if (range == null) {
			return new ICompletionProposal[] {};
		}

		Collection<LSPDocumentInfo> infos = LanguageServiceAccessor.getLSPDocumentInfosFor(document,
				capabilities -> Boolean.TRUE.equals(capabilities.getReferencesProvider()));
		LSPDocumentInfo docInfo = infos.iterator().next();

		return snippets.stream().filter(s -> s.matchesPrefix(prefix))
				.map(s -> s.convertToCompletionProposal(offset, docInfo, prefix, indent, range))
				.toArray(ICompletionProposal[]::new);
	}

	/**
	 * Get the current selection from the given {@code viewer}. If there is a (non
	 * empty) selection returns a {@code Range} computed from the selection and
	 * returns this wrapped in an optional, otherwise returns an empty optional.
	 *
	 * @param document currently active document
	 * @param viewer   the text viewer for the completion
	 * @return either an optional containing the text selection, or an empty
	 *         optional, if there is no (non-empty) selection.
	 */
	private static Optional<Range> getRangeFromSelection(IDocument document, ITextViewer viewer) {
		if (!(viewer instanceof ITextViewerExtension9)) {
			return Optional.empty();
		}
		ITextViewerExtension9 textViewer = (ITextViewerExtension9) viewer;

		ITextSelection textSelection = textViewer.getLastKnownSelection();
		if (textSelection == null) {
			return Optional.empty();
		}
		int selectionLength = textSelection.getLength();
		if (selectionLength <= 0) {
			return Optional.empty();
		}

		try {
			int startOffset = textSelection.getOffset();
			Position startPosition = LSPEclipseUtils.toPosition(startOffset, document);
			int endOffset = startOffset + selectionLength;
			Position endPosition = LSPEclipseUtils.toPosition(endOffset, document);

			return Optional.of(new Range(startPosition, endPosition));
		} catch (BadLocationException e) {
			return Optional.empty();
		}
	}

	private static boolean isOffsetInComment(String textToOffset) {
		Matcher singleLineCommentMatcher = SINGLE_LINE_COMMENT_PATTERN.matcher(textToOffset);
		Matcher multiLineCommentMatcher = MULTI_LINE_COMMENT_PATTERN.matcher(textToOffset);
		return singleLineCommentMatcher.matches() || multiLineCommentMatcher.matches();
	}

	@Override
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
		return new IContextInformation[0];
	}

	@Override
	public char[] getCompletionProposalAutoActivationCharacters() {
		return new char[0];
	}

	@Override
	public char[] getContextInformationAutoActivationCharacters() {
		return new char[0];
	}

	@Override
	public String getErrorMessage() {
		return null;
	}

	@Override
	public IContextInformationValidator getContextInformationValidator() {
		return null;
	}

}
