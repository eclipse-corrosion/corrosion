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
package org.eclipse.corrosion.debug;

import java.util.Collections;
import java.util.Set;

import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.corrosion.Messages;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTargetFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

public class ToggleBreakpointsTargetFactory implements IToggleBreakpointsTargetFactory {
	public static final String FACTORY_ID = CorrosionPlugin.PLUGIN_ID + "BreakpointFactory"; //$NON-NLS-1$

	@Override public Set<String> getToggleTargets(IWorkbenchPart part, ISelection selection) {
		return isRustPart(part) ? Collections.singleton(FACTORY_ID) : Collections.emptySet();
	}

	@Override public String getDefaultToggleTarget(IWorkbenchPart part, ISelection selection) {
		return isRustPart(part) ? FACTORY_ID : null;
	}

	private boolean isRustPart(IWorkbenchPart part) {
		if (part instanceof ITextEditor) {
			IEditorInput editorInput = ((ITextEditor) part).getEditorInput();
			return editorInput instanceof FileEditorInput && "rs".equals(((FileEditorInput) editorInput).getPath().getFileExtension()); //$NON-NLS-1$
		}
		return false;
	}

	@Override public IToggleBreakpointsTarget createToggleTarget(String targetID) {
		if (FACTORY_ID.equals(targetID)) {
			return new ToggleBreakpointAdapter();
		}
		return null;
	}

	@Override public String getToggleTargetName(String targetID) {
		return Messages.ToggleBreakpointsTargetFactory_breakpoint;
	}

	@Override public String getToggleTargetDescription(String targetID) {
		return Messages.ToggleBreakpointsTargetFactory_breakpointTarget;
	}

}
