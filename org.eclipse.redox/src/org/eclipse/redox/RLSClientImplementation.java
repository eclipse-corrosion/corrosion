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
package org.eclipse.redox;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.lsp4e.LanguageClientImpl;
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;

@SuppressWarnings("restriction")
public class RLSClientImplementation extends LanguageClientImpl {

	private Job diagnosticsJob;
	private String status;

	@JsonNotification("rustDocument/beginBuild")
	public void beginBuild() {
		status = "Building project";
		initializeJob();
	}

	@JsonNotification("rustDocument/diagnosticsBegin")
	public void diagnosticsBegin() {
		status = "Compiling diagnostics";
		initializeJob();
	}

	@JsonNotification("rustDocument/diagnosticsEnd")
	public void diagnosticsEnd() {
		status = null;
		if (diagnosticsJob != null) {
			diagnosticsJob.done(new Status(IStatus.OK, RedoxPlugin.PLUGIN_ID, null));
			diagnosticsJob = null;
		}
	}

	private void initializeJob() {
		if (diagnosticsJob == null) {
			diagnosticsJob = Job.create("Compiling Rust project diagnostics", monitor -> {
				try {
					int maxWaitTime = 60000; // 1 minute
					monitor.beginTask("Building project", IProgressMonitor.UNKNOWN);
					while (maxWaitTime > 0) {
						if (status == null || monitor.isCanceled()) {
							break;
						} else {
							monitor.subTask(status);
						}
						Thread.sleep(50);
						maxWaitTime -= 50;
					}
					monitor.done();
					diagnosticsJob = null;
				} catch (InterruptedException e) {
					// Exception ends the job
				}
			});
			diagnosticsJob.schedule();
		}
	}
}
