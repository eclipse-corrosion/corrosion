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
package org.eclipse.redox.builder;

import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.redox.RedoxPlugin;
import org.eclipse.redox.RedoxPreferenceInitializer;

public class IncrementalCargoBuilder extends IncrementalProjectBuilder {

	public static final String BUILDER_ID = "org.eclipse.redox.builder.IncrementalCargoBuilder";

	private boolean isBuilding = false;
	private Process buildProcess;

	@Override
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
		IProject project = getProject();
		IPath manifest = project.getFile("Cargo.toml").getLocation();
		IMarker[] errorMarkers = project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);

		if (isBuilding && buildProcess != null) {
			buildProcess.destroyForcibly();
		}
		if (errorMarkers.length == 0 && manifest != null) {
			Job.create("cargo build", (ICoreRunnable) buildMonitor -> {
				try {
					IPreferenceStore store = RedoxPlugin.getDefault().getPreferenceStore();
					String cargo = store.getDefaultString(RedoxPreferenceInitializer.cargoPathPreference);
					String[] commandList = { cargo, "build", "--manifest-path", manifest.toString() };
					isBuilding = true;
					buildProcess = DebugPlugin.exec(commandList, project.getLocation().makeAbsolute().toFile());
					buildProcess.waitFor();
					isBuilding = false;
					project.refreshLocal(IResource.DEPTH_INFINITE, null);
				} catch (InterruptedException | CoreException e) {
					return;
				}
			}).schedule();
		}
		return null;
	}

}
