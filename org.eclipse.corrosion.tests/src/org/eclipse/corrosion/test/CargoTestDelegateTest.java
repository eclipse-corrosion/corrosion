/*********************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.corrosion.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class CargoTestDelegateTest {

	@Test
	void testExecutableOfTestArtifact() {
		String line = """
				{"reason":"compiler-artifact","package_id":"path+file:///work/functions#0.1.0",\
				"target":{"kind":["bin"],"name":"functions"},\
				"profile":{"opt_level":"0","debuginfo":2,"test":true},\
				"filenames":["/work/functions/target/debug/deps/functions-6d597759b8187556"],\
				"executable":"/work/functions/target/debug/deps/functions-6d597759b8187556","fresh":true}"""; //$NON-NLS-1$
		assertEquals("/work/functions/target/debug/deps/functions-6d597759b8187556", //$NON-NLS-1$
				CargoTestDelegate.testExecutable(line));
	}

	@Test
	void testArtifactThatIsNotATest() {
		String line = """
				{"reason":"compiler-artifact","profile":{"test":false},\
				"executable":"/work/functions/target/debug/functions","fresh":true}"""; //$NON-NLS-1$
		assertNull(CargoTestDelegate.testExecutable(line));
	}

	@Test
	void testTestArtifactWithoutExecutable() {
		String line = """
				{"reason":"compiler-artifact","profile":{"test":true},"executable":null}"""; //$NON-NLS-1$
		assertNull(CargoTestDelegate.testExecutable(line));
	}

	@Test
	void testOtherMessages() {
		assertNull(CargoTestDelegate.testExecutable("{\"reason\":\"build-finished\",\"success\":true}")); //$NON-NLS-1$
		assertNull(CargoTestDelegate.testExecutable("   Compiling functions v0.1.0")); //$NON-NLS-1$
		assertNull(CargoTestDelegate.testExecutable("")); //$NON-NLS-1$
	}
}
