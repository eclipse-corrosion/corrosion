/*********************************************************************
 * Copyright (c) 2018 Red Hat Inc. and others.
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
package org.eclipse.corrosion.extensions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;

public class ProgressIndicatorJob extends Job {
	private SubMonitor subMonitor;
	private int completedPercentage = 0;
	private String subTaskName;
	private boolean isCompleted = false;

	public ProgressIndicatorJob(String type) {
		super(type);
	}

	public void update(ProgressParams params) {
		if (params.isDone()) {
			isCompleted = true;
		} else if (params.getMessage() != null) {
			updateCrateName(params.getMessage());
		} else if (params.getPercentage() > completedPercentage) {
			updatePercentage(params.getPercentage());
		}
	}

	private void updateCrateName(String name) {
		if (subMonitor != null) {
			subMonitor.subTask(name);
		}
		this.subTaskName = name;
	}

	private void updatePercentage(int completedPercentage) {
		if (subMonitor != null) {
			subMonitor.worked(completedPercentage - this.completedPercentage);
		}
		this.completedPercentage = completedPercentage;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		subMonitor = SubMonitor.convert(monitor, 100);
		if (completedPercentage > 0) {
			subMonitor.worked(completedPercentage);
		}
		try {
			subMonitor.beginTask(getName(), IProgressMonitor.UNKNOWN);
			if (subTaskName != null) {
				subMonitor.subTask(subTaskName);
			}
			while (!subMonitor.isCanceled() && !isCompleted) {
				Thread.sleep(50);
			}
			if (isCompleted) {
				return Status.OK_STATUS;
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return Status.CANCEL_STATUS;
	}

}
