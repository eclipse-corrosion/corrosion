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
import org.eclipse.corrosion.RLSClientImplementation;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.tests.harness.util.DisplayHelper;
import org.junit.Test;

public class TestLSPExtensions {

	@Test
	public void testInitializationMessages() {
		RLSClientImplementation clientImplementation = new RLSClientImplementation();
		IJobManager jobManager = Job.getJobManager();
		clientImplementation.beginBuild();
		new DisplayHelper() {
			@Override
			protected boolean condition() {
				return getRustDiagnosticsJob(jobManager) != null;
			}
		}.waitForCondition(Display.getCurrent(), 5000);

		Job rustJob = getRustDiagnosticsJob(jobManager);
		assertNotNull(rustJob);
		clientImplementation.diagnosticsBegin();
		clientImplementation.diagnosticsEnd();
		assertEquals(rustJob.getResult().getCode(), IStatus.OK);
	}

	private Job getRustDiagnosticsJob(IJobManager jobManager) {
		for (Job job : jobManager.find(null)) {
			if ("Compiling Rust project diagnostics".equals(job.getName())) {
				return job;
			}
		}
		return null;
	}
}
