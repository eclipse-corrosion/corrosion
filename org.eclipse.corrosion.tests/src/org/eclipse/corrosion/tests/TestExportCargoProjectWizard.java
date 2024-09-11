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
 *  Lucas Bullen   (Red Hat Inc.) - Initial implementation
 *******************************************************************************/
package org.eclipse.corrosion.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Duration;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.corrosion.wizards.export.CargoExportWizard;
import org.eclipse.corrosion.wizards.export.CargoExportWizardPage;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.junit.jupiter.api.Test;

class TestExportCargoProjectWizard extends AbstractCorrosionTest {

	private WizardDialog dialog;
	private CargoExportWizard wizard;
	private Text projectText;
	private Label locationLabel;

	private void createWizard(String selectedProjectName) throws IOException, CoreException {
		wizard = new CargoExportWizard();
		if (!selectedProjectName.isEmpty()) {
			IProject project = getProject(selectedProjectName);
			wizard.init(PlatformUI.getWorkbench(), new StructuredSelection(project));
		} else {
			wizard.init(PlatformUI.getWorkbench(), StructuredSelection.EMPTY);
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
	void testExportPageWithSelection() throws IOException, CoreException {
		createWizard(BASIC_PROJECT_NAME);
		confirmPageState(getProject(BASIC_PROJECT_NAME).getName(), true);

		projectText.setText("");
		confirmPageState(null, false);
	}

	@Test
	void testExportPageWithChangingProject() throws IOException, CoreException {
		createWizard(BASIC_PROJECT_NAME);
		confirmPageState(getProject(BASIC_PROJECT_NAME).getName(), true);

		IProject project = getProject(BASIC_ERRORS_PROJECT_NAME);
		projectText.setText(project.getName());
		confirmPageState(project.getName(), true);
	}

	@Test
	void testExportPageWithNonCargoProject() throws IOException, CoreException {
		createWizard(BASIC_PROJECT_NAME);
		confirmPageState(getProject(BASIC_PROJECT_NAME).getName(), true);

		IProject project = getProject(NOT_CARGO_PROJECT_NAME);
		projectText.setText(project.getName());
		confirmPageState(null, false);
	}

	private void confirmPageState(String expectedProjectName, Boolean expectedFinishState) {
		CargoExportWizardPage page = (CargoExportWizardPage) wizard.getPages()[0];
		if (!expectedFinishState) {
			assertFalse(wizard.canFinish());
			assertTrue(locationLabel.getText().isEmpty());
		} else {
			assertEquals(expectedProjectName, page.getProject().getName());
			assertEquals(locationLabel.getText(),
					"Crate will be created in: " + expectedProjectName + "/target/package/");
		}
	}

	@Test
	void testExportProject() throws IOException, CoreException {
		IProject basic = getProject(BASIC_PROJECT_NAME);
		createWizard(BASIC_PROJECT_NAME);
		Composite composite = (Composite) wizard.getPages()[0].getControl();
		Button allowDirty = (Button) composite.getChildren()[10];
		allowDirty.setSelection(true); // required of another test updates the project
		waitUntil(getShell().getDisplay(), Duration.ofSeconds(3), allowDirty::getSelection);
		assertTrue(wizard.canFinish());
		assertTrue(wizard.performFinish());
		waitUntil(getShell().getDisplay(), Duration.ofSeconds(15),
				() -> basic.getFolder("target").getFolder("package").exists());
		assertTrue(basic.getFolder("target").getFolder("package").members().length > 0);
	}

	@Override
	public void tearDown() throws CoreException, IOException {
		if (dialog != null) {
			dialog.close();
		}
		super.tearDown();
	}
}
