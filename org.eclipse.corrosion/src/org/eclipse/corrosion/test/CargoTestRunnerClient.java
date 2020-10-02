/*********************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.corrosion.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.unittest.launcher.TestRunnerClient;
import org.eclipse.unittest.model.ITestElement;
import org.eclipse.unittest.model.ITestRunSession;

public class CargoTestRunnerClient extends TestRunnerClient {

	private IProcess process;
	private ITestRunSession session;

	public CargoTestRunnerClient(ITestRunSession session) {
		this.session = session;
		this.process = connectProcess(session.getLaunch());
	}

	private IProcess connectProcess(ILaunch launch) {
		if (this.process != null) {
			return this.process;
		}
		this.process = launch.getProcesses()[0];
		if (this.process != null) {
			InputStream inputStream = toInputStream(process);
			Job.createSystem("Monitor test process", (ICoreRunnable) monitor -> run(inputStream)) //$NON-NLS-1$
					.schedule(100);
			// TODO schedule(100) is a workaround because we need to wait for listeners to
			// be plugged in, but
			// it's actually a design issue: listeners should be part of the session and
			// plugged in earlier so
			// no delay would be necessary
		}
		return this.process;
	}

	private static InputStream toInputStream(IProcess process) {
		IStreamMonitor monitor = process.getStreamsProxy().getOutputStreamMonitor();
		if (monitor == null) {
			return null;
		}
		List<Integer> content = Collections.synchronizedList(new LinkedList<>());
		monitor.addListener((text, progresMonitor) -> text.chars().forEach(content::add));
		byte[] initialContent = monitor.getContents().getBytes();
		for (int i = initialContent.length - 1; i >= 0; i--) {
			content.add(0, Integer.valueOf(initialContent[i]));
		}
		return new InputStream() {
			@Override
			public int read() throws IOException {
				while (!process.isTerminated() || !content.isEmpty()) {
					if (!content.isEmpty()) {
						return content.remove(0).intValue();
					}
					try {
						Thread.sleep(20, 0);
					} catch (InterruptedException e) {
						return -1;
					}
				}
				return -1;
			}

			@Override
			public int read(byte[] b, int off, int len) throws IOException {
				if (process.isTerminated() && available() == 0) {
					return -1;
				}
				if (len == 0) {
					return 0;
				}
				int i = 0;
				do {
					b[off + i] = (byte) read();
					i++;
				} while (available() > 0 && i < len && off + i < b.length);
				return i;
			}

			@Override
			public int available() throws IOException {
				return content.size();
			}
		};
	}

	public void run(InputStream iStream) {
		if (iStream == null) {
			return;
		}
		notifyTestRunStarted(0);
		try (InputStreamReader isReader = new InputStreamReader(iStream);
				BufferedReader reader = new BufferedReader(isReader)) {
			String line = null;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("test ") && line.contains("...")) { //$NON-NLS-1$ //$NON-NLS-2$
					String testId = line.substring("test ".length(), line.indexOf(" ...")); //$NON-NLS-1$ //$NON-NLS-2$
					notifyTestStarted(testId, testId);
					if (line.endsWith("FAILED")) { //$NON-NLS-1$ `
//						extractFailure(testId, testId, 1, false); // TODO randomish
//						notifyTestFailed();
						notifyTestFailed(ITestElement.Status.ERROR.getOldCode(), testId, testId, false, "", "", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					}
					notifyTestEnded(testId, testId, false);
				}
			}
			notifyTestRunEnded((long) (session.getElapsedTimeInSeconds() * 1000));
		} catch (IOException e) {
			CorrosionPlugin.logError(e);
			notifyTestRunTerminated();
		}
		shutDown();
	}

	@Override
	public void stopTest() {
		// TODO Auto-generated method stub

	}

}
