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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.corrosion.wizards.newproject.NewCargoProjectWizard;
import org.eclipse.corrosion.wizards.newproject.NewCargoProjectWizardPage;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.junit.jupiter.api.Test;

class TestNewCargoProjectWizard extends AbstractCorrosionTest {

	private static final String DEFAULT_PROJECT_NAME = "new_rust_project";

	@Test
	void testNewProjectPage() {
		NewCargoProjectWizard wizard = new NewCargoProjectWizard();
		WizardDialog dialog = new WizardDialog(getShell(), wizard);
		wizard.init(PlatformUI.getWorkbench(), StructuredSelection.EMPTY);
		dialog.create();
		confirmPageState(wizard, DEFAULT_PROJECT_NAME, "none", true);

		Composite composite = (Composite) wizard.getPages()[0].getControl();
		Button binaryCheckBox = (Button) composite.getChildren()[12];
		binaryCheckBox.setSelection(false);
		confirmPageState(wizard, DEFAULT_PROJECT_NAME, "none", false);

		Button vcsCheckBox = (Button) composite.getChildren()[14];
		vcsCheckBox.setSelection(true);
		confirmPageState(wizard, DEFAULT_PROJECT_NAME, "git", false);

		dialog.close();
	}

	private static void confirmPageState(IWizard wizard, String expectedProjectName, String expectedVCS,
			Boolean expectedBinaryState) {
		NewCargoProjectWizardPage page = (NewCargoProjectWizardPage) wizard.getPages()[0];
		assertEquals(expectedProjectName, page.getProjectName());
		assertEquals(expectedVCS, page.getVCS());
		assertEquals(expectedBinaryState, page.isBinaryTemplate());
	}

	@Test
	void testCreateNewProject() {
		Collection<IProject> initialProjects = Arrays.asList(ResourcesPlugin.getWorkspace().getRoot().getProjects());
		NewCargoProjectWizard wizard = new NewCargoProjectWizard();
		WizardDialog dialog = new WizardDialog(getShell(), wizard);
		wizard.init(PlatformUI.getWorkbench(), StructuredSelection.EMPTY);
		dialog.create();

		assertTrue(wizard.canFinish());
		assertTrue(wizard.performFinish());
		dialog.close();
		waitUntil(getShell().getDisplay(), Duration.ofSeconds(15),
				() -> ResourcesPlugin.getWorkspace().getRoot().getProjects().length > 0);
		Set<IProject> newProjects = new HashSet<>(
				Arrays.asList(ResourcesPlugin.getWorkspace().getRoot().getProjects()));
		newProjects.removeAll(initialProjects);
		assertEquals(1, newProjects.size());
		assertTrue(newProjects.iterator().next().getFile("Cargo.toml").exists());
	}

	@Test
	void testCreateNewProjectOutOfWorkspace() throws IOException {
		Collection<IProject> initialProjects = Arrays.asList(ResourcesPlugin.getWorkspace().getRoot().getProjects());
		NewCargoProjectWizard wizard = new NewCargoProjectWizard();
		WizardDialog dialog = new WizardDialog(getShell(), wizard);
		wizard.init(PlatformUI.getWorkbench(), StructuredSelection.EMPTY);
		dialog.create();
		confirmPageState(wizard, DEFAULT_PROJECT_NAME, "none", true);
		Composite composite = (Composite) wizard.getPages()[0].getControl();
		Optional<Text> locationText = Arrays.stream(composite.getChildren()).filter(Text.class::isInstance)
				.map(Text.class::cast).findFirst();
		Path tempDir = Files.createTempDirectory("corrosion-test");
		locationText.get().setText(tempDir.toString());
		Path fileName = tempDir.getFileName();
		assertNotNull(fileName);
		confirmPageState(wizard, fileName.toString(), "none", true);
		assertTrue(wizard.canFinish());
		assertTrue(wizard.performFinish());
		dialog.close();
		waitUntil(getShell().getDisplay(), Duration.ofSeconds(15),
				() -> ResourcesPlugin.getWorkspace().getRoot().getProjects().length > 0);
		try {
			Set<IProject> newProjects = new HashSet<>(
					Arrays.asList(ResourcesPlugin.getWorkspace().getRoot().getProjects()));
			newProjects.removeAll(initialProjects);
			assertEquals(1, newProjects.size());
			IProject project = newProjects.iterator().next();
			assertEquals(tempDir.toFile(), project.getLocation().toFile());
			assertTrue(project.getFile("Cargo.toml").exists());
		} finally {
			FileUtils.deleteDirectory(tempDir.toFile());
		}
	}

	@Override
	public void tearDown() throws CoreException, IOException {
		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
			if (!project.getName().equals(".cargo")) {
				project.delete(true, new NullProgressMonitor());
			}
		}
		super.tearDown();
	}
}
