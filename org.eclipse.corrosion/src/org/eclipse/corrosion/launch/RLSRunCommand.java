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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.Command;

/**
 * Class holding all information of the {@code rls.run} RLS LSP command.
 * Instances can be read from {@link Command} objects using the
 * {@link RLSRunCommand#fromLSPCommand(Command)}.
 */
class RLSRunCommand {

	public final String binary;
	public final Map<String, String> env;
	public final String[] args;

	private RLSRunCommand(String binary, Map<String, String> env, String[] args) {
		super();
		this.binary = binary;
		this.env = env;
		this.args = args;
	}

	/**
	 * Creates an {@link RLSRunCommand} if the given {@code command} contains all
	 * information needed for creating one. If all information is available, the
	 * returned {@code Option} will contain an instance of {@code RLSRunCommand},
	 * otherwise the returned {@code Option} will be empty.
	 *
	 * @param command the LSP command used to read all information needed for the
	 *                resulting {@link RLSRunCommand}
	 * @return if all information available for creating a {@code RLSRunCommand},
	 *         the {@code Optional} will hold a value, otherwise the returned
	 *         {@code Optional} will be empty.
	 */
	public static Optional<RLSRunCommand> fromLSPCommand(Command command) {
		List<Object> arguments = command.getArguments();
		if (arguments == null || arguments.size() < 1) {
			return Optional.empty();
		}

		Object argumentsObj = arguments.get(0);
		Map<?, ?> argMap = castOrNull(argumentsObj, Map.class);
		if (argMap == null) {
			return Optional.empty();
		}

		Object binaryObj = argMap.get("binary"); //$NON-NLS-1$
		if (binaryObj == null) {
			return Optional.empty();
		}
		String binary = Objects.toString(binaryObj);

		List<?> argsObj = castOrNull(argMap.get("args"), List.class); //$NON-NLS-1$
		if (argsObj == null) {
			return Optional.empty();
		}
		String[] args = argsObj.stream().filter(String.class::isInstance).map(String.class::cast)
				.toArray(String[]::new);

		Map<?, ?> envObjsMap = castOrNull(argMap.get("env"), Map.class); //$NON-NLS-1$
		if (envObjsMap == null) {
			return Optional.empty();
		}
		Map<String, String> envMap = envObjsMap.entrySet().stream()
				.filter(e -> e.getKey() != null && e.getValue() != null)
				.collect(Collectors.toMap(e -> Objects.toString(e.getKey()), e -> Objects.toString(e.getValue())));

		return Optional.of(new RLSRunCommand(binary, envMap, args));
	}

	private static <T> T castOrNull(Object o, Class<T> clazz) {
		if (!clazz.isInstance(o)) {
			return null;
		}
		return clazz.cast(o);
	}
}
