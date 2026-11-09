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
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.IProcess;
import org.junit.jupiter.api.Test;

class DebuggedTestProgramsTest {

	@Test
	void testProcessesComeInOrderUntilDone() throws Exception {
		ILaunch launch = new Launch(null, ILaunchManager.DEBUG_MODE, null);
		IProcess first = process(launch);
		IProcess second = process(launch);
		DebuggedTestPrograms.add(launch, first);
		DebuggedTestPrograms.add(launch, second);
		DebuggedTestPrograms.done(launch);

		assertSame(first, DebuggedTestPrograms.next(launch));
		assertSame(second, DebuggedTestPrograms.next(launch));
		assertNull(DebuggedTestPrograms.next(launch));
	}

	@Test
	void testReaderWaitsForTheNextProcess() throws Exception {
		ILaunch launch = new Launch(null, ILaunchManager.DEBUG_MODE, null);
		CompletableFuture<IProcess> next = CompletableFuture.supplyAsync(() -> {
			try {
				return DebuggedTestPrograms.next(launch);
			} catch (InterruptedException e) {
				throw new IllegalStateException(e);
			}
		});
		IProcess process = process(launch);
		DebuggedTestPrograms.add(launch, process);
		assertSame(process, next.get(5, TimeUnit.SECONDS));
		DebuggedTestPrograms.done(launch);
		assertNull(DebuggedTestPrograms.next(launch));
	}

	@Test
	void testLaunchesAreSeparate() throws Exception {
		ILaunch launch1 = new Launch(null, ILaunchManager.DEBUG_MODE, null);
		ILaunch launch2 = new Launch(null, ILaunchManager.DEBUG_MODE, null);
		IProcess process = process(launch1);
		DebuggedTestPrograms.done(launch2);
		DebuggedTestPrograms.add(launch1, process);
		DebuggedTestPrograms.done(launch1);

		assertNull(DebuggedTestPrograms.next(launch2));
		assertSame(process, DebuggedTestPrograms.next(launch1));
		assertNull(DebuggedTestPrograms.next(launch1));
	}

	@Test
	void testStreamStaysAtItsEnd() throws Exception {
		ILaunch launch = new Launch(null, ILaunchManager.DEBUG_MODE, null);
		InputStream stream = CargoTestRunnerClient.debuggedTestProgramsStream(launch);
		DebuggedTestPrograms.done(launch);
		assertEquals(-1, stream.read());
		// The unit test view can read again after the end. It must get the end again,
		// and not wait for a next test program that does not come.
		assertEquals(-1, CompletableFuture.supplyAsync(() -> {
			try {
				return stream.read(new byte[10], 0, 10);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}).get(5, TimeUnit.SECONDS));
	}

	/** A process object of the launch, only to put in the queue */
	private static IProcess process(ILaunch launch) {
		return (IProcess) Proxy.newProxyInstance(DebuggedTestProgramsTest.class.getClassLoader(),
				new Class<?>[] { IProcess.class }, (proxy, method, args) -> switch (method.getName()) {
				case "getLaunch" -> launch; //$NON-NLS-1$
				case "hashCode" -> System.identityHashCode(proxy); //$NON-NLS-1$
				case "equals" -> proxy == args[0]; //$NON-NLS-1$
				case "toString" -> "test process"; //$NON-NLS-1$ //$NON-NLS-2$
				default -> null;
				});
	}
}
