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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.lsp4j.Command;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

public class RLSRunCommandTest {

	private static final String TITLE = "Run test";
	private static final String COMMAND_ID = "rls.run";

	// Test data
	private static final String VALID_BINARY = "cargo";
	private static final List<String> VALID_ARGS = Arrays.asList("--release");
	private static final List<String> EMPTY_ARGS = Collections.emptyList();
	private static final Map<String, String> VALID_ENV = Collections.singletonMap("RUST_BACKTRACE", "short");
	private static final Map<String, String> EMPTY_ENV = Collections.emptyMap();

	@RunWith(Parameterized.class)
	public static class MissingField {
		private String binary;
		private List<String> arguments;
		private Map<String, String> environment;

		public MissingField(String binary, List<String> arguments, Map<String, String> environment) {
			super();
			this.binary = binary;
			this.arguments = arguments;
			this.environment = environment;
		}

		@Parameters
		public static Collection<Object[]> parameters() {
			return Arrays.asList(new Object[][] { 
				{ VALID_BINARY, null, null }, 
				{ null, VALID_ARGS, null },
				{ null, null, VALID_ENV }, 
				{ VALID_BINARY, VALID_ARGS, null }, 
				{ null, VALID_ARGS, VALID_ENV },
				{ VALID_BINARY, null, VALID_ENV }, 
			});
		}

		@Test
		public void testMapEntryArgument() {
			Map<String, Object> argument = createArgument(binary, arguments, environment);
			Command command = new Command(TITLE, COMMAND_ID, Arrays.asList(argument));
			Optional<RLSRunCommand> lspCommand = RLSRunCommand.fromLSPCommand(command);
			assertFalse(lspCommand.isPresent());
		}
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
