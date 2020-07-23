/*********************************************************************
 * Copyright (c) 2019 Fraunhofer FOKUS and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Max Bureck (Fraunhofer FOKUS) - Initial implementation
 *******************************************************************************/
package org.eclipse.corrosion.launch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.lsp4j.Command;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class RLSRunCommandTest {

	private static final String TITLE = "Run test";
	private static final String COMMAND_ID = "rls.run";

	// Test data
	private static final String VALID_BINARY = "cargo";
	private static final List<String> VALID_ARGS = List.of("--release");
	private static final List<String> EMPTY_ARGS = Collections.emptyList();
	private static final Map<String, String> VALID_ENV = Collections.singletonMap("RUST_BACKTRACE", "short");
	private static final Map<String, String> EMPTY_ENV = Collections.emptyMap();

	static Stream<Arguments> parameters() {
		return Stream.of(Arguments.arguments(createArgument(VALID_BINARY, null, null)),
				Arguments.arguments(createArgument(null, VALID_ARGS, null)),
				Arguments.arguments(createArgument(null, null, VALID_ENV)),
				Arguments.arguments(createArgument(VALID_BINARY, VALID_ARGS, null)),
				Arguments.arguments(createArgument(null, VALID_ARGS, VALID_ENV)),
				Arguments.arguments(createArgument(VALID_BINARY, null, VALID_ENV)));
	}

	@ParameterizedTest
	@MethodSource("parameters")
	public void testMapEntryArgument(Map<String, Object> argument) {
		Command command = new Command(TITLE, COMMAND_ID, Arrays.asList(argument));
		Optional<RLSRunCommand> lspCommand = RLSRunCommand.fromLSPCommand(command);
		assertFalse(lspCommand.isPresent());
	}

	@Test
	public void testNoArgument() {
		Command command = new Command(TITLE, COMMAND_ID, Arrays.asList());
		Optional<RLSRunCommand> lspCommand = RLSRunCommand.fromLSPCommand(command);
		assertFalse(lspCommand.isPresent());
	}

	@Test
	public void testArgumentListNull() {
		Command command = new Command(TITLE, COMMAND_ID, null);
		Optional<RLSRunCommand> lspCommand = RLSRunCommand.fromLSPCommand(command);
		assertFalse(lspCommand.isPresent());
	}

	@Test
	public void testArgumentListWrongEntry() {
		Command command = new Command(TITLE, COMMAND_ID, Arrays.asList("foo"));
		Optional<RLSRunCommand> lspCommand = RLSRunCommand.fromLSPCommand(command);
		assertFalse(lspCommand.isPresent());
	}

	@Test
	public void testEmptyArguments() {
		Command command = new Command(TITLE, COMMAND_ID,
				Arrays.asList(createArgument(VALID_BINARY, EMPTY_ARGS, EMPTY_ENV)));
		Optional<RLSRunCommand> lspCommand = RLSRunCommand.fromLSPCommand(command);

		assertTrue(lspCommand.isPresent());
		RLSRunCommand runCommand = lspCommand.get();
		assertEquals(0, runCommand.args.length);
		assertTrue(runCommand.env.isEmpty());
	}

	@Test
	public void testValidArguments() {
		Command command = new Command(TITLE, COMMAND_ID,
				Arrays.asList(createArgument(VALID_BINARY, VALID_ARGS, VALID_ENV)));
		Optional<RLSRunCommand> lspCommand = RLSRunCommand.fromLSPCommand(command);

		assertTrue(lspCommand.isPresent());
		RLSRunCommand runCommand = lspCommand.get();
		assertEquals(VALID_ARGS, Arrays.asList(runCommand.args));
		assertEquals(VALID_ENV, runCommand.env);
	}

	@Test
	public void testInvalidEnvEntry() {
		Map<String, String> env = new HashMap<>();
		env.put("INVALID", null);
		env.put("RUST_BACKTRACE", "short");
		env.put(null, "INVALID_KEY");

		Command command = new Command(TITLE, COMMAND_ID, Arrays.asList(createArgument(VALID_BINARY, VALID_ARGS, env)));
		Optional<RLSRunCommand> lspCommand = RLSRunCommand.fromLSPCommand(command);

		assertTrue(lspCommand.isPresent());
		RLSRunCommand runCommand = lspCommand.get();
		assertEquals(VALID_ENV, runCommand.env);
	}

	private static Map<String, Object> createArgument(String binary, List<String> arguments,
			Map<String, String> environment) {
		Map<String, Object> result = new HashMap<>();

		if (binary != null) {
			result.put("binary", binary);
		}

		if (arguments != null) {
			result.put("args", arguments);
		}

		if (environment != null) {
			result.put("env", environment);
		}

		return result;
	}

}
