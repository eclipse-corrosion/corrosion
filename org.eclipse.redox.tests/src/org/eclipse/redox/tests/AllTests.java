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
package org.eclipse.redox.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ TestIDEIntegration.class, TestSyntaxHighlighting.class, TestLSPIntegration.class,
		TestNewCargoProjectWizard.class, TestRunConfiguration.class, TestExportCargoProjectWizard.class,
		TestBuilder.class, TestPerspective.class, TestLSPExtensions.class })
public class AllTests {

}
