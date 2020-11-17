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
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.unittest.launcher.ITestRunnerClient;
import org.eclipse.unittest.model.ITestElement;
import org.eclipse.unittest.model.ITestElement.FailureTrace;
import org.eclipse.unittest.model.ITestElement.Result;
import org.eclipse.unittest.model.ITestRunSession;
import org.eclipse.unittest.model.ITestSuiteElement;

public class CargoTestRunnerClient implements ITestRunnerClient {

	class TestElementReference {
		String parentId;
		String id;
		String name;
		Result failureKind;

		public TestElementReference(String parentId, String id, String name) {
			this.parentId = parentId;
			this.id = id;
			this.name = name;
			this.failureKind = Result.UNDEFINED;
		}
	}

	/**
	 * A simple state machine to process requests from the RemoteTestRunner
	 */
	private interface ProcessingState extends BiFunction<String, RunContext<String>, ProcessingState> {
		// nothing more
	}

	// Running target/debug/deps/new_rust_project-710966a2c9040a7a
	private static final String TEST_SUITE_HEADER_LINE_BEGIN = "Running"; //$NON-NLS-1$

	// running 0 tests
	private static final String TEST_SUITE_START_LINE_BEGIN = "running"; //$NON-NLS-1$
	private static final String TEST_SUITE_START_LINE_END_MULTIPLE = "tests"; //$NON-NLS-1$
	private static final String TEST_SUITE_START_LINE_END_SINGLE = "test"; //$NON-NLS-1$

	// test result: ok. 0 passed; 0 failed; 0 ignored; 0 measured; 0 filtered out
	// test result: FAILED. 1 passed; 1 failed; 0 ignored; 0 measured; 0 filtered
	// out
	private static final String TEST_SUITE_END_LINE = "test result:"; //$NON-NLS-1$
	private static final String STATUS_OK = "ok"; //$NON-NLS-1$
	private static final String STATUS_FAILED = "FAILED"; //$NON-NLS-1$

	// test tests::it_works ... ok
	// test tests::it_fails ... FAILED
	private static final String TEST_PERFORMED_LINE_BEGIN = "test "; //$NON-NLS-1$
	private static final String TEST_PERFORMED_LINE_END = "..."; //$NON-NLS-1$

	// failures:
	private static final String TEST_FAIUURES_LINE = "failures:"; //$NON-NLS-1$

	// ---- tests::it_fails stdout ----
	private static final String TEST_STDOUT_LINE_BEGIN = "---- "; //$NON-NLS-1$
	private static final String TEST_STDOUT_LINE_END = "stdout ----"; //$NON-NLS-1$

	// note: run with `RUST_BACKTRACE=1` environment variable to display a backtrace
	private static final String TEST_NOTE_LINE = "note:"; //$NON-NLS-1$

	private static final String TEST_NAME_SEPARATOR = "::"; //$NON-NLS-1$
	private static final String TEST_SUITE_NAME_PREFIX = ""; //$NON-NLS-1$
	private static final String TEST_ELEMENT_DISPLAY_NAME_PREFIX = "Target: "; //$NON-NLS-1$

	private int fTestId = -1;

	private Map<String, TestElementReference> fExecutedTests = new HashMap<>();
	private String fFailedTestCaseName = null;
	private StringBuilder fFailedTestStdout = new StringBuilder();

	private Deque<ITestSuiteElement> fRootTestSuiteStack = new LinkedList<>();

