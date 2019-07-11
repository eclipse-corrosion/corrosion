/*********************************************************************
 * Copyright (c) 2019 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Alexander Kurtakov (Red Hat Inc.) - Initial implementation
 *******************************************************************************/
package org.eclipse.corrosion;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.corrosion.launch.RustLaunchDelegateTools;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IHyperlink;
import org.eclipse.ui.console.IPatternMatchListenerDelegate;
import org.eclipse.ui.console.PatternMatchEvent;
import org.eclipse.ui.console.TextConsole;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;

public class ErrorLineMatcher implements IPatternMatchListenerDelegate {

	private TextConsole console;

	@Override
	public void connect(TextConsole console) {
		this.console = console;
	}

	@Override
	public void disconnect() {
		console = null;
	}

	@Override
	public void matchFound(PatternMatchEvent event) {
		try {
			int offset = event.getOffset();
			int length = event.getLength();
			IProcess process = (IProcess) console.getAttribute(IDebugUIConstants.ATTR_CONSOLE_PROCESS);
			if (process != null) {
				ILaunch launch = process.getLaunch();
				String projectName = launch.getLaunchConfiguration()
						.getAttribute(RustLaunchDelegateTools.PROJECT_ATTRIBUTE, ""); //$NON-NLS-1$
				IWorkspaceRoot myWorkspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
				IProject myProject = myWorkspaceRoot.getProject(projectName);
				String errorString = console.getDocument().get(event.getOffset(), event.getLength());
				String[] coordinates = errorString.split(":"); //$NON-NLS-1$
				IHyperlink link = makeHyperlink(myProject.getFile(coordinates[0]), Integer.parseInt(coordinates[1]),
						Integer.parseInt(coordinates[2]));
				console.addHyperlink(link, offset, length);
			}
		} catch (BadLocationException | CoreException e) {
			// ignore
		}
	}

	private static IHyperlink makeHyperlink(IFile file, int lineNumber, int lineOffset) {
		return new IHyperlink() {
			@Override
			public void linkExited() {
				// ignore
			}

			@Override
			public void linkEntered() {
				// ignore
			}

			@Override
			public void linkActivated() {
				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				try {
					IEditorPart editorPart = IDE.openEditor(page, file);
					jumpToPosition(editorPart, lineNumber, lineOffset);
				} catch (PartInitException e) {
					// nothing to do here
				}
			}
		};
	}

	private static void jumpToPosition(IEditorPart editorPart, int lineNumber, int lineOffset) {
		if (editorPart instanceof ITextEditor) {
			ITextEditor textEditor = (ITextEditor) editorPart;
			IDocument document = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());

			if (document != null) {
				IRegion region = null;
				try {
					region = document.getLineInformation(lineNumber - 1);
				} catch (BadLocationException exception) {
					// ignore
				}

				if (region != null)
					textEditor.selectAndReveal(region.getOffset() + lineOffset - 1, 0);
			}
		}
	}
}
