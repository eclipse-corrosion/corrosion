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
import java.io.PushbackReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.unittest.launcher.ITestRunnerClient;
import org.eclipse.unittest.model.ITestElement;
import org.eclipse.unittest.model.ITestElement.FailureTrace;
import org.eclipse.unittest.model.ITestElement.Result;
import org.eclipse.unittest.model.ITestRunSession;
import org.eclipse.unittest.model.ITestSuiteElement;

public class CargoTestRunnerClient implements ITestRunnerClient {

	/**
	 * A simple state machine to process requests from the RemoteTestRunner
	 */
	private interface ProcessingState extends Function<String, ProcessingState> {
		// nothing more
	}

	// test result: ok. 0 passed; 0 failed; 0 ignored; 0 measured; 0 filtered out
	// test result: FAILED. 1 passed; 1 failed; 0 ignored; 0 measured; 0 filtered
	// out
	private static final String STATUS_FAILED = "FAILED"; //$NON-NLS-1$

	// test tests::it_works ... ok
	// test tests::it_fails ... FAILED
	private static final String TEST_PERFORMED_LINE_BEGIN = "test "; //$NON-NLS-1$
	private static final String TEST_PERFORMED_LINE_END = "..."; //$NON-NLS-1$

	// failures:
	private static final String TEST_FAILURES_LINE = "failures:"; //$NON-NLS-1$

	// ---- tests::it_fails stdout ----
	private static final String TEST_STDOUT_LINE_BEGIN = "---- "; //$NON-NLS-1$
	private static final String TEST_STDOUT_LINE_END = "stdout ----"; //$NON-NLS-1$

	// note: run with `RUST_BACKTRACE=1` environment variable to display a backtrace
	private static final String TEST_NOTE_LINE = "note:"; //$NON-NLS-1$

	private static final String TEST_NAME_SEPARATOR = "::"; //$NON-NLS-1$

	private String fFailedTestCaseName = null;
	private StringBuilder fFailedTestStdout = new StringBuilder();

	class DefaultProcessingState implements ProcessingState {
		@Override
		public ProcessingState apply(String message) {
//			test tests::it_works ... ok
//			test tests::it_fails ... FAILED
			if (message.startsWith(TEST_PERFORMED_LINE_BEGIN) && message.contains(TEST_PERFORMED_LINE_END)) {
				String testName = message
						.substring(TEST_PERFORMED_LINE_BEGIN.length(), message.indexOf(TEST_PERFORMED_LINE_END)).trim();
				String testDisplayName = testName.contains(TEST_NAME_SEPARATOR)
						? testName.substring(testName.lastIndexOf(TEST_NAME_SEPARATOR) + TEST_NAME_SEPARATOR.length())
								.trim()
						: testName;
				String testSuiteName = testName.contains(TEST_NAME_SEPARATOR)
						? testName.substring(0, testName.lastIndexOf(TEST_NAME_SEPARATOR)).trim()
						: null;
				ITestSuiteElement suite = getOrCreateTestSuite(testSuiteName);
				ITestElement testElement = session.newTestCase(testName, testName, suite, testDisplayName, message);
				session.notifyTestStarted(testElement);
				if (message.endsWith(STATUS_FAILED)) {
					session.notifyTestFailed(testElement, Result.FAILURE, false, null);
				}
				session.notifyTestEnded(testElement, false);
				return this;
			}

			// failures:
			if (message.startsWith(TEST_FAILURES_LINE)) {
				return fTraceState;
			}

			return this;
		}

		private ITestSuiteElement getOrCreateTestSuite(String testSuiteName) {
			if (testSuiteName == null) {
				return session;
			}
			ITestSuiteElement parent = session;
			String[] segments = testSuiteName.split(TEST_NAME_SEPARATOR);

			for (String segment : segments) {
				String currentSuiteName = parent instanceof ITestRunSession ? segment
						: parent.getTestName() + TEST_NAME_SEPARATOR + segment;
				ITestSuiteElement currentSuite = (ITestSuiteElement) session.getTestElement(currentSuiteName);
				if (currentSuite == null) {
					currentSuite = session.newTestSuite(currentSuiteName, currentSuiteName, null, parent, segment,
							null);
				}
				parent = currentSuite;
			}
			return parent;
		}
	}

	class TraceProcessingState implements ProcessingState {
		private static final String FAILURE_THREAD = "thread"; //$NON-NLS-1$
		private static final String FAILURE_PANICKED_AT_BEGIN = "panicked at"; //$NON-NLS-1$
		private static final String FAILURE_PANICKED_AT_END = "',"; //$NON-NLS-1$