	class DefaultProcessingState implements ProcessingState {
		@Override
		public ProcessingState apply(String message, RunContext<String> context) {
			// running 0 tests
			if (message.startsWith(TEST_SUITE_START_LINE_BEGIN) && (message.endsWith(TEST_SUITE_START_LINE_END_SINGLE)
					|| message.endsWith(TEST_SUITE_START_LINE_END_MULTIPLE))) {
				/* The following code is not used */
//				int count = 0;
//				try {
//					String arg = message
//							.substring(TEST_SUITE_START_LINE_BEGIN.length(), message.indexOf(TEST_SUITE_START_LINE_END))
//							.trim();
//					count = Integer.parseInt(arg);
//				} catch (IndexOutOfBoundsException | NumberFormatException e) {
//					CorrosionPlugin.logError(e);
//				}

				String testSuiteId = String.valueOf(++fTestId);
				String testSuiteName = TEST_SUITE_NAME_PREFIX + context.next();
				String testSuiteDisplayName = TEST_ELEMENT_DISPLAY_NAME_PREFIX + testSuiteName;

				session.newTestSuite(testSuiteId, testSuiteName, null, null, testSuiteDisplayName, null);

				fRootTestSuiteStack.push(getTestSuite(testSuiteId));
				return this;
			}

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
				ITestSuiteElement rootSuite = fRootTestSuiteStack.isEmpty() ? null : fRootTestSuiteStack.peek();

				TestElementReference parentSuiteRef = testSuiteName != null
						? getOrCreateParentTestSuite(testSuiteName, rootSuite)
						: null;
				ITestSuiteElement parentSuite = testSuiteName != null
						? (ITestSuiteElement) session.getTestElement(parentSuiteRef.id)
						: rootSuite;

				String testId = String.valueOf(++fTestId);

				TestElementReference testRef = new TestElementReference(parentSuite.getId(), testId, testName);
				fExecutedTests.put(testName, testRef);
				session.newTestCase(testId, testName, parentSuite, testDisplayName, null);

				ITestElement testElement = session.getTestElement(testId);
				session.notifyTestStarted(testElement);
				if (message.endsWith(STATUS_FAILED)) {
					testRef.failureKind = Result.FAILURE;
					session.notifyTestFailed(session.getTestElement(testId), Result.FAILURE, false,
							new FailureTrace("", "", "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				} else if (message.endsWith(STATUS_OK)) {
					testRef.failureKind = Result.OK;
				}
				session.notifyTestEnded(testElement, false);
				return this;
			}

			// test result: ok. 0 passed; 0 failed; 0 ignored; 0 measured; 0 filtered out
			// test result: FAILED. 1 passed; 1 failed; 0 ignored; 0 measured; 0 filtered
			// out
			if (message.startsWith(TEST_SUITE_END_LINE)) {
				if (!fRootTestSuiteStack.isEmpty()) {
					fRootTestSuiteStack.pop();
				} else {
					CorrosionPlugin.logError(new Exception("Ending of an unexpected Test Suite element")); //$NON-NLS-1$
				}
				context.done();
				return this;
			}

			// failures:
			if (message.startsWith(TEST_FAIUURES_LINE)) {
				return fTraceState;
			}

			return this;
		}

		private TestElementReference getOrCreateParentTestSuite(String testSuiteName, ITestSuiteElement rootSuite) {
			String[] displayNames = testSuiteName.split(TEST_NAME_SEPARATOR);
			String parentSuiteName = null;

			for (String displayName : displayNames) {
				String name = parentSuiteName != null ? parentSuiteName + TEST_NAME_SEPARATOR + displayName
						: displayName;
				TestElementReference suiteRef = fExecutedTests.get(name);
				if (suiteRef == null) {
					String testSuiteId = String.valueOf(++fTestId);

					TestElementReference parentSuiteRef = parentSuiteName != null ? fExecutedTests.get(parentSuiteName)
							: null;
					ITestSuiteElement parentSuite = parentSuiteRef != null
							? (ITestSuiteElement) session.getTestElement(parentSuiteRef.id)
							: rootSuite;
					String parentSuiteId = parentSuite != null ? parentSuite.getId() : null;

					suiteRef = new TestElementReference(parentSuiteId, testSuiteId, name);
					fExecutedTests.put(name, suiteRef);
					session.newTestSuite(testSuiteId, name, null, parentSuite, displayName, null);
				}
				parentSuiteName = name;
			}
			return fExecutedTests.get(parentSuiteName);
		}

		private ITestSuiteElement getTestSuite(String parentId) {
			ITestElement element = session.getTestElement(parentId);
			return element instanceof ITestSuiteElement ? (ITestSuiteElement) element : null;
		}
	}

	class TraceProcessingState implements ProcessingState {
		boolean isCollectingAFailureTrace = false;

		private void reset() {
			// Clear the buffers and test name
			fFailedTestCaseName = null;
			fFailedTestStdout.setLength(0);
			isCollectingAFailureTrace = false;
		}

		private void submit() {
			// Submit the existing buffer to a test case element, if any
			if (fFailedTestCaseName != null && fExecutedTests.get(fFailedTestCaseName) != null
					&& fFailedTestStdout.length() > 0) {
				TestElementReference testRef = fExecutedTests.get(fFailedTestCaseName);
				FailureTrace failureTrace = fillFailureTrace(fFailedTestStdout.toString());

				session.notifyTestFailed(session.getTestElement(testRef.id), testRef.failureKind, false, failureTrace);
			}
		}

		private static final String FAILURE_THREAD = "thread"; //$NON-NLS-1$
		private static final String FAILURE_PANICKED_AT_BEGIN = "panicked at"; //$NON-NLS-1$
		private static final String FAILURE_PANICKED_AT_END = "',"; //$NON-NLS-1$

