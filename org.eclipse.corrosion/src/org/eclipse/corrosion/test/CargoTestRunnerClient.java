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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

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
	abstract class ProcessingState {
		abstract ProcessingState readMessage(String message);
	}

	// Running target/debug/deps/new_rust_project-710966a2c9040a7a
	private static final String TEST_SUITE_HEADER_LINE_BEGIN = "Running"; //$NON-NLS-1$

	// running 0 tests
	private static final String TEST_SUITE_START_LINE_BEGIN = "running"; //$NON-NLS-1$
	private static final String TEST_SUITE_START_LINE_END = "tests"; //$NON-NLS-1$

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

	List<String> fTestSuiteNameList = new ArrayList<>();
	Stack<ITestSuiteElement> fRootTestSuiteStack = new Stack<>();

	class DefaultProcessingState extends ProcessingState {
		@Override
		ProcessingState readMessage(String message) {
			message = message.trim();
			// Running target/debug/deps/testfoo-6f7cfd8c97086512
			if (message.startsWith(TEST_SUITE_HEADER_LINE_BEGIN)) {
				String testSuiteName = message
						.substring(TEST_SUITE_HEADER_LINE_BEGIN.length(), message.lastIndexOf('-')).trim();
				fTestSuiteNameList.add(testSuiteName);
				return this;
			}

			// running 0 tests
			if (message.startsWith(TEST_SUITE_START_LINE_BEGIN) && message.endsWith(TEST_SUITE_START_LINE_END)) {
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
				String testSuiteName = TEST_SUITE_NAME_PREFIX + fTestSuiteNameList.remove(0);
				String testSuiteDisplayName = TEST_ELEMENT_DISPLAY_NAME_PREFIX + testSuiteName;

				session.newTestSuite(testSuiteId, testSuiteName, null, true, null, testSuiteDisplayName, null);

				fRootTestSuiteStack.push(getTestSuite(testSuiteId));
				return this;
			}

//			test tests::it_works ... ok
//			test tests::it_fails ... FAILED
			if (message.startsWith(TEST_PERFORMED_LINE_BEGIN) && message.contains(TEST_PERFORMED_LINE_END)) {
				String testName = message
						.substring(TEST_PERFORMED_LINE_BEGIN.length(), message.indexOf(TEST_PERFORMED_LINE_END)).trim();
				String testDisplayName = testName
						.substring(testName.indexOf(TEST_NAME_SEPARATOR) + TEST_NAME_SEPARATOR.length()).trim();
				String testSuiteName = testName.substring(0, testName.indexOf(TEST_NAME_SEPARATOR)).trim();

				ITestSuiteElement rootSuite = fRootTestSuiteStack.isEmpty() ? null : fRootTestSuiteStack.peek();

				TestElementReference parentSuiteRef = fExecutedTests.get(testSuiteName);
				if (parentSuiteRef == null) {
					String testSuiteId = String.valueOf(++fTestId);
					parentSuiteRef = new TestElementReference(rootSuite.getId(), testSuiteId, testSuiteName);
					fExecutedTests.put(testSuiteName, parentSuiteRef);
					session.newTestSuite(testSuiteId, testSuiteName, null, true, rootSuite, testSuiteName, null);
				}

				ITestSuiteElement parentSuite = (ITestSuiteElement) session.getTestElement(parentSuiteRef.id);

				String testId = String.valueOf(++fTestId);

				TestElementReference testRef = new TestElementReference(parentSuite.getId(), testId, testName);
				fExecutedTests.put(testName, testRef);
				session.newTestCase(testId, testName, true, parentSuite, testDisplayName, null);

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
				return this;
			}

			// failures:
			if (message.startsWith(TEST_FAIUURES_LINE)) {
				return fTraceState;
			}

			return this;
		}
	}

	class TraceProcessingState extends ProcessingState {
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

				session.notifyTestFailed(session.getTestElement(testRef.id), testRef.failureKind,
						failureTrace.isComparisonFailure(), failureTrace);
			}
		}

		private static final String FAILURE_ASSERTION_BEGIN = "'assertion failed:"; //$NON-NLS-1$
		private static final String FAILURE_ASSERTION_LEFT = "left: "; //$NON-NLS-1$
		private static final String FAILURE_ASSERTION_RIGHT = "right:"; //$NON-NLS-1$
		private static final String FAILURE_ASSERTION_SEPARATOR = ","; //$NON-NLS-1$
		private static final String FAILURE_ASSERTION_END = "',"; //$NON-NLS-1$
		private static final String NL = "\n"; //$NON-NLS-1$

		private FailureTrace fillFailureTrace(String trace) {
			// thread 'tests::it_fails' panicked at 'assertion failed: `(left == right)`
			// left: `4`,
			// right: `5`', tests/testfoo.rs:12:9
			if (trace.indexOf(FAILURE_ASSERTION_BEGIN) != -1) {
				int assertion = trace.indexOf(FAILURE_ASSERTION_BEGIN);
				int leftIndex = trace.indexOf(FAILURE_ASSERTION_LEFT, assertion);
				int rightIndex = trace.indexOf(FAILURE_ASSERTION_RIGHT, assertion);
				int assertionEmd = trace.lastIndexOf(FAILURE_ASSERTION_END);
				if (assertion != -1 && leftIndex != -1 && rightIndex != -1 && assertionEmd != -1) {
					String assertionText = trace.substring(0, assertionEmd);
					String leftValue = assertionText.substring(leftIndex + FAILURE_ASSERTION_LEFT.length(),
							assertionText.lastIndexOf(FAILURE_ASSERTION_SEPARATOR, rightIndex)).strip();
					String rightValue = assertionText.substring(rightIndex + FAILURE_ASSERTION_RIGHT.length()).strip();
					String source = trace.substring(assertionEmd + FAILURE_ASSERTION_END.length()).strip();
					StringBuilder sb = new StringBuilder();
					sb.append(assertionText).append(NL).append(TestViewSupport.FRAME_PREFIX).append(source); // $NON-NLS-1$
					return new FailureTrace(sb.toString(), leftValue, rightValue);
				}
			}

			return new FailureTrace(trace, null, null);
		}

		@Override
		ProcessingState readMessage(String message) {
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

	ProcessingState fDefaultState = new DefaultProcessingState();
	ProcessingState fTraceState = new TraceProcessingState();
	ProcessingState fCurrentState = fDefaultState;

	public CargoTestRunnerClient(ITestRunSession session) {
		this.session = session;
		this.process = connectProcess(session.getLaunch());
	}

	private IProcess connectProcess(ILaunch launch) {
		if (this.process != null) {
			return this.process;
		}
		this.process = launch.getProcesses()[0];
		if (this.process != null && this.inputStream == null) {
			inputStream = toInputStream(process);
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
		IStreamMonitor osMonitor = process.getStreamsProxy().getOutputStreamMonitor();
		IStreamMonitor esMonitor = process.getStreamsProxy().getErrorStreamMonitor();

		if (osMonitor == null && esMonitor == null) {
			return null;
		}
		List<Integer> content = Collections.synchronizedList(new LinkedList<>());
		osMonitor.addListener((text, progresMonitor) -> text.chars().forEach(content::add));
		esMonitor.addListener((text, progresMonitor) -> text.chars().forEach(content::add));
		if (esMonitor != null) {
			byte[] initialContent = esMonitor.getContents().getBytes();
			for (int i = initialContent.length - 1; i >= 0; i--) {
				content.add(0, Integer.valueOf(initialContent[i]));
			}
		}
		if (osMonitor != null) {
			byte[] initialContent = osMonitor.getContents().getBytes();
			for (int i = initialContent.length - 1; i >= 0; i--) {
				content.add(0, Integer.valueOf(initialContent[i]));
			}
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
		session.notifyTestRunStarted(0);
		fExecutedTests.clear();
		try (InputStreamReader isReader = new InputStreamReader(iStream, StandardCharsets.UTF_8);
				BufferedReader reader = new BufferedReader(isReader);
				PushbackReader pushbackReader = new PushbackReader(reader)) {

			String message;
			while (pushbackReader != null && (message = readMessage(pushbackReader)) != null) {
				receiveMessage(message);
			}
		} catch (IOException e) {
			CorrosionPlugin.logError(e);
			session.notifyTestRunTerminated();
		}
		fExecutedTests.clear();
		fRootTestSuiteStack.clear();
		shutDown();
	}

	private String fLastLineDelimiter = "\n"; //$NON-NLS-1$

	private String readMessage(PushbackReader in) throws IOException {
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
		if (buf.length() == 0)
			return null;
		return buf.toString();
	}

	public void receiveMessage(String message) {
		fCurrentState = fCurrentState.readMessage(message);
	}

	@Override
	public void stopTest() {
		try {
			inputStream.close();
		} catch (IOException e) {
			CorrosionPlugin.logError(e);
		}
	}

	@Override
	public boolean isRunning() {
		try {
			return inputStream.available() > 0;
		} catch (IOException e) {
			return false;
		}
	}

	@Override
	public void stopWaiting() {
		// TODO Auto-generated method stub

	}

	@Override
	public void shutDown() {
		try {
			inputStream.close();
		} catch (IOException e) {
			CorrosionPlugin.logError(e);
		}

	}

	private ITestSuiteElement getTestSuite(String parentId) {
		ITestElement element = session.getTestElement(parentId);
		return element instanceof ITestSuiteElement ? (ITestSuiteElement) element : null;
	}
}
