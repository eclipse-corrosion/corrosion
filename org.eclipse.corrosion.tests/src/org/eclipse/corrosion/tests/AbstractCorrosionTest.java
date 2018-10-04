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
 *  Mickael Istria (Red Hat Inc.) - Source Reference
 *  Lucas Bullen   (Red Hat Inc.) - Initial implementation
 *******************************************************************************/
package org.eclipse.corrosion.tests;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.junit.After;
import org.junit.Before;

/**
 * Takes care of creating a temporary project and resource before test and to
 * clean it up after.
 */
public class AbstractCorrosionTest {
	protected static final String BASIC_PROJECT_NAME = "basic";
	protected static final String BASIC_ERRORS_PROJECT_NAME = "basic_errors";
	protected static final String NOT_CARGO_PROJECT_NAME = "not_cargo";

	private Map<String, IProject> provisionedProjects;

	@Before
	public void setUp() {
		this.provisionedProjects = new HashMap<>();
	}

	/**
	 *
	 * @param projectName
	 *            the name that will be used as prefix for the project, and that
	 *            will be used to find the content of the project from the plugin
	 *            "projects" folder
	 * @throws IOException
	 * @throws CoreException
	 */
	protected IProject provisionProject(String projectName) throws IOException, CoreException {
		URL url = FileLocator.find(Platform.getBundle("org.eclipse.corrosion.tests"),
				Path.fromPortableString("projects/" + projectName), Collections.emptyMap());
		url = FileLocator.toFileURL(url);
		File folder = new File(url.getFile());
		if (folder.exists()) {
			IProject project = ResourcesPlugin.getWorkspace().getRoot()
					.getProject(projectName + "_" + getClass().getName() + "_" + System.currentTimeMillis());
			project.create(new NullProgressMonitor());
			this.provisionedProjects.put(projectName, project);
			FileUtils.copyDirectory(folder, project.getLocation().toFile());
			project.open(new NullProgressMonitor());
			project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
			return project;
		} 			
		return null;
	}

	@After
	public void tearDown() throws CoreException {
		for (String projectName : this.provisionedProjects.keySet()) {
			try {
				getProject(projectName).delete(true, new NullProgressMonitor());
			} catch (CoreException | IOException e) {
				fail(e.getMessage());
			}
		}
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().closeAllEditors(false);
	}

	/**
	 * @param projectPrefix
	 *            the prefix of the project, as it can be found in plugin's
	 *            "projects" folder
	 * @return a project with the content from the specified projectPrefix
	 * @throws CoreException
	 * @throws IOException
	 */
	protected IProject getProject(String projectPrefix) throws IOException, CoreException {
		if (!this.provisionedProjects.containsKey(projectPrefix)) {
			provisionProject(projectPrefix);
		}
		return this.provisionedProjects.get(projectPrefix);
	}

	protected Shell getShell() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
	}

	protected IWorkbench getWorkbench() {
		return PlatformUI.getWorkbench();
	}
}
