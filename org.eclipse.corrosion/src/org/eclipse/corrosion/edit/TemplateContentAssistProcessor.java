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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.corrosion.CorrosionPreferenceInitializer;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.eclipse.lsp4e.LanguageServiceAccessor;
import org.eclipse.lsp4e.LanguageServiceAccessor.LSPDocumentInfo;

@SuppressWarnings("restriction")
public class TemplateContentAssistProcessor implements IContentAssistProcessor {

	private static final String ENDS_WITH_WORD = "(?<indent>\\s*).*?(?<prefix>\\w*)"; //$NON-NLS-1$
	private static final Pattern ENDS_WITH_WORD_PATTERN = Pattern.compile(ENDS_WITH_WORD);

	@Override
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		if (!CorrosionPlugin.getDefault().getPreferenceStore()
				.getBoolean(CorrosionPreferenceInitializer.enableTemplates)) {
			return null;
		}
		IDocument document = viewer.getDocument();
		String textToOffset = document.get().substring(0, offset);

		Matcher matcher = ENDS_WITH_WORD_PATTERN.matcher(textToOffset.substring(textToOffset.lastIndexOf('\n') + 1));
		matcher.matches();
		String indent = matcher.group("indent"); //$NON-NLS-1$
		String prefix = matcher.group("prefix"); //$NON-NLS-1$

		Collection<LSPDocumentInfo> infos = LanguageServiceAccessor.getLSPDocumentInfosFor(document,
				capabilities -> Boolean.TRUE.equals(capabilities.getReferencesProvider()));

		TemplateStore store = TemplateHelper.getTemplateStore("rust"); //$NON-NLS-1$
		Template[] templates = store.getTemplates();
		List<ICompletionProposal> proposals = new ArrayList<>();
		for (Template template : templates) {
			if (template.getName().startsWith(prefix) && !TemplateHelper.isOffestInARustComment(textToOffset)) {
				proposals.add(TemplateHelper.convertTemplateToCompletionProposal(template, offset,
						infos.iterator().next(), prefix, indent));
			}
		}
		return proposals.toArray(new ICompletionProposal[proposals.size()]);
	}

	@Override
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
		return null;
	}

	@Override
	public char[] getCompletionProposalAutoActivationCharacters() {
		return null;
	}

	@Override
	public char[] getContextInformationAutoActivationCharacters() {
		return null;
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
