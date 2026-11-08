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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.corrosion.Messages;
import org.eclipse.corrosion.run.CargoRunDelegate;
import org.eclipse.corrosion.test.CargoTestDelegate;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.dialogs.ListDialog;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Asks whether to run the Cargo project, its tests, or the test at the cursor,
 * and then launches the selected one. In Rust, code and tests are in the same
 * file, thus "Run" alone cannot know which of them to launch.
 */
public class RunAsHandler extends AbstractHandler {

	private record Choice(String label, Runnable action) {
	}

	@Override
	public Object execute(ExecutionEvent event) {
		IWorkbenchPart part = HandlerUtil.getActivePart(event);
		IEditorPart editor = part instanceof IEditorPart editorPart ? editorPart : null;
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		IResource resource = editor != null ? RustLaunchDelegateTools.resourceFromEditor(editor)
				: RustLaunchDelegateTools.firstResourceFromSelection(selection);
		if (resource == null) {
			return null;
		}

		List<Choice> choices = new ArrayList<>();
		choices.add(new Choice(Messages.RunAsHandler_cargoRun, () -> launch(new CargoRunDelegate(), editor, selection)));
		choices.add(
				new Choice(Messages.RunAsHandler_cargoTest, () -> launch(new CargoTestDelegate(), editor, selection)));
		Choice initial = choices.get(0);
		Optional<TestAtCursor> test = editor instanceof ITextEditor textEditor ? TestAtCursor.find(textEditor)
				: Optional.empty();
		if (test.isPresent()) {
			initial = new Choice(NLS.bind(Messages.RunAsHandler_singleTest, test.get().name()),
					() -> launchSingleTest(resource, test.get()));
			choices.add(initial);
		}

		ListDialog dialog = new ListDialog(HandlerUtil.getActiveShell(event));
		dialog.setTitle(Messages.RunAsHandler_title);
		dialog.setMessage(NLS.bind(Messages.RunAsHandler_message, resource.getName()));
		dialog.setContentProvider(ArrayContentProvider.getInstance());
		dialog.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				return ((Choice) element).label();
			}

			@Override
			public Image getImage(Object element) {
				return CorrosionPlugin.getDefault().getImageRegistry().get("images/cargo16.png"); //$NON-NLS-1$
			}
		});
		dialog.setInput(choices);
		dialog.setInitialSelections(initial);
		dialog.setHelpAvailable(false);
		if (dialog.open() != Window.OK) {
			return null;
		}
		Object[] result = dialog.getResult();
		if (result != null && result.length > 0) {
			((Choice) result[0]).action().run();
		}
		return null;
	}

	private static void launch(ILaunchShortcut shortcut, IEditorPart editor, ISelection selection) {
		if (editor != null) {
			shortcut.launch(editor, ILaunchManager.RUN_MODE);
		} else {
			shortcut.launch(selection, ILaunchManager.RUN_MODE);
		}
	}

	private static void launchSingleTest(IResource resource, TestAtCursor test) {
		try {
			RustLaunchDelegateTools.launch(CargoTestDelegate.getSingleTestConfiguration(resource.getProject(), test),
					ILaunchManager.RUN_MODE);
		} catch (CoreException e) {
			CorrosionPlugin.logError(e);
		}
	}
}
