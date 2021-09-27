/*********************************************************************
 * Copyright (c) 2018, 2021 Red Hat Inc. and others.
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
package org.eclipse.corrosion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;

public class CommandJob extends Job {
	private Process process;
	private String[] command;
	private String progressMessage;
	private String errorTitle;
	private String errorMessage;
	private int expectedWork;

	public CommandJob(String[] command, String progressMessage, String errorTitle, String errorMessage,
			int expectedWork) {
		super(progressMessage);
		if (command == null) {
			this.command = null;
		} else {
			this.command = Arrays.copyOf(command, command.length);
		}
		this.progressMessage = progressMessage;
		this.errorTitle = errorTitle;
		this.errorMessage = errorMessage;
		this.expectedWork = expectedWork;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor, expectedWork);
		try {
			subMonitor.beginTask(progressMessage, expectedWork);
			process = CorrosionPlugin.getProcessForCommand(command);
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
				CompletableFuture.runAsync(() -> reader.lines().forEachOrdered(line -> {
					subMonitor.subTask(line);
					if (expectedWork > 0) {
						subMonitor.worked(1);
					}
				}));
				if (process.waitFor() != 0) {
					if (!subMonitor.isCanceled()) {
						CorrosionPlugin.showError(errorTitle, errorMessage);
					}
					return Status.CANCEL_STATUS;
				}
			}
			return Status.OK_STATUS;
		} catch (IOException e) {
			CorrosionPlugin.showError(errorTitle, errorMessage, e);
			return Status.CANCEL_STATUS;
		} catch (InterruptedException e) {
			CorrosionPlugin.showError(errorTitle, errorMessage, e);
			Thread.currentThread().interrupt();
			return Status.CANCEL_STATUS;
		}
	}

	@Override
	protected void canceling() {
		if (process != null) {
			process.destroyForcibly();
		}
	}
}
