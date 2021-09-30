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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This class represents arguments to cargo. This consist of the command (e.g.
 * "test" or "run"), the options (flags for the command) and arguments (to be
 * passed to the executable called by cargo). Instances of this class can be
 * created via the static factory method
 * {@link CargoArgs#fromAllArguments(String...)}.
 */
class CargoArgs {
	public final List<String> options;
	public final List<String> arguments;
	public final String command;

	private CargoArgs(List<String> options, List<String> arguments, String command) {
		super();
		this.options = options;
		this.arguments = arguments;
		this.command = command;
	}

	/**
	 * This static factory method splits the given {@code args} by expecting the
	 * following argument pattern:
	 * {@code [OMITTED] COMMAND [OPTIONS] [-- ARGS]}.<br>
	 * Where:
	 * <ul>
	 * <li>{@code OMITTED} are flags starting with {@code "-"}, which are
	 * omitted</li>
	 * <li>{@code COMMAND} is the cargo command (e.g "test" or "run")</li>
	 * <li>{@code OPTIONS} are flags for the cargo command</li>
	 * <li>{@code ARGS} are arguments passed to the program called by cargo</li>
	 * </ul>
	 * This pattern is a heuristic for most cargo commands, it may be updated to
	 * support different command structures.
	 *
	 * @param args all arguments after a sub-command (e.g. )
	 * @return instance of {@code CargoArgs} holding command, options and arguments
	 */
	public static CargoArgs fromAllArguments(String... args) {

		String resultCommand = ""; //$NON-NLS-1$
		List<String> resultOptions = Collections.emptyList();
		List<String> resultArguments = Collections.emptyList();

		int startIndex = 0;
		for (String arg : args) {
			startIndex++;
			// skip starting flags
			if (!arg.startsWith("-")) { //$NON-NLS-1$
				resultCommand = arg;
				break;
			}
		}

		int length = args.length;
		if (startIndex < length - 1) {
			List<String> argsList = Arrays.asList(args);
			int separatorIndex = argsList.indexOf("--"); //$NON-NLS-1$
			if (separatorIndex < 0) {
				resultOptions = argsList.subList(startIndex, length);
			} else {
				resultOptions = argsList.subList(startIndex, separatorIndex);
				if (separatorIndex < length - 1) {
					resultArguments = argsList.subList(separatorIndex + 1, length);
				}
			}
		}
		return new CargoArgs(resultOptions, resultArguments, resultCommand);
	}
}