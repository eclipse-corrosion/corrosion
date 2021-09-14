/*********************************************************************
 * Copyright (c) 2018 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************/
package org.eclipse.corrosion.tests;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.eclipse.corrosion.ErrorLineMatcher;
import org.eclipse.ui.console.IConsoleDocumentPartitioner;
import org.eclipse.ui.console.PatternMatchEvent;
import org.eclipse.ui.console.TextConsole;
import org.junit.jupiter.api.Test;

class TestErrorLineMatcher {

	private static class MockTextConsole extends TextConsole {

		public MockTextConsole(String content) {
			super("name", "consoleType", null, false);
			getDocument().set(content);
		}

		@Override
		protected IConsoleDocumentPartitioner getPartitioner() {
			return null;
		}
	}

	@Test
	void testColonInFileName() throws Exception {
		MockTextConsole console = new MockTextConsole("--> C:\\coding\\rust\\checkbook\\src\\data_types.rs:2:2");
		PatternMatchEvent event = new PatternMatchEvent(console, 4, console.getDocument().getLength() - 4);
		ErrorLineMatcher errorLineMatcher = new ErrorLineMatcher();
		errorLineMatcher.connect(console);
		errorLineMatcher.matchFound(event);
		assertNotEquals(0, console.getHyperlinks().length);
	}

}
