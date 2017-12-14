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
 *  Lucas Bullen   (Red Hat Inc.) - Initial implementation
 *******************************************************************************/
package org.eclipse.redox.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.redox.wizards.newCargo.NewCargoProjectWizard;
import org.eclipse.redox.wizards.newCargo.NewCargoProjectWizardPage;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.tests.harness.util.DisplayHelper;
import org.junit.Test;

public class TestNewCargoProjectWizard extends AbstractRedoxTest {

	@Test
	public void testNewProjectPage() throws Exception {
		NewCargoProjectWizard wizard = new NewCargoProjectWizard();
		WizardDialog dialog = new WizardDialog(getShell(), wizard);
		wizard.init(getWorkbench(), new StructuredSelection());
		dialog.create();
		confirmPageState(wizard, "new_rust_project", "none", true);

		Composite composite = (Composite) wizard.getPages()[0].getControl();
		Button binaryCheckBox = (Button) composite.getChildren()[12];
		binaryCheckBox.setSelection(false);
		confirmPageState(wizard, "new_rust_project", "none", false);

		Button vcsCheckBox = (Button) composite.getChildren()[14];
		vcsCheckBox.setSelection(true);
		confirmPageState(wizard, "new_rust_project", "git", false);

		dialog.close();
	}

	private void confirmPageState(IWizard wizard, String expectedProjectName, String expectedVCS,
			Boolean expectedBinaryState) {
		NewCargoProjectWizardPage page = (NewCargoProjectWizardPage) wizard.getPages()[0];
		assertEquals(expectedProjectName, page.getProjectName());
		assertEquals(expectedVCS, page.getVCS());
		assertEquals(expectedBinaryState, page.isBinaryTemplate());
	}

	@Test
	public void testCreateNewProject() throws Exception {
		NewCargoProjectWizard wizard = new NewCargoProjectWizard();
		WizardDialog dialog = new WizardDialog(getShell(), wizard);
		wizard.init(getWorkbench(), new StructuredSelection());
		dialog.create();

		assertTrue(wizard.canFinish());
		assertTrue(wizard.performFinish());
		dialog.close();
		new DisplayHelper() {

			@Override
			protected boolean condition() {
				return ResourcesPlugin.getWorkspace().getRoot().getProjects().length > 0;
			}
		}.waitForCondition(getShell().getDisplay(), 15000);
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		assertEquals(1, projects.length);
		assertTrue(projects[0].getFile("Cargo.toml").exists());
	}
}
