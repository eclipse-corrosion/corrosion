/*********************************************************************
 * Copyright (c) 2017, 2018 Red Hat Inc. and others.
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
package org.eclipse.corrosion.builder;

import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.corrosion.CorrosionPreferenceInitializer;

public class IncrementalCargoBuilder extends IncrementalProjectBuilder {

	public static final String BUILDER_ID = "org.eclipse.corrosion.builder.IncrementalCargoBuilder";
	private static Job buildJob;
	private static boolean wasCausedByRefresh = false;

	@Override
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
		if (wasCausedByRefresh) {
			wasCausedByRefresh = false;
			return null;
		}
		IProject project = getProject();
		IPath manifest = project.getFile("Cargo.toml").getLocation();
		IMarker[] errorMarkers = project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);

		if (buildJob != null) {
			buildJob.cancel();
		}
		if (errorMarkers.length == 0 && manifest != null) {
			buildJob = Job.create("cargo build", buildMonitor -> {
				SubMonitor subMonitor = SubMonitor.convert(buildMonitor, 2);
				subMonitor.worked(1);
				try {
					IPreferenceStore store = CorrosionPlugin.getDefault().getPreferenceStore();
					String cargo = store.getString(CorrosionPreferenceInitializer.cargoPathPreference);
					String[] commandList = { cargo, "build", "--manifest-path", manifest.toString() };
					Process buildProcess = DebugPlugin.exec(commandList, project.getLocation().makeAbsolute().toFile());
					while (buildProcess.isAlive() && !subMonitor.isCanceled()) {
						Thread.sleep(50);
					}
					wasCausedByRefresh = true;
					project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
					buildJob = null;
					if (subMonitor.isCanceled()) {
						buildProcess.destroyForcibly();
						return;
					}

				} catch (InterruptedException | CoreException e) {
					return;
				}
			});
			buildJob.schedule();
		}
		return null;
	}

}
