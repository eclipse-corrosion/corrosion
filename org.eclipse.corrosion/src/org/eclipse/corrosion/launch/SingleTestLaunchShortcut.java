/*********************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.corrosion.launch;

import java.util.Optional;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.corrosion.Messages;
import org.eclipse.corrosion.test.CargoTestDelegate;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.ui.ILaunchShortcut2;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Runs or debugs the test function at the cursor of a Rust editor. rust-analyzer
 * gives the test name and the Cargo arguments. Each test has its own saved
 * launch configuration, so that "Run" (Ctrl+F11) and "Debug" (F11) can launch
 * it again.
 */
public class SingleTestLaunchShortcut implements ILaunchShortcut2 {

	@Override
	public void launch(IEditorPart editor, String mode) {
		IResource resource = RustLaunchDelegateTools.resourceFromEditor(editor);
		Optional<TestAtCursor> test = editor instanceof ITextEditor textEditor ? TestAtCursor.find(textEditor)
				: Optional.empty();
		if (resource == null || test.isEmpty()) {
			MessageDialog.openInformation(Display.getCurrent().getActiveShell(), Messages.SingleTestLaunchShortcut_title,
					Messages.SingleTestLaunchShortcut_noTestAtCursor);
			return;
		}
		try {
			RustLaunchDelegateTools.launch(
					CargoTestDelegate.getSingleTestConfiguration(resource.getProject(), test.get()), mode);
		} catch (CoreException e) {
			CorrosionPlugin.logError(e);
		}
	}

	@Override
	public void launch(ISelection selection, String mode) {
		// A selection in a view has no cursor, thus no test
		MessageDialog.openInformation(Display.getCurrent().getActiveShell(), Messages.SingleTestLaunchShortcut_title,
				Messages.SingleTestLaunchShortcut_noTestAtCursor);
	}

	/**
	 * Gives no configurations, so that "Run" (Ctrl+F11) does not ask rust-analyzer
	 * for the test at the cursor. The configurations of single tests are Cargo
	 * test configurations, and the Cargo test shortcut gives them.
	 */
	@Override
	public ILaunchConfiguration[] getLaunchConfigurations(ISelection selection) {
		return new ILaunchConfiguration[0];
	}

	@Override
	public ILaunchConfiguration[] getLaunchConfigurations(IEditorPart editor) {
		return new ILaunchConfiguration[0];
	}

	@Override
	public IResource getLaunchableResource(ISelection selection) {
		return RustLaunchDelegateTools.firstResourceFromSelection(selection);
	}

	@Override
	public IResource getLaunchableResource(IEditorPart editor) {
		return RustLaunchDelegateTools.resourceFromEditor(editor);
	}
}
