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
package org.eclipse.corrosion.tests;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.corrosion.RustPerspective;
import org.eclipse.corrosion.wizards.newCargo.NewCargoProjectWizard;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.wizards.newresource.BasicNewFileResourceWizard;
import org.eclipse.ui.wizards.newresource.BasicNewFolderResourceWizard;
import org.junit.Before;
import org.junit.Test;

public class TestPerspective {

	@Before
	public void setup() {
		IPerspectiveDescriptor descriptor = PlatformUI.getWorkbench().getPerspectiveRegistry()
				.findPerspectiveWithId(RustPerspective.ID);

		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().setPerspective(descriptor);
	}

	@Test
	public void testNewWizardShortCuts() {
		String[] newWizardShortcutIds = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getNewWizardShortcuts();
		String[] expectedIds = new String[] { NewCargoProjectWizard.ID, BasicNewFolderResourceWizard.WIZARD_ID,
				BasicNewFileResourceWizard.WIZARD_ID };
		assertArrayEquals(expectedIds, newWizardShortcutIds);
	}

	@Test
	public void testPerspectiveShortcut() {
		String[] perspectiveShortcutIds = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getPerspectiveShortcuts();
		String[] expectedIds = new String[] { "org.eclipse.debug.ui.DebugPerspective",
				"org.eclipse.ui.resourcePerspective", "org.eclipse.team.ui.TeamSynchronizingPerspective" };
		assertArrayEquals(expectedIds, perspectiveShortcutIds);
	}

	@Test
	public void testShowViewShortcuts() {
		String[] showViewShortcutIds = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getShowViewShortcuts();
		String[] expectedIds = new String[] { IPageLayout.ID_OUTLINE, IPageLayout.ID_PROBLEM_VIEW,
				IPageLayout.ID_TASK_LIST, NewSearchUI.SEARCH_VIEW_ID, IPageLayout.ID_PROGRESS_VIEW,
				IConsoleConstants.ID_CONSOLE_VIEW };
		assertArrayEquals(expectedIds, showViewShortcutIds);
	}

	@Test
	public void testAddedViews() {
		List<String> expectedViewIds = new ArrayList<>();
		expectedViewIds.add(IPageLayout.ID_PROJECT_EXPLORER);
		expectedViewIds.add(IPageLayout.ID_PROBLEM_VIEW);
		expectedViewIds.add(IPageLayout.ID_TASK_LIST);
		expectedViewIds.add(IPageLayout.ID_PROGRESS_VIEW);
		expectedViewIds.add(IConsoleConstants.ID_CONSOLE_VIEW);
		IViewReference[] viewReferences = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getViewReferences();
		for (IViewReference viewReference : viewReferences) {
			expectedViewIds.remove(viewReference.getId());
		}
		assertEquals("Not all views present. Missing views: " + String.join(", ", expectedViewIds), 0,
				expectedViewIds.size());
	}
}
