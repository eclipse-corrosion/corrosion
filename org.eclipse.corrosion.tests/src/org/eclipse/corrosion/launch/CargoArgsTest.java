/*********************************************************************
 * Copyright (c) 2019, 2021 Fraunhofer FOKUS and others.
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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class CargoArgsTest {

	private static final String SEPARATOR = "--";

	@Test
	void testOnlyCommand() {
		String command = "fmt";
		CargoArgs result = CargoArgs.fromAllArguments(command);
		assertEquals(command, result.command);
		assertTrue(result.arguments.isEmpty());
		assertTrue(result.options.isEmpty());
	}

	@Test
	void testOnlyCommandAndSeparator() {
		String command = "fmt";
		CargoArgs result = CargoArgs.fromAllArguments(command, SEPARATOR);
		assertEquals(command, result.command);
		assertTrue(result.arguments.isEmpty());
		assertTrue(result.options.isEmpty());
	}

	@Test
	void testOnlyCommandAndOptions() {
		String command = "build";
		String opt1 = "--all";
		String opt2 = "--release";
		CargoArgs result = CargoArgs.fromAllArguments(command, opt1, opt2);
		assertEquals(command, result.command);
		assertEquals(Arrays.asList(opt1, opt2), result.options);
		assertTrue(result.arguments.isEmpty());
	}

	@Test
	void testOnlyCommandAndOptionsAndSeparator() {
		String command = "build";
		String opt1 = "--all";
		String opt2 = "--release";
		CargoArgs result = CargoArgs.fromAllArguments(command, opt1, opt2, SEPARATOR);
		assertEquals(command, result.command);
		assertEquals(Arrays.asList(opt1, opt2), result.options);
		assertTrue(result.arguments.isEmpty());
	}

	@Test
	void testOnlyCommandAndArguments() {
		String command = "build";
		List<String> arguments = List.of("--nocapture", "my_test");

		List<String> all_arguments = new ArrayList<>();
		all_arguments.add(command);
		all_arguments.add(SEPARATOR);
		all_arguments.addAll(arguments);
		String[] input = all_arguments.toArray(String[]::new);

		CargoArgs result = CargoArgs.fromAllArguments(input);

		assertEquals(command, result.command);
		assertTrue(result.options.isEmpty());
		assertEquals(arguments, result.arguments);
	}

	@Test
	void testOnlyCommandAndOptionsAndArguments() {
		String command = "build";
		List<String> options = List.of("--all", "--release");
		List<String> arguments = List.of("--nocapture", "my_test");

		List<String> all_arguments = new ArrayList<>();
		all_arguments.add(command);
		all_arguments.addAll(options);
		all_arguments.add(SEPARATOR);
		all_arguments.addAll(arguments);
		String[] input = all_arguments.toArray(String[]::new);

		CargoArgs result = CargoArgs.fromAllArguments(input);

		assertEquals(command, result.command);
		assertEquals(options, result.options);
		assertEquals(arguments, result.arguments);
	}

	@Test
	void testOnlyCommandAndOptionsAndArgumentsIncludingSeparator() {
		String command = "build";
		List<String> options = List.of("--all", "--release");
		List<String> arguments = List.of("--nocapture", SEPARATOR, "my_test");

		List<String> all_arguments = new ArrayList<>();
		all_arguments.add(command);
		all_arguments.addAll(options);
		all_arguments.add(SEPARATOR);
		all_arguments.addAll(arguments);
		String[] input = all_arguments.toArray(String[]::new);

		CargoArgs result = CargoArgs.fromAllArguments(input);

		assertEquals(command, result.command);
		assertEquals(options, result.options);
		assertEquals(arguments, result.arguments);
	}
}
