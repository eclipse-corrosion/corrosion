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

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;

/**
 * The test program processes of a Cargo test launch in debug mode. In debug
 * mode, GDB starts each test program in its own launch. The unit test view
 * reads the output of these processes, one after the other.
 */
final class DebuggedTestPrograms {

	private static final Map<ILaunch, BlockingQueue<Optional<IProcess>>> QUEUES = new ConcurrentHashMap<>();

	private DebuggedTestPrograms() {
	}

	/** Adds the next test program process of the Cargo test launch. */
	static void add(ILaunch testLaunch, IProcess process) {
		queue(testLaunch).add(Optional.of(process));
	}

	/** Tells that no more test programs come for the Cargo test launch. */
	static void done(ILaunch testLaunch) {
		queue(testLaunch).add(Optional.empty());
	}

	/**
	 * Waits for the next test program process of the Cargo test launch.
	 *
	 * @return the process, or null if no more test programs come
	 */
	static IProcess next(ILaunch testLaunch) throws InterruptedException {
		Optional<IProcess> next = queue(testLaunch).take();
		if (next.isEmpty()) {
			QUEUES.remove(testLaunch);
			return null;
		}
		return next.get();
	}

	private static BlockingQueue<Optional<IProcess>> queue(ILaunch testLaunch) {
		return QUEUES.computeIfAbsent(testLaunch, launch -> new LinkedBlockingQueue<>());
	}
}
