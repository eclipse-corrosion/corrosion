/*********************************************************************
 * Copyright (c) 2017, 2021 Red Hat Inc. and others.
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

import java.util.Collection;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.lsp4e.LanguageServerPlugin;
import org.eclipse.lsp4e.LanguageServiceAccessor;
import org.eclipse.lsp4e.LanguageServiceAccessor.LSPDocumentInfo;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.ITextEditor;

@SuppressWarnings("restriction")
public class Implementations extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart part = HandlerUtil.getActiveEditor(event);
		if (part instanceof ITextEditor editor) {
			Collection<LSPDocumentInfo> infos = LanguageServiceAccessor.getLSPDocumentInfosFor(
					LSPEclipseUtils.getDocument(editor),
					capabilities -> Boolean.TRUE.equals(capabilities.getReferencesProvider().get()));
			if (!infos.isEmpty()) {
				LSPDocumentInfo info = infos.iterator().next();
				ISelection sel = ((AbstractTextEditor) part).getSelectionProvider().getSelection();

				if (sel instanceof TextSelection selection) {
					try {
						int offset = selection.getOffset();
						ImplementationsSearchQuery query = new ImplementationsSearchQuery(offset, info);
						NewSearchUI.runQueryInBackground(query);
					} catch (BadLocationException e) {
						LanguageServerPlugin.logError(e);
					}
				}
			}
		}
		return null;
	}

	@Override
	public boolean isEnabled() {
		IWorkbenchPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();
		if (part instanceof ITextEditor) {
			Collection<LSPDocumentInfo> infos = LanguageServiceAccessor.getLSPDocumentInfosFor(
					LSPEclipseUtils.getDocument((ITextEditor) part),
					capabilities -> Boolean.TRUE.equals(capabilities.getReferencesProvider().get()));
			ISelection selection = ((ITextEditor) part).getSelectionProvider().getSelection();
			return !infos.isEmpty() && !selection.isEmpty() && selection instanceof ITextSelection;
		}
		return false;
	}

	@Override
	public boolean isHandled() {
		return true;
	}

}
