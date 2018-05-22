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
package org.eclipse.corrosion;

import org.eclipse.corrosion.wizards.newCargo.NewCargoProjectWizard;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.wizards.newresource.BasicNewFileResourceWizard;
import org.eclipse.ui.wizards.newresource.BasicNewFolderResourceWizard;

public class RustPerspective implements IPerspectiveFactory {
	public static final String ID = "org.eclipse.corrosion.rustPerspective"; //$NON-NLS-1$

	@Override public void createInitialLayout(IPageLayout layout) {
		addViewStructure(layout);

		addActionSets(layout);
		addShowViewShortcuts(layout);
		addNewWizardShortcuts(layout);
		addPerspectiveShotcuts(layout);
	}

	protected void addViewStructure(IPageLayout layout) {
		String editorArea = layout.getEditorArea();

		IFolderLayout leftFolder = layout.createFolder("leftPane", IPageLayout.LEFT, 0.25f, editorArea); //$NON-NLS-1$
		leftFolder.addView(IPageLayout.ID_PROJECT_EXPLORER);

		IFolderLayout bottomFolder = layout.createFolder("bottomPane", IPageLayout.BOTTOM, 0.75f, editorArea); //$NON-NLS-1$

		bottomFolder.addView(IPageLayout.ID_PROBLEM_VIEW);
		bottomFolder.addView(IPageLayout.ID_TASK_LIST);
		bottomFolder.addPlaceholder(NewSearchUI.SEARCH_VIEW_ID);
		bottomFolder.addView(IPageLayout.ID_PROGRESS_VIEW);
		bottomFolder.addView(IConsoleConstants.ID_CONSOLE_VIEW);

		// Create outline after bottom pane
		layout.addView(IPageLayout.ID_OUTLINE, IPageLayout.RIGHT, 0.75f, editorArea);
	}

	protected void addShowViewShortcuts(IPageLayout layout) {
		layout.addShowViewShortcut(IPageLayout.ID_OUTLINE);

		layout.addShowViewShortcut(IPageLayout.ID_PROBLEM_VIEW);
		layout.addShowViewShortcut(IPageLayout.ID_TASK_LIST);
		layout.addShowViewShortcut(NewSearchUI.SEARCH_VIEW_ID);
		layout.addShowViewShortcut(IPageLayout.ID_PROGRESS_VIEW);
		layout.addShowViewShortcut(IConsoleConstants.ID_CONSOLE_VIEW);
	}

	protected void addActionSets(IPageLayout layout) {
		layout.addActionSet(IPageLayout.ID_NAVIGATE_ACTION_SET);
		layout.addActionSet(IDebugUIConstants.LAUNCH_ACTION_SET);
	}

	protected void addNewWizardShortcuts(IPageLayout layout) {
		// Lang
		layout.addNewWizardShortcut(NewCargoProjectWizard.ID);

		// General
		layout.addNewWizardShortcut(BasicNewFolderResourceWizard.WIZARD_ID);
		layout.addNewWizardShortcut(BasicNewFileResourceWizard.WIZARD_ID);
	}

	protected void addPerspectiveShotcuts(IPageLayout layout) {
		layout.addPerspectiveShortcut("org.eclipse.debug.ui.DebugPerspective"); //$NON-NLS-1$
		layout.addPerspectiveShortcut("org.eclipse.ui.resourcePerspective"); //$NON-NLS-1$
		layout.addPerspectiveShortcut("org.eclipse.team.ui.TeamSynchronizingPerspective"); //$NON-NLS-1$
	}

}
