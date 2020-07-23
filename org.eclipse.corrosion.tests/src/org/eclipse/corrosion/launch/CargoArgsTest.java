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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

public class CargoArgsTest {

	private static final String SEPARATOR = "--";

	@Test
	public void testOnlyCommand() {
		String command = "fmt";
		String[] input = { command };
		CargoArgs result = CargoArgs.fromAllArguments(input);
		assertEquals(command, result.command);
		assertTrue(result.arguments.isEmpty());
		assertTrue(result.options.isEmpty());
	}

	@Test
	public void testOnlyCommandAndSeparator() {
		String command = "fmt";
		String[] input = { command, SEPARATOR };
		CargoArgs result = CargoArgs.fromAllArguments(input);
		assertEquals(command, result.command);
		assertTrue(result.arguments.isEmpty());
		assertTrue(result.options.isEmpty());
	}

	@Test
	public void testOnlyCommandAndOptions() {
		String command = "build";
		String opt1 = "--all";
		String opt2 = "--release";
		String[] input = { command, opt1, opt2 };
		CargoArgs result = CargoArgs.fromAllArguments(input);
		assertEquals(command, result.command);
		assertEquals(Arrays.asList(opt1, opt2), result.options);
		assertTrue(result.arguments.isEmpty());
	}

	@Test
	public void testOnlyCommandAndOptionsAndSeparator() {
		String command = "build";
		String opt1 = "--all";
		String opt2 = "--release";
		String[] input = { command, opt1, opt2, SEPARATOR };
		CargoArgs result = CargoArgs.fromAllArguments(input);
		assertEquals(command, result.command);
		assertEquals(Arrays.asList(opt1, opt2), result.options);
		assertTrue(result.arguments.isEmpty());
	}

	@Test
	public void testOnlyCommandAndArguments() {
		String command = "build";
		List<String> arguments = Arrays.asList("--nocapture", "my_test");

		List<String> all_arguments = new ArrayList<>();
		all_arguments.add(command);
		all_arguments.add(SEPARATOR);
		all_arguments.addAll(arguments);
		String[] input = all_arguments.toArray(new String[] {});

		CargoArgs result = CargoArgs.fromAllArguments(input);

		assertEquals(command, result.command);
		assertTrue(result.options.isEmpty());
		assertEquals(arguments, result.arguments);
	}

	@Test
	public void testOnlyCommandAndOptionsAndArguments() {
		String command = "build";
		List<String> options = Arrays.asList("--all", "--release");
		List<String> arguments = Arrays.asList("--nocapture", "my_test");

		List<String> all_arguments = new ArrayList<>();
		all_arguments.add(command);
		all_arguments.addAll(options);
		all_arguments.add(SEPARATOR);
		all_arguments.addAll(arguments);
		String[] input = all_arguments.toArray(new String[] {});

		CargoArgs result = CargoArgs.fromAllArguments(input);

		assertEquals(command, result.command);
		assertEquals(options, result.options);
		assertEquals(arguments, result.arguments);
	}

	@Test
	public void testOnlyCommandAndOptionsAndArgumentsIncludingSeparator() {
		String command = "build";
		List<String> options = Arrays.asList("--all", "--release");
		List<String> arguments = Arrays.asList("--nocapture", SEPARATOR, "my_test");

		List<String> all_arguments = new ArrayList<>();
		all_arguments.add(command);
		all_arguments.addAll(options);
		all_arguments.add(SEPARATOR);
		all_arguments.addAll(arguments);
		String[] input = all_arguments.toArray(new String[] {});

		CargoArgs result = CargoArgs.fromAllArguments(input);

		assertEquals(command, result.command);
		assertEquals(options, result.options);
		assertEquals(arguments, result.arguments);
	}
}
