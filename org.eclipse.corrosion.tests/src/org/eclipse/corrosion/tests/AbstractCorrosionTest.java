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
 *  Mickael Istria (Red Hat Inc.) - Source Reference
 *  Lucas Bullen   (Red Hat Inc.) - Initial implementation
 *******************************************************************************/
package org.eclipse.corrosion.tests;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.corrosion.CorrosionPreferenceInitializer;
import org.eclipse.corrosion.RustManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.opentest4j.AssertionFailedError;

/**
 * Takes care of creating a temporary project and resource before test and to
 * clean it up after.
 */
public abstract class AbstractCorrosionTest {
	protected static final String BASIC_PROJECT_NAME = "basic";
	protected static final String BASIC_ERRORS_PROJECT_NAME = "basic_errors";
	protected static final String NOT_CARGO_PROJECT_NAME = "not_cargo";

	private Map<String, IProject> provisionedProjects;

	@BeforeEach
	public void setUp() {
		this.provisionedProjects = new HashMap<>();
		setupRustAnalyzerExecutable();
	}

	/**
	 *
	 * @param projectName the name that will be used as prefix for the project, and
	 *                    that will be used to find the content of the project from
	 *                    the plugin "projects" folder
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

	@AfterEach
	public void tearDown() throws CoreException, IOException {
		for (String projectName : this.provisionedProjects.keySet()) {
			getProject(projectName).delete(true, new NullProgressMonitor());
		}
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().closeAllEditors(false);
	}

	/**
	 * @param projectPrefix the prefix of the project, as it can be found in
	 *                      plugin's "projects" folder
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

	protected void setupRustAnalyzerExecutable() {
		IPreferenceStore store = CorrosionPlugin.getDefault().getPreferenceStore();

		// Download Rust Analyzer if it'n not installed yet
		if (getRustAnalyzerExecutable(store) == null) {
			try {
				RustManager.downloadAndInstallRustAnalyzer(progress -> {
				}).thenAccept(file -> {
					String rustAnalyzerPath = file.getAbsolutePath();

					// CorrosionPreferencePage sets up the following preference value in `performOK`
					// method, then the preference value is used to get the rust-analyzer executable
					// when needed
					//
					store.setValue(CorrosionPreferenceInitializer.RLS_PATH_PREFERENCE, rustAnalyzerPath);
				}).exceptionally(ex -> {
					fail(ex);
					return null;
				}).get(30, TimeUnit.SECONDS);
			} catch (InterruptedException | ExecutionException | TimeoutException e) {
				fail("Rust Analyzer executable setup failed", e);
			}

			assertNotNull(getRustAnalyzerExecutable(store), "Rust Analyzer executable setup failed");
		}
	}

	protected File getRustAnalyzerExecutable(IPreferenceStore store) {
		String rustAnalyzerExecutablePath = store.getString(CorrosionPreferenceInitializer.RLS_PATH_PREFERENCE);
		File rustAnalyzerExecutable = new File(rustAnalyzerExecutablePath != null ? rustAnalyzerExecutablePath : "");

		return (rustAnalyzerExecutable.exists() && rustAnalyzerExecutable.isFile()
				&& rustAnalyzerExecutable.canExecute() ? rustAnalyzerExecutable : null);
	}

	protected static void waitUntil(Display display, Duration duration, BooleanSupplier condition)
			throws AssertionFailedError {
		Instant timeout = Instant.now().plus(duration);
		Duration interval = Duration.ofMillis(200);
		do {
			boolean[] conditionMet = new boolean[] { false };
			if (Display.getCurrent() != display) {
				display.syncExec(() -> conditionMet[0] = condition.getAsBoolean());
			} else {
				conditionMet[0] = condition.getAsBoolean();
			}
			if (conditionMet[0]) {
				return;
			}
			if (Display.getCurrent() != display) {
				try {
					Thread.sleep(interval.toMillis());
				} catch (InterruptedException e) {
					//
				}
			} else {
				Instant endOfInterval = Instant.now().plus(interval);
				do {
					display.readAndDispatch();
				} while (Instant.now().isBefore(endOfInterval));
			}
		} while (Instant.now().isBefore(timeout));
		throw new AssertionFailedError("Timeout");
	}
}
