/*********************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.corrosion.launch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.eclipse.lsp4j.Position;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class TestAtCursorTest {

	/**
	 * A reply of rust-analyzer to "experimental/runnables" for a file with a
	 * "tests" module (lines 4 to 17) and the test "adds_two_numbers" (lines 8 to
	 * 11).
	 */
	private static final String RUNNABLES = """
			[
			 {"label": "test-mod tests",
			  "location": {"targetRange": {"start": {"line": 4, "character": 0}, "end": {"line": 17, "character": 1}}},
			  "kind": "cargo",
			  "args": {"cargoArgs": ["test", "--package", "ratest", "--lib"],
			           "executableArgs": ["tests", "--nocapture"]}},
			 {"label": "test tests::adds_two_numbers",
			  "location": {"targetRange": {"start": {"line": 8, "character": 4}, "end": {"line": 11, "character": 5}}},
			  "kind": "cargo",
			  "args": {"cargoArgs": ["test", "--package", "ratest", "--lib"],
			           "executableArgs": ["tests::adds_two_numbers", "--exact", "--nocapture", "--include-ignored"]}},
			 {"label": "cargo test -p ratest --all-targets",
			  "kind": "cargo",
			  "args": {"cargoArgs": ["test", "--package", "ratest", "--all-targets"], "executableArgs": []}}
			]
			"""; //$NON-NLS-1$

	@Test
	void testFindsTestAtCursor() {
		Optional<TestAtCursor> test = TestAtCursor.fromRunnables(JsonParser.parseString(RUNNABLES),
				new Position(9, 10));
		assertTrue(test.isPresent());
		assertEquals("tests::adds_two_numbers", test.get().name());
		assertEquals("--package ratest --lib", test.get().options());
		// --nocapture is removed, so that the unit test view can read the output
		assertEquals("--exact --include-ignored", test.get().arguments());
	}

	@Test
	void testCursorOnLastLineOfTest() {
		Optional<TestAtCursor> test = TestAtCursor.fromRunnables(JsonParser.parseString(RUNNABLES),
				new Position(11, 0));
		assertEquals("tests::adds_two_numbers", test.map(TestAtCursor::name).orElse(null));
	}

	@Test
	void testCursorInModuleButNotInTest() {
		// A "test-mod" runnable is not a single test
		assertTrue(TestAtCursor.fromRunnables(JsonParser.parseString(RUNNABLES), new Position(14, 0)).isEmpty());
	}

	@Test
	void testCursorOutsideOfTests() {
		assertTrue(TestAtCursor.fromRunnables(JsonParser.parseString(RUNNABLES), new Position(0, 0)).isEmpty());
	}

	@Test
	void testNoRunnables() {
		assertTrue(TestAtCursor.fromRunnables(JsonParser.parseString("[]"), new Position(9, 10)).isEmpty()); //$NON-NLS-1$
		assertTrue(TestAtCursor.fromRunnables(JsonNull.INSTANCE, new Position(9, 10)).isEmpty());
		assertTrue(TestAtCursor.fromRunnables(new JsonObject(), new Position(9, 10)).isEmpty());
	}

	@Test
	void testIgnoresRunnableThatIsNotCargoTest() {
		String runnables = """
				[{"label": "test tests::adds",
				  "location": {"targetRange": {"start": {"line": 8, "character": 4}, "end": {"line": 11, "character": 5}}},
				  "args": {"cargoArgs": ["run", "--package", "ratest"], "executableArgs": ["tests::adds"]}}]
				"""; //$NON-NLS-1$
		assertTrue(TestAtCursor.fromRunnables(JsonParser.parseString(runnables), new Position(9, 10)).isEmpty());
	}
}
