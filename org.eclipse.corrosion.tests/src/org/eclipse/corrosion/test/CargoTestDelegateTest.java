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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.corrosion.test.CargoTestDelegate.DebugRunProcess;
import org.eclipse.corrosion.test.CargoTestDelegate.TestProgram;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.junit.jupiter.api.Test;

class CargoTestDelegateTest {

	@Test
	void testProgramOfTestArtifact() {
		String line = """
				{"reason":"compiler-artifact","package_id":"path+file:///work/ws/functions#0.1.0",\
				"manifest_path":"/work/ws/functions/Cargo.toml",\
				"target":{"kind":["bin"],"name":"functions"},\
				"profile":{"opt_level":"0","debuginfo":2,"test":true},\
				"filenames":["/work/ws/target/debug/deps/functions-6d597759b8187556"],\
				"executable":"/work/ws/target/debug/deps/functions-6d597759b8187556","fresh":true}"""; //$NON-NLS-1$
		TestProgram program = CargoTestDelegate.testProgram(line);
		assertEquals("/work/ws/target/debug/deps/functions-6d597759b8187556", program.executable()); //$NON-NLS-1$
		// Cargo starts a test program in the folder of its package
		assertEquals("/work/ws/functions", program.directory()); //$NON-NLS-1$
		// The target does not have the hash, which changes when Cargo builds again
		assertEquals("functions bin", program.target()); //$NON-NLS-1$
	}

	@Test
	void testProgramWithoutManifestPath() {
		String line = """
				{"reason":"compiler-artifact","profile":{"test":true},"executable":"/work/target/debug/deps/a-1"}"""; //$NON-NLS-1$
		TestProgram program = CargoTestDelegate.testProgram(line);
		assertEquals("/work/target/debug/deps/a-1", program.executable()); //$NON-NLS-1$
		assertNull(program.directory());
		// Without the target, the file name of the program
		assertEquals("a-1", program.target()); //$NON-NLS-1$
	}

	@Test
	void testArtifactThatIsNotATest() {
		String line = """
				{"reason":"compiler-artifact","profile":{"test":false},\
				"executable":"/work/functions/target/debug/functions","fresh":true}"""; //$NON-NLS-1$
		assertNull(CargoTestDelegate.testProgram(line));
	}

	@Test
	void testTestArtifactWithoutExecutable() {
		String line = """
				{"reason":"compiler-artifact","profile":{"test":true},"executable":null}"""; //$NON-NLS-1$
		assertNull(CargoTestDelegate.testProgram(line));
	}

	@Test
	void testDebugRunKeepsTheLaunchActiveUntilItEnds() {
		ILaunch launch = new Launch(null, ILaunchManager.DEBUG_MODE, null);
		DebugRunProcess debugRun = new DebugRunProcess(launch);
		launch.addProcess(debugRun);
		// Eclipse removes terminated launches when it adds the debug launch of a test
		// program, thus the test launch must not be terminated during the debug run
		assertFalse(launch.isTerminated());
		assertTrue(debugRun.canTerminate());
		assertNull(debugRun.getStreamsProxy(), "the debug run has no console"); //$NON-NLS-1$

		debugRun.terminate();
		assertTrue(debugRun.isTerminated());
		assertFalse(debugRun.canTerminate());
		assertTrue(launch.isTerminated());
	}

	@Test
	void testOtherMessages() {
		assertNull(CargoTestDelegate.testProgram("{\"reason\":\"build-finished\",\"success\":true}")); //$NON-NLS-1$
		assertNull(CargoTestDelegate.testProgram("   Compiling functions v0.1.0")); //$NON-NLS-1$
		assertNull(CargoTestDelegate.testProgram("")); //$NON-NLS-1$
	}
}
