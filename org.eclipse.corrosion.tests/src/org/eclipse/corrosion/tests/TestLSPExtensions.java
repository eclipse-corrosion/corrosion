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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.corrosion.edit.RLSClientImplementation;
import org.eclipse.corrosion.extensions.ProgressParams;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.tests.harness.util.DisplayHelper;
import org.junit.jupiter.api.Test;

class TestLSPExtensions {

	private static final String PROGRESS_ID_1 = "progress_1";
	private static final String PROGRESS_ID_2 = "progress_2";

	@Test
	void testBuildingMessages() {
		String jobType = "Building";
		RLSClientImplementation clientImplementation = new RLSClientImplementation();
		IJobManager jobManager = Job.getJobManager();
		clientImplementation.progress(new ProgressParams(PROGRESS_ID_1, jobType));
		waitUntilJobIsStarted(jobManager, jobType);

		Job rustJob = getRustDiagnosticsJob(jobManager, jobType);
		assertNotNull(rustJob);

		clientImplementation.progress(new ProgressParams(PROGRESS_ID_1, jobType, "rust_project", 0));
		clientImplementation.progress(new ProgressParams(PROGRESS_ID_1, jobType, null, 50));

		clientImplementation.progress(new ProgressParams(PROGRESS_ID_1, jobType, true));
		waitUntilJobIsDone(jobManager, jobType);
		assertEquals(IStatus.OK, rustJob.getResult().getCode());
	}

	@Test
	void testIndexingMessages() {
		String jobType = "Indexing";
		RLSClientImplementation clientImplementation = new RLSClientImplementation();
		IJobManager jobManager = Job.getJobManager();
		clientImplementation.progress(new ProgressParams(PROGRESS_ID_2, jobType));
		waitUntilJobIsStarted(jobManager, jobType);

		Job rustJob = getRustDiagnosticsJob(jobManager, jobType);
		assertNotNull(rustJob);

		clientImplementation.progress(new ProgressParams(PROGRESS_ID_2, jobType, true));
		waitUntilJobIsDone(jobManager, jobType);
		assertEquals(IStatus.OK, rustJob.getResult().getCode());
	}

	private static void waitUntilJobIsStarted(IJobManager jobManager, String jobType) {
		new DisplayHelper() {
			@Override
			protected boolean condition() {
				return getRustDiagnosticsJob(jobManager, jobType) != null;
			}
		}.waitForCondition(Display.getCurrent(), 5000);
	}

	private static void waitUntilJobIsDone(IJobManager jobManager, String jobType) {
		new DisplayHelper() {
			@Override
			protected boolean condition() {
				return getRustDiagnosticsJob(jobManager, jobType) == null;
			}
		}.waitForCondition(Display.getCurrent(), 5000);
	}

	private static Job getRustDiagnosticsJob(IJobManager jobManager, String jobType) {
		for (Job job : jobManager.find(null)) {
			if (jobType.equals(job.getName())) {
				return job;
			}
		}
		return null;
	}
}
