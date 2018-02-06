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
package org.eclipse.corrosion.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.corrosion.wizards.export.CargoExportWizard;
import org.eclipse.corrosion.wizards.export.CargoExportWizardPage;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.tests.harness.util.DisplayHelper;
import org.junit.Test;

public class TestExportCargoProjectWizard extends AbstractCorrosionTest {

	private WizardDialog dialog;
	private CargoExportWizard wizard;
	private Text projectText;
	private Label locationLabel;

	@Override
	public void setUp() throws Exception {
		super.setUp();
	}

	private void createWizard(String selectedProjectName) throws Exception {
		wizard = new CargoExportWizard();
		if (!selectedProjectName.isEmpty()) {
			IProject project = getProject(selectedProjectName);
			wizard.init(getWorkbench(), new StructuredSelection(project));
		} else {
			wizard.init(getWorkbench(), new StructuredSelection());
		}
		dialog = new WizardDialog(getShell(), wizard);
		dialog.create();
		dialog.setBlockOnOpen(false);
		dialog.open();

		Composite composite = (Composite) wizard.getPages()[0].getControl();
		projectText = (Text) composite.getChildren()[1];
		locationLabel = (Label) composite.getChildren()[4];
	}

	@Test
	public void testExportPageWithSelection() throws Exception {
		createWizard("basic");
		confirmPageState(getProject("basic").getName(), true);

		projectText.setText("");
		confirmPageState(null, false);
	}

	@Test
	public void testExportPageWithChangingProject() throws Exception {
		createWizard("basic");
		confirmPageState(getProject("basic").getName(), true);

		IProject project = getProject("basic_errors");
		projectText.setText(project.getName());
		confirmPageState(project.getName(), true);
	}

	@Test
	public void testExportPageWithNonCargoProject() throws Exception {
		createWizard("basic");
		confirmPageState(getProject("basic").getName(), true);

		IProject project = getProject("not_cargo");
		projectText.setText(project.getName());
		confirmPageState(null, false);
	}

	private void confirmPageState(String expectedProjectName, Boolean expectedFinishState) {
		CargoExportWizardPage page = (CargoExportWizardPage) wizard.getPages()[0];
		if (!expectedFinishState) {
			assertFalse(wizard.canFinish());
			assertEquals(locationLabel.getText(), "");
		} else {
			assertEquals(expectedProjectName, page.getProject().getName());
			assertEquals(locationLabel.getText(),
					"Crate will be created in: " + expectedProjectName + "/target/package/");
		}
	}

	@Test
	public void testExportProject() throws Exception {
		IProject basic = getProject("basic");
		createWizard("basic");
		Composite composite = (Composite) wizard.getPages()[0].getControl();
		Button allowDirty = (Button) composite.getChildren()[10];
		allowDirty.setSelection(true); // required of another test updates the project
		new DisplayHelper() {
			@Override
			protected boolean condition() {
				return allowDirty.getSelection();
			}
		}.waitForCondition(getShell().getDisplay(), 3000);

		assertTrue(wizard.canFinish());
		assertTrue(wizard.performFinish());
		new DisplayHelper() {

			@Override
			protected boolean condition() {
				return basic.getFolder("target").getFolder("package").exists();
			}
		}.waitForCondition(getShell().getDisplay(), 15000);
		assertTrue(basic.getFolder("target").getFolder("package").members().length > 0);
	}

	@Override
	public void tearDown() throws CoreException {
		if (dialog != null) {
			dialog.close();
		}
		super.tearDown();
	}
}
