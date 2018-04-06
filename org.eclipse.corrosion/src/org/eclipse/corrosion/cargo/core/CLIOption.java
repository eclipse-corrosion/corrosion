/*********************************************************************
 * Copyright (c) 2018 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Lucas Bullen   (Red Hat Inc.) - Initial implementation
 *******************************************************************************/
package org.eclipse.corrosion.cargo.core;

import java.util.Arrays;
import java.util.List;

public class CLIOption {
	private String flag;
	private String[] arguments;
	private String description;

	public CLIOption(List<String> lines) {
		if (lines == null || lines.size() == 0 || !lines.get(0).matches("\\s*-+.*")) {
			throw new IllegalArgumentException("lines is not the option help content");
		}

		String[] firstLine = lines.get(0).trim().split("  ");
		int start = firstLine[0].lastIndexOf(", ");
		start = start == -1 ? 0 : start + 2;
		String[] flagSection = firstLine[0].substring(start).split("\\s");
		flag = flagSection[0];
		arguments = Arrays.copyOfRange(flagSection, 1, flagSection.length);

		description = String.join("  ", Arrays.copyOfRange(firstLine, 1, firstLine.length)).trim();
		lines.remove(0);
		for (String string : lines) {
			description += " " + string.trim();
		}
	}

	public String getFlag() {
		return flag;
	}

	public String getDescription() {
		return description;
	}

	public String[] getArguments() {
		return arguments;
	}

	public String getDescription(int length) {
		if (length > description.length()) {
			return description;
		}
		if (length > 3) {
			return description.substring(0, length - 3);
		}
		return description.substring(0, length);
	}
}