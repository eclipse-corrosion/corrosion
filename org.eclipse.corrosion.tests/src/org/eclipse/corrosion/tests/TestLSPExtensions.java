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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.corrosion.edit.RLSClientImplementation;
import org.eclipse.corrosion.extensions.ProgressParams;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.tests.harness.util.DisplayHelper;
import org.junit.Test;

public class TestLSPExtensions {

	@Test
	public void testBuildingMessages() {
		String jobType = "Building";
		RLSClientImplementation clientImplementation = new RLSClientImplementation();
		IJobManager jobManager = Job.getJobManager();
		clientImplementation.progress(new ProgressParams("progress_1", jobType));
		waitUntilJobIsStarted(jobManager, jobType);

		Job rustJob = getRustDiagnosticsJob(jobManager, jobType);
		assertNotNull(rustJob);

		clientImplementation.progress(new ProgressParams("progress_1", jobType, "rust_project", 0));
		clientImplementation.progress(new ProgressParams("progress_1", jobType, null, 50));

		clientImplementation.progress(new ProgressParams("progress_1", jobType, true));
		waitUntilJobIsDone(jobManager, jobType);
		assertEquals(rustJob.getResult().getCode(), IStatus.OK);
	}

	@Test
	public void testIndexingMessages() {
		String jobType = "Indexing";
		RLSClientImplementation clientImplementation = new RLSClientImplementation();
		IJobManager jobManager = Job.getJobManager();
		clientImplementation.progress(new ProgressParams("progress_2", jobType));
		waitUntilJobIsStarted(jobManager, jobType);

		Job rustJob = getRustDiagnosticsJob(jobManager, jobType);
		assertNotNull(rustJob);

		clientImplementation.progress(new ProgressParams("progress_2", jobType, true));
		waitUntilJobIsDone(jobManager, jobType);
		assertEquals(rustJob.getResult().getCode(), IStatus.OK);
	}

	private void waitUntilJobIsStarted(IJobManager jobManager, String jobType) {
		new DisplayHelper() {
			@Override
			protected boolean condition() {
				return getRustDiagnosticsJob(jobManager, jobType) != null;
			}
		}.waitForCondition(Display.getCurrent(), 5000);
	}

	private void waitUntilJobIsDone(IJobManager jobManager, String jobType) {
		new DisplayHelper() {
			@Override
			protected boolean condition() {
				return getRustDiagnosticsJob(jobManager, jobType) == null;
			}
		}.waitForCondition(Display.getCurrent(), 5000);
	}

	private Job getRustDiagnosticsJob(IJobManager jobManager, String jobType) {
		for (Job job : jobManager.find(null)) {
			if (jobType.equals(job.getName())) {
				return job;
			}
		}
		return null;
	}
}
