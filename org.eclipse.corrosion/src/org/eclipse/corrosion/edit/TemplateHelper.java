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
package org.eclipse.corrosion.edit;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.GlobalTemplateVariables;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.eclipse.lsp4e.LanguageServiceAccessor.LSPDocumentInfo;
import org.eclipse.lsp4e.operations.completion.LSCompletionProposal;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.InsertTextFormat;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.ui.editors.text.templates.ContributionContextTypeRegistry;
import org.eclipse.ui.editors.text.templates.ContributionTemplateStore;

@SuppressWarnings("restriction")
public class TemplateHelper {
	private static final String SINGLE_LINE_COMMENT = "(?s).*(\\/\\/[^\\n]*)"; //$NON-NLS-1$
	private static final String MULTI_LINE_COMMENT = "(?s).*\\/\\*(.(?!\\*\\/))*"; //$NON-NLS-1$
	// A '$' escapes a '$' so the prefix ${ must be preceded by an odd number of '$'
	private static final String VALID_TAG_PREFIX = "(?s).*(?<!\\$)(\\$\\$)*\\$\\{"; //$NON-NLS-1$
	private static final Pattern SINGLE_LINE_COMMENT_PATTERN = Pattern.compile(SINGLE_LINE_COMMENT);
	private static final Pattern MULTI_LINE_COMMENT_PATTERN = Pattern.compile(MULTI_LINE_COMMENT);

	private static HashMap<String, TemplateStore> fStoreList = new HashMap<>();
	private static ContributionContextTypeRegistry fRegistry;

	/**
	 * Gives the template store related to the given type or creates a new one if
	 * there isn't a template store yet related to this type
	 *
	 * @param type
	 * @return template store
	 */
	public static TemplateStore getTemplateStore(String type) {
		if (!fStoreList.containsKey(type)) {
			ContributionTemplateStore store = new ContributionTemplateStore(getContextTypeRegistry(),
					CorrosionPlugin.getDefault().getPreferenceStore(),
					CorrosionPlugin.PLUGIN_ID + ".templates." + type); //$NON-NLS-1$
			try {
				store.load();
				fStoreList.put(type, store);
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}

		}

		return fStoreList.get(type);
	}

	/**
	 * Gives the context type registry of creates a new one if there isn't a context
	 * type registry created yet
	 *
	 * @return
	 */
	public static ContextTypeRegistry getContextTypeRegistry() {
		if (fRegistry == null) {
			fRegistry = new ContributionContextTypeRegistry();
			fRegistry.addContextType(CorrosionContextType.RUST_CONTEXT);
		}
		return fRegistry;
	}

	/**
	 * Checks if the given prefixing text to the cursor offset shows that the cursor
	 * is in a comment or not
	 *
	 * @param textToOffset
	 * @return if the offset would be within a comment
	 */
	public static boolean isOffestInARustComment(String textToOffset) {
		Matcher singleLineCommentMatcher = SINGLE_LINE_COMMENT_PATTERN.matcher(textToOffset);
		Matcher multiLineCommentMatcher = MULTI_LINE_COMMENT_PATTERN.matcher(textToOffset);
		return singleLineCommentMatcher.matches() || multiLineCommentMatcher.matches();
	}

	/**
	 * Converts the given template and information into a completion proposal
	 *
	 * @param template
	 * @param offset          user cursor offset
	 * @param info            LSP document info
	 * @param prefix          word preceding the cursor
	 * @param lineIndentation any whitespace to add to each new line of the
	 *                        insertion
	 * @return completion proposal that will insert the template
	 */
	public static ICompletionProposal convertTemplateToCompletionProposal(Template template, int offset,
			LSPDocumentInfo info, String prefix, String lineIndentation) {
		CompletionItem item = new CompletionItem();
		item.setLabel(template.getName());
		item.setKind(CompletionItemKind.Snippet);
		item.setInsertTextFormat(InsertTextFormat.Snippet);

		Range r = null;
		try {
			int line = info.getDocument().getLineOfOffset(offset);
			int lineOffset = offset - info.getDocument().getLineOffset(line);
			r = new Range(new Position(line, lineOffset - prefix.length()), new Position(line, lineOffset));
		} catch (BadLocationException e) {
			// Caught by null return
		}
		if (r == null) {
			return null;
		}
		item.setTextEdit(new TextEdit(r,
				convertPatternToInsertion(template.getPattern().replace("\n", "\n" + lineIndentation)))); //$NON-NLS-1$ //$NON-NLS-2$
		return new LSCompletionProposal(item, offset, info);
	}

	/**
	 * Converts a template's pattern to a formatted TextEdit's string. Converts and
	 * variables to the TextEdit format
	 *
	 * @param pattern
	 * @return pattern formatted to match TextEdit's newString format
	 */
	public static String convertPatternToInsertion(String pattern) {
		StringBuilder result = new StringBuilder();
		Map<String, Integer> tagIndex = new HashMap<>();
		int currentIndex = 0;
		int tagCount = 0;
		String tagPrefix = "${"; //$NON-NLS-1$
		while (currentIndex < pattern.length()) {
			int indexOfStart = pattern.indexOf(tagPrefix, currentIndex);
			if (indexOfStart == -1) {
				result.append(pattern.substring(currentIndex));
				return result.toString();
			}
			String prefix = pattern.substring(currentIndex, indexOfStart + tagPrefix.length());
			result.append(prefix);
			currentIndex = indexOfStart + tagPrefix.length();
			if (!prefix.matches(VALID_TAG_PREFIX)) {
				continue;
			}

			int indexOfClose = pattern.indexOf('}', currentIndex);
			if (indexOfClose == -1) {
				result.append(pattern.substring(currentIndex));
				return result.toString();
			}

			String tag = pattern.substring(currentIndex, indexOfClose);
			currentIndex = indexOfClose + 1;
			if (tag.equals(GlobalTemplateVariables.Cursor.NAME) || tag.isEmpty()) {
				result.append(tagCount);
				result.append('}');
				tagCount++;
				continue;
			}
			if (!tagIndex.containsKey(tag)) {
				tagIndex.put(tag, tagCount);
				tagCount++;
			}
			result.append(tagIndex.get(tag));
			result.append(':' + tag + '}');
		}
		return result.toString();
	}
}
