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

import java.util.Collection;
import java.util.List;

import org.eclipse.core.text.StringMatcher;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.IViewPart;
import org.eclipse.unittest.launcher.ITestRunnerClient;
import org.eclipse.unittest.model.ITestCaseElement;
import org.eclipse.unittest.model.ITestElement;
import org.eclipse.unittest.model.ITestRunSession;
import org.eclipse.unittest.model.ITestSuiteElement;
import org.eclipse.unittest.ui.ITestViewSupport;

public class TestViewSupport implements ITestViewSupport {

	public static final String FRAME_PREFIX = " at "; //$NON-NLS-1$

	@Override
	public ITestRunnerClient newTestRunnerClient(ITestRunSession session) {
		return new CargoTestRunnerClient(session);
	}

	@Override
	public IAction getOpenTestAction(IViewPart testRunnerPart, ITestCaseElement testCase) {
		return null;
	}

	@Override
	public IAction getOpenTestAction(IViewPart testRunnerPart, ITestSuiteElement testSuite) {
		return null;
	}

	@Override
	public IAction createOpenEditorAction(IViewPart testRunnerPart, ITestElement failure, String traceLine) {
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ILaunchConfiguration getRerunLaunchConfiguration(List<ITestElement> testElements) {
		// TODO Auto-generated method stub
		return null;
	}

}
