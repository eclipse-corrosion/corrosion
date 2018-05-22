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
 *  Lucas Bullen (Red Hat Inc.) - Initial implementation
 *******************************************************************************/
package org.eclipse.corrosion.extensions;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.corrosion.Messages;
import org.eclipse.corrosion.edit.RLSServerInterface;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.lsp4e.LanguageServerPlugin;
import org.eclipse.lsp4e.LanguageServiceAccessor.LSPDocumentInfo;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.ReferenceContext;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.osgi.util.NLS;
import org.eclipse.search.internal.ui.text.FileMatch;
import org.eclipse.search.internal.ui.text.FileSearchQuery;
import org.eclipse.search.internal.ui.text.FileSearchResult;
import org.eclipse.search.internal.ui.text.LineElement;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.Match;

//TODO: investigate implementation without high reliance on restrictions
@SuppressWarnings("restriction")
public class ImplementationsSearchQuery extends FileSearchQuery {

	private final Position position;
	private final LSPDocumentInfo info;
	private final String filename;

	private FileSearchResult result;

	private long startTime;

	private CompletableFuture<List<? extends Location>> references;

	public ImplementationsSearchQuery(int offset, LSPDocumentInfo info) throws BadLocationException {
		super("", false, false, null); //$NON-NLS-1$
		this.position = LSPEclipseUtils.toPosition(offset, info.getDocument());
		this.info = info;
		IResource resource = LSPEclipseUtils.findResourceFor(info.getFileUri().toString());
		this.filename = resource != null ? resource.getName() : info.getFileUri().toString();
	}

	@Override public IStatus run(IProgressMonitor monitor) throws OperationCanceledException {
		startTime = System.currentTimeMillis();
		// Cancel last references future if needed.
		if (references != null) {
			references.cancel(true);
		}
		AbstractTextSearchResult textResult = (AbstractTextSearchResult) getSearchResult();
		textResult.removeAll();

		try {
			// Execute LSP "references" service
			ReferenceParams params = new ReferenceParams();
			params.setContext(new ReferenceContext(true));
			params.setTextDocument(new TextDocumentIdentifier(info.getFileUri().toString()));
			params.setPosition(position);
			info.getInitializedLanguageClient().thenCompose(languageServer -> ((RLSServerInterface) languageServer).implementations(params)).thenAccept(locs -> {
				// Loop for each LSP Location and convert it to Match search.
				for (Location loc : locs) {
					Match match = toMatch(loc);
					result.addMatch(match);
				}
			});
			return Status.OK_STATUS;
		} catch (Exception ex) {
			return new Status(IStatus.ERROR, LanguageServerPlugin.getDefault().getBundle().getSymbolicName(), ex.getMessage(), ex);
		}
	}

	/**
	 * Convert the given LSP {@link Location} to Eclipse search {@link Match}.
	 *
	 * @param location
	 *            the LSP location to convert.
	 * @return the converted Eclipse search {@link Match}.
	 */
	private static Match toMatch(Location location) {
		try {
			IResource resource = LSPEclipseUtils.findResourceFor(location.getUri());
			IDocument document = LSPEclipseUtils.getDocument(resource);
			if (document != null) {
				int startOffset = LSPEclipseUtils.toOffset(location.getRange().getStart(), document);
				int endOffset = LSPEclipseUtils.toOffset(location.getRange().getEnd(), document);

				IRegion lineInformation = document.getLineInformationOfOffset(startOffset);
				LineElement lineEntry = new LineElement(resource, document.getLineOfOffset(startOffset), lineInformation.getOffset(), document.get(lineInformation.getOffset(), lineInformation.getLength()));
				return new FileMatch((IFile) resource, startOffset, endOffset - startOffset, lineEntry);

			}
			Position startPosition = location.getRange().getStart();
			LineElement lineEntry = new LineElement(resource, startPosition.getLine(), 0, String.format("%s:%s", startPosition.getLine(), startPosition.getCharacter())); //$NON-NLS-1$
			return new FileMatch((IFile) resource, 0, 0, lineEntry);
		} catch (BadLocationException ex) {
			LanguageServerPlugin.logError(ex);
		}
		return null;
	}

	@Override public ISearchResult getSearchResult() {
		if (result == null) {
			result = new FileSearchResult(this);
		}
		return result;
	}

	@Override public String getLabel() {
		return Messages.ImplementationsSearchQuery_implementations;
	}

	@Override public String getResultLabel(int nMatches) {
		long time = 0;
		if (startTime > 0) {
			time = System.currentTimeMillis() - startTime;
		}
		if (nMatches == 1) {
			return NLS.bind(Messages.ImplementationsSearchQuery_oneReference, new Object[] { filename, position.getLine() + 1, position.getCharacter() + 1, time });
		}
		return NLS.bind(Messages.ImplementationsSearchQuery_severalReferences, new Object[] { filename, position.getLine() + 1, position.getCharacter() + 1, nMatches, time });
	}

	@Override public boolean isFileNameSearch() {
		// Return false to display lines where references are found
		return false;
	}
}