		private static final String FAILURE_ASSERTION_BEGIN = "'assertion failed:"; //$NON-NLS-1$
		private static final String FAILURE_ASSERTION_LEFT = "left: "; //$NON-NLS-1$
		private static final String FAILURE_ASSERTION_RIGHT = "right:"; //$NON-NLS-1$
		private static final String FAILURE_ASSERTION_SEPARATOR = ","; //$NON-NLS-1$

		boolean isCollectingAFailureTrace = false;

		private void reset() {
			// Clear the buffers and test name
			fFailedTestCaseName = null;
			fFailedTestStdout.setLength(0);
			isCollectingAFailureTrace = false;
		}

		private void submit() {
			// Submit the existing buffer to a test case element, if any
			if (fFailedTestCaseName != null && fFailedTestStdout.length() > 0) {
				ITestElement testElement = session.getTestElement(fFailedTestCaseName);
				if (testElement != null) {
					FailureTrace failureTrace = fillFailureTrace(fFailedTestStdout.toString());
					session.notifyTestFailed(testElement, Result.FAILURE, false, failureTrace);
				}
			}
		}

		private FailureTrace fillFailureTrace(String trace) {
			// thread 'tests1::it_fails_on_panic' panicked at 'Make this test fail',
			// tests/testfoo.rs:24:9
			if (trace.contains(FAILURE_THREAD) && trace.contains(FAILURE_PANICKED_AT_BEGIN)
					&& trace.contains(FAILURE_PANICKED_AT_END)) {
				// a common case to get a source reference from
				int panickedAtEmd = trace.lastIndexOf(FAILURE_PANICKED_AT_END);
				String panickedAtText = trace.substring(0, panickedAtEmd);
				String source = trace.substring(panickedAtEmd + FAILURE_PANICKED_AT_END.length()).strip();
				StringBuilder failureTrace = new StringBuilder();
				failureTrace.append(panickedAtText);
				failureTrace.append('\n').append(CargoTestViewSupport.FRAME_PREFIX).append(source); // $NON-NLS-1$

				// Some private cases

				// thread 'tests::it_fails' panicked at 'assertion failed: `(left == right)`
				// left: `4`,
				// right: `5`', tests/testfoo.rs:12:9
				if (panickedAtText.contains(FAILURE_ASSERTION_BEGIN)) {
					int leftIndex = trace.indexOf(FAILURE_ASSERTION_LEFT);
					int rightIndex = trace.indexOf(FAILURE_ASSERTION_RIGHT, leftIndex);
					if (leftIndex != -1 && rightIndex != -1) {
						String leftValue = panickedAtText.substring(leftIndex + FAILURE_ASSERTION_LEFT.length(),
								panickedAtText.lastIndexOf(FAILURE_ASSERTION_SEPARATOR, rightIndex)).strip();
						String rightValue = panickedAtText.substring(rightIndex + FAILURE_ASSERTION_RIGHT.length())
								.strip();
						return new FailureTrace(failureTrace.toString(), leftValue, rightValue);
					}
				}
				return new FailureTrace(failureTrace.toString(), null, null);
			}

			return new FailureTrace(trace, null, null);
		}

		@Override
		public ProcessingState apply(String message) {
//			---- tests::it_fails stdout ----
			if (message.startsWith(TEST_STDOUT_LINE_BEGIN) && message.endsWith(TEST_STDOUT_LINE_END)) {
				submit();
				reset();

				fFailedTestCaseName = message
						.substring(TEST_STDOUT_LINE_BEGIN.length(), message.indexOf(TEST_STDOUT_LINE_END)).trim();
				isCollectingAFailureTrace = true;
				return this;
			}

//			thread 'tests::it_fails' panicked at 'assertion failed: `(left == right)`
//			  left: `4`,
//			 right: `5`', tests/testfoo.rs:12:9

//			note: run with `RUST_BACKTRACE=1` environment variable to display a backtrace
//			failures:
//			    tests::it_fails
			if (message.startsWith(TEST_NOTE_LINE) || message.startsWith(TEST_FAILURES_LINE)) {
				submit();
				reset();
				return this;
			}

			if (isCollectingAFailureTrace) {
				fFailedTestStdout.append(message);
				if (fLastLineDelimiter != null) {
					fFailedTestStdout.append(fLastLineDelimiter);
				}
			}

			return this;
		}
	}

	private IProcess process;
	private ITestRunSession session;
	private InputStream inputStream;
	private boolean monitoringDebuggedPrograms;

	ProcessingState fDefaultState = new DefaultProcessingState();
	ProcessingState fTraceState = new TraceProcessingState();
	ProcessingState fCurrentState = fDefaultState;

	public CargoTestRunnerClient(ITestRunSession session) {
		this.session = session;
	}

