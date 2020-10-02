/*********************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************/
package org.eclipse.corrosion.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.text.StringMatcher;
import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.corrosion.launch.RustLaunchDelegateTools;
import org.eclipse.corrosion.test.actions.OpenEditorAtLineAction;
import org.eclipse.corrosion.test.actions.OpenTestAction;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.unittest.launcher.ITestRunnerClient;
import org.eclipse.unittest.model.ITestCaseElement;
import org.eclipse.unittest.model.ITestElement;
import org.eclipse.unittest.model.ITestRunSession;
import org.eclipse.unittest.model.ITestSuiteElement;
import org.eclipse.unittest.ui.ITestViewSupport;

public class CargoTestViewSupport implements ITestViewSupport {

	public static final String FRAME_PREFIX = " at "; //$NON-NLS-1$

	@Override
	public ITestRunnerClient newTestRunnerClient(ITestRunSession session) {
		return new CargoTestRunnerClient(session);
	}

	@Override
	public IAction getOpenTestAction(Shell shell, ITestCaseElement testCase) {
		return new OpenTestAction(shell, testCase);
	}

	@Override
	public IAction getOpenTestAction(Shell shell, ITestSuiteElement testSuite) {
		return new OpenTestAction(shell, testSuite);
	}

	@Override
	public IAction createOpenEditorAction(Shell shell, ITestElement failure, String traceLine) {
		try {
			int indexOfFramePrefix = traceLine.indexOf(FRAME_PREFIX);
			if (indexOfFramePrefix == -1) {
				return null;
			}
			int columNumberIndex = traceLine.lastIndexOf(':');
			int lineNumberIndex = traceLine.lastIndexOf(':', columNumberIndex - 1);
			String testName = traceLine
					.substring(traceLine.indexOf(FRAME_PREFIX) + FRAME_PREFIX.length(), lineNumberIndex).trim();

			String lineNumber = traceLine.substring(lineNumberIndex + 1, columNumberIndex).trim();
			int line = Integer.parseInt(lineNumber);
			return new OpenEditorAtLineAction(shell, testName, failure.getTestRunSession(), line);
		} catch (NumberFormatException | IndexOutOfBoundsException e) {
			CorrosionPlugin.logError(e);
		}
		return null;
	}

	@Override
	public Runnable createShowStackTraceInConsoleViewActionDelegate(ITestElement failedTest) {
		return null;
	}

	@Override
	public String getDisplayName() {
		return "Cargo"; //$NON-NLS-1$
	}

	@Override
	public Collection<StringMatcher> getTraceExclusionFilterPatterns() {
		return Collections.emptySet();
	}

	private static final String EXACT_FLAG = "--exact"; //$NON-NLS-1$
	private static final String TEST_FLAG = "--test"; //$NON-NLS-1$

	@Override
	public ILaunchConfiguration getRerunLaunchConfiguration(List<ITestElement> testElements) {
		if (testElements.isEmpty()) {
			return null;
		}
		ILaunchConfiguration origin = testElements.get(0).getTestRunSession().getLaunch().getLaunchConfiguration();
		ILaunchConfigurationWorkingCopy res;
		try {
			res = origin.copy(origin.getName() + "\uD83D\uDD03"); //$NON-NLS-1$

			ArrayList<String> list = (ArrayList<String>) testElements.stream().map(CargoTestViewSupport::packTestPaths)
					.collect(Collectors.toList());

			String testName = ""; //$NON-NLS-1$
			StringBuilder attributes = new StringBuilder();

			// in case of Zero-elements length both testName and attributes are to be empty
			if (list.size() == 1) {
				testName = list.get(0);
				attributes.append(EXACT_FLAG);
				System.out
						.println("ReRun ONE test: name: [" + testName + "], options: [" + attributes.toString() + "]"); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
			} else if (list.size() > 1) {
				attributes.append(TEST_FLAG);
				for (String v : list) {
					attributes.append(' ').append(v.trim());
				}
				System.out.println("ReRun MULTIPLE (" + list.size() + ") tests: name: [" + testName + "], options: [" //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
						+ attributes.toString() + "]"); //$NON-NLS-1$
			} else {
				System.out
						.println("ReRun ALL tests: name: [" + testName + "], options: [" + attributes.toString() + "]"); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
			}

			res.setAttribute(CargoTestDelegate.TEST_NAME_ATTRIBUTE, testName);
			res.setAttribute(RustLaunchDelegateTools.ARGUMENTS_ATTRIBUTE, attributes.toString());
			return res;
		} catch (CoreException e) {
			CorrosionPlugin.logError(e);
			return null;
		}
	}

	/**
	 * Pack the paths to specified test items to string list.
	 *
	 * @param testElement test element to pack
	 *
	 * @return string list
	 */
	private static String packTestPaths(ITestElement testElement) {
		if (testElement instanceof ITestCaseElement) {
			return testElement.getTestName();
		} else if (testElement instanceof ITestSuiteElement) {
			if (testElement.getParent() != null && !(testElement.getParent() instanceof ITestRunSession)
					&& !(testElement.getParent().getParent() instanceof ITestRunSession)) {
				return testElement.getTestName();
			}
		}
		return ""; //$NON-NLS-1$ // Re-Run everything
	}
}
