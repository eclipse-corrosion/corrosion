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
 *  Lucas Bullen (Red Hat Inc.) - Initial implementation
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.lsp4e.LanguageServiceAccessor;
import org.eclipse.lsp4e.LanguageServiceAccessor.LSPDocumentInfo;
import org.eclipse.lsp4j.CompletionItemKind;

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
		JsonParser parser = new JsonParser();
		try {
			URL url = FileLocator.toFileURL(FileLocator.find(CorrosionPlugin.getDefault().getBundle(),
					Path.fromPortableString("snippets/rust.json"), Collections.emptyMap())); //$NON-NLS-1$
			StringBuilder snippetsBuilder = new StringBuilder();
			Files.readAllLines(new File(url.getFile()).toPath()).forEach(line -> snippetsBuilder.append(line));
			snippetArray = parser.parse(snippetsBuilder.toString()).getAsJsonArray();
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

		Collection<LSPDocumentInfo> infos = LanguageServiceAccessor.getLSPDocumentInfosFor(document,
				capabilities -> Boolean.TRUE.equals(capabilities.getReferencesProvider()));

		List<ICompletionProposal> proposals = new ArrayList<>();
		for (Snippet snippet : snippets) {
			if (snippet.matchesPrefix(prefix)) {
				proposals.add(snippet.convertToCompletionProposal(offset, infos.iterator().next(), prefix, indent));
			}
		}
		return proposals.toArray(new ICompletionProposal[proposals.size()]);
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