	private IProcess connectProcess(ILaunch launch) {
		if (this.process != null) {
			return this.process;
		}
		if (ILaunchManager.DEBUG_MODE.equals(launch.getLaunchMode())) {
			// In debug mode, GDB starts the test programs in other launches. See
			// CargoTestDelegate.
			if (!this.monitoringDebuggedPrograms) {
				this.monitoringDebuggedPrograms = true;
				inputStream = debuggedTestProgramsStream(launch);
				Job.createSystem("Monitor debugged test programs", (ICoreRunnable) monitor -> run(inputStream)) //$NON-NLS-1$
						.schedule(100);
			}
			return null;
		}
		this.process = launch.getProcesses()[0];
		if (this.process != null && this.inputStream == null) {
			inputStream = toInputStream(process, false);
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

	/**
	 * Returns the output of the test programs that GDB starts for the given Cargo
	 * test launch, one after the other.
	 */
	static InputStream debuggedTestProgramsStream(ILaunch launch) {
		return new InputStream() {
			private InputStream current;
			private volatile boolean closed;
			/**
			 * True after the last test program. The reader can read again after the end,
			 * and then must get the end again, not wait for a next program.
			 */
			private boolean ended;

			/** Waits for the next test program. Returns false if no more come. */
			private boolean nextProgram() {
				while (current == null && !closed && !ended) {
					IProcess next;
					try {
						next = DebuggedTestPrograms.next(launch);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return false;
					}
					if (next == null) {
						ended = true;
						return false;
					}
					current = toInputStream(next, false);
				}
				return current != null;
			}

			@Override
			public int read() throws IOException {
				while (nextProgram()) {
					int c = current.read();
					if (c != -1) {
						return c;
					}
					current = null;
				}
				return -1;
			}

			@Override
			public int read(byte[] b, int off, int len) throws IOException {
				while (nextProgram()) {
					int count = current.read(b, off, len);
					if (count != -1) {
						return count;
					}
					current = null;
				}
				return -1;
			}

			@Override
			public void close() {
				closed = true;
			}
		};
	}

	private static InputStream toInputStream(IProcess process, boolean errorStream) {
		IStreamsProxy streams = process.getStreamsProxy();
		if (streams == null) {
			return null;
		}
		IStreamMonitor monitor = errorStream ? streams.getErrorStreamMonitor() : streams.getOutputStreamMonitor();
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
						Thread.currentThread().interrupt();
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

	private void run(InputStream iStream) {
		if (iStream == null) {
			return;
		}
		session.notifyTestSessionStarted(null);
		try (InputStreamReader isReader = new InputStreamReader(iStream, StandardCharsets.UTF_8);
				BufferedReader reader = new BufferedReader(isReader);
				PushbackReader inputReader = new PushbackReader(reader)) {

			String message;
			do {
				message = readMessage(inputReader);
				if (message != null) {
					fCurrentState = fCurrentState.apply(message);
				}
			} while (message != null);
			session.notifyTestSessionCompleted(session.getDuration());
		} catch (IOException e) {
			CorrosionPlugin.logError(e);
			session.notifyTestSessionAborted(null, e);
		}
	}

	private String fLastLineDelimiter = "\n"; //$NON-NLS-1$

	private String readMessage(PushbackReader in) throws IOException {
		if (in == null) {
			return null;
		}

		StringBuilder buf = new StringBuilder(128);
		int ch;
		while ((ch = in.read()) != -1) {
			switch (ch) {
			case '\n':
				fLastLineDelimiter = "\n"; //$NON-NLS-1$
				return buf.toString();
			case '\r':
				ch = in.read();
				if (ch == '\n') {
					fLastLineDelimiter = "\r\n"; //$NON-NLS-1$
				} else {
					in.unread(ch);
					fLastLineDelimiter = "\r"; //$NON-NLS-1$
				}
				return buf.toString();
			default:
				buf.append((char) ch);
				break;
			}
		}
		fLastLineDelimiter = null;
		if (buf.length() == 0) {
			return null;
		}
		return buf.toString();
	}

	@Override
	public void stopTest() {
		stopMonitoring();
		ILaunch launch = session.getLaunch();
		// In debug mode, also stop the debug run, else the next test programs start
		if (launch != null && ILaunchManager.DEBUG_MODE.equals(launch.getLaunchMode()) && launch.canTerminate()) {
			try {
				launch.terminate();
			} catch (DebugException e) {
				CorrosionPlugin.logError(e);
			}
		}
	}

	@Override
	public void startMonitoring() {
		this.process = connectProcess(session.getLaunch());
	}

	@Override
	public void stopMonitoring() {
		try {
			if (inputStream != null) {
				inputStream.close();
				inputStream = null;
			}
		} catch (IOException e) {
			CorrosionPlugin.logError(e);
		}
	}

}
