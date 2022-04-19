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

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

import org.eclipse.cdt.debug.core.ICDTLaunchConfigurationConstants;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.corrosion.launch.RustLaunchDelegateTools;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
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

	private static class FileLocation {
		public final String filePath;
		public final int lineNumber;
		public final int lineOffset;

		private FileLocation(String filePath, int lineNumber, int lineOffset) {
			this.filePath = filePath;
			this.lineNumber = lineNumber;
			this.lineOffset = lineOffset;
		}

		public static FileLocation readFromConsoleString(String consoleString) {
			int lineNumberLineOffsetSeparatorIndex = consoleString.lastIndexOf(':');
			int fileNameLineNumberSeparatorIndex = consoleString.lastIndexOf(':',
					lineNumberLineOffsetSeparatorIndex - 1);
			return new FileLocation(consoleString.substring(0, fileNameLineNumberSeparatorIndex), //
					Integer.parseInt(consoleString.substring(fileNameLineNumberSeparatorIndex + 1,
							lineNumberLineOffsetSeparatorIndex)), //
					Integer.parseInt(consoleString.substring(lineNumberLineOffsetSeparatorIndex + 1)));
		}
	}

	@Override
	public void matchFound(PatternMatchEvent event) {
		try {
			int offset = event.getOffset();
			int length = event.getLength();
			FileLocation entry = FileLocation
					.readFromConsoleString(console.getDocument().get(event.getOffset(), event.getLength()));
			console.addHyperlink(makeHyperlink(getProject(console), entry), offset, length);
		} catch (BadLocationException | CoreException e) {
			CorrosionPlugin.logError(e);
		}
	}

	private static IHyperlink makeHyperlink(IProject project, FileLocation location) {
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
				IEditorPart editorPart = null;
				Optional<IFile> ifile = toIFile(project, location.filePath);
				try {
					if (ifile.isPresent()) {
						editorPart = IDE.openEditor(page, ifile.get());
					} else {
						File file = new File(location.filePath);
						if (file.isFile()) {
							IDE.openInternalEditorOnFileStore(page, EFS.getStore(file.getAbsoluteFile().toURI()));
						}
					}
					if (editorPart != null) {
						jumpToPosition(editorPart, location.lineNumber, location.lineOffset);
					}
				} catch (CoreException ex) {
					CorrosionPlugin.logError(ex);
				}
			}

			private Optional<IFile> toIFile(IProject project, String filePath) {
				if (filePath == null) {
					return Optional.empty();
				}
				if (project != null && project.isAccessible()) {
					IFile file = project.getFile(Path.fromOSString(filePath));
					if (file.isAccessible()) {
						return Optional.of(file);
					}
				}
				IFile[] files = ResourcesPlugin.getWorkspace().getRoot()
						.findFilesForLocationURI(new File(filePath).getAbsoluteFile().toURI());
				return Arrays.stream(files) //
						.filter(IFile::isAccessible) //
						.min(Comparator.comparingInt(file -> file.getProjectRelativePath().segmentCount()));
			}
		};
	}

	private static void jumpToPosition(IEditorPart editorPart, int lineNumber, int lineOffset) {
		if (editorPart instanceof ITextEditor textEditor) {
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

	private static IProject getProject(TextConsole console) throws CoreException {
		IProcess process = (IProcess) console.getAttribute(IDebugUIConstants.ATTR_CONSOLE_PROCESS);
		if (process == null) {
			return null;
		}
		ILaunch launch = process.getLaunch();
		String projectAttribute = RustLaunchDelegateTools.PROJECT_ATTRIBUTE;
		String launchConfigurationType = launch.getLaunchConfiguration().getType().getIdentifier();
		if (launchConfigurationType.equals(RustLaunchDelegateTools.CORROSION_DEBUG_LAUNCH_CONFIG_TYPE)) {
			// support debug launch configs
			projectAttribute = ICDTLaunchConfigurationConstants.ATTR_PROJECT_NAME;
		}
		String projectName = launch.getLaunchConfiguration().getAttribute(projectAttribute, ""); //$NON-NLS-1$
		if (projectName.trim().isEmpty()) {
			return null; // can't determine project so prevent error down
		}
		IWorkspaceRoot myWorkspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		IProject myProject = myWorkspaceRoot.getProject(projectName);
		return myProject.isAccessible() ? myProject : null;
	}
}