		private static final String FAILURE_ASSERTION_BEGIN = "'assertion failed:"; //$NON-NLS-1$
		private static final String FAILURE_ASSERTION_LEFT = "left: "; //$NON-NLS-1$
		private static final String FAILURE_ASSERTION_RIGHT = "right:"; //$NON-NLS-1$
		private static final String FAILURE_ASSERTION_SEPARATOR = ","; //$NON-NLS-1$

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
		public ProcessingState apply(String message, RunContext<String> context) {
//			---- tests::it_fails stdout ----
			if (message.startsWith(TEST_STDOUT_LINE_BEGIN) && message.endsWith(TEST_STDOUT_LINE_END)) {
				submit();
				reset();

				fFailedTestCaseName = message
						.substring(TEST_STDOUT_LINE_BEGIN.length(), message.indexOf(TEST_STDOUT_LINE_END)).trim();
				isCollectingAFailureTrace = true;
				return this;
			}

			// test result: ok. 0 passed; 0 failed; 0 ignored; 0 measured; 0 filtered out
			// test result: FAILED. 1 passed; 1 failed; 0 ignored; 0 measured; 0 filtered
			// out
			if (message.startsWith(TEST_SUITE_END_LINE)) {
				submit();
				reset();

				if (!fRootTestSuiteStack.isEmpty()) {
					fRootTestSuiteStack.pop();
				} else {
					CorrosionPlugin.logError(new Exception("Ending of an unexpected Test Suite element")); //$NON-NLS-1$
				}
				context.done();
				return fDefaultState;
			}

//			thread 'tests::it_fails' panicked at 'assertion failed: `(left == right)`
//			  left: `4`,
//			 right: `5`', tests/testfoo.rs:12:9

//			note: run with `RUST_BACKTRACE=1` environment variable to display a backtrace
//			failures:
//			    tests::it_fails
			if (message.startsWith(TEST_NOTE_LINE) || message.startsWith(TEST_FAIUURES_LINE)) {
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
	private InputStream errorStream;

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
		this.process = launch.getProcesses()[0];
		if (this.process != null && this.errorStream == null && this.inputStream == null) {
			errorStream = toInputStream(process, true);
			inputStream = toInputStream(process, false);
			Job.createSystem("Monitor test process", (ICoreRunnable) monitor -> run(errorStream, inputStream)) //$NON-NLS-1$
					.schedule(100);
			// TODO schedule(100) is a workaround because we need to wait for listeners to
			// be plugged in, but
			// it's actually a design issue: listeners should be part of the session and
			// plugged in earlier so
			// no delay would be necessary
		}
		return this.process;
	}

	private static InputStream toInputStream(IProcess process, boolean errorStream) {
		IStreamMonitor monitor = errorStream ? process.getStreamsProxy().getErrorStreamMonitor()
				: process.getStreamsProxy().getOutputStreamMonitor();
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

	class RunContext<T> {
		Deque<T> queue = new LinkedList<>();
		T current;

		synchronized void enqueue(T item) {
			queue.add(item);
		}

		synchronized boolean isEmpty() {
			return queue.isEmpty() && current == null;
		}

		synchronized T next() {
			return current = queue.pop();
		}

		synchronized boolean busy() {
			return current != null;
		}

		synchronized void done() {
			current = null;
		}
	}

	private void run(InputStream eStream, InputStream iStream) {
		if (iStream == null || eStream == null) {
			return;
		}
		session.notifyTestSessionStarted(null);
		fExecutedTests.clear();
		try (InputStreamReader esReader = new InputStreamReader(eStream, StandardCharsets.UTF_8);
				BufferedReader eReader = new BufferedReader(esReader);
				PushbackReader errorrReader = new PushbackReader(eReader);
				InputStreamReader isReader = new InputStreamReader(iStream, StandardCharsets.UTF_8);
				BufferedReader reader = new BufferedReader(isReader);
				PushbackReader inputReader = new PushbackReader(reader)) {

			String message;
			RunContext<String> context = new RunContext<>();
			while (true) {
				//
				if (!context.busy()) {
					message = readMessage(errorrReader);
					if (message == null && context.isEmpty()) {
						break; // Nothing to do - stop the test run
					}
					receiveHeaders(message, context);
					if (context.isEmpty()) {
						continue; // Waiting for a next test suite
					}
				}

				if (!context.isEmpty()) {
					message = readMessage(inputReader);
					if (message == null) {
						break;
					}
					fCurrentState = fCurrentState.apply(message, context);
				}
			}
			session.notifyTestSessionCompleted(session.getDuration());
		} catch (IOException e) {
			CorrosionPlugin.logError(e);
			session.notifyTestSessionAborted(null, e);
		}
		fExecutedTests.clear();
		fRootTestSuiteStack.clear();
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

	public void receiveHeaders(String message, RunContext<String> context) {
		if (message != null) {
			message = message.trim();
			// Running target/debug/deps/testfoo-6f7cfd8c97086512
			if (message.startsWith(TEST_SUITE_HEADER_LINE_BEGIN)) {
				String testSuiteName = message
						.substring(TEST_SUITE_HEADER_LINE_BEGIN.length(), message.lastIndexOf('-')).trim();
				context.enqueue(testSuiteName);
			}
		}
	}

	@Override
	public void stopTest() {
		stopMonitoring();
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
		try {
			if (errorStream != null) {
				errorStream.close();
				errorStream = null;
			}
		} catch (IOException e) {
			CorrosionPlugin.logError(e);
		}
	}

}
