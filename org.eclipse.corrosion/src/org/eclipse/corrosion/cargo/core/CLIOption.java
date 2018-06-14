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

import org.eclipse.corrosion.Messages;

public class CLIOption {
	private String flag;
	private String[] arguments;
	private String description;

	public CLIOption(List<String> lines) {
		if (lines == null || lines.isEmpty() || !lines.get(0).matches("\\s*-+.*")) { //$NON-NLS-1$
			throw new IllegalArgumentException(Messages.CLIOption_lineIsNotHelp);
		}

		String[] firstLine = lines.get(0).trim().split("  "); //$NON-NLS-1$
		int start = firstLine[0].lastIndexOf(", "); //$NON-NLS-1$
		start = start == -1 ? 0 : start + 2;
		String[] flagSection = firstLine[0].substring(start).split("\\s"); //$NON-NLS-1$
		flag = flagSection[0];
		arguments = Arrays.copyOfRange(flagSection, 1, flagSection.length);

		StringBuilder descriptionBuilder = new StringBuilder(String.join("  ", Arrays.copyOfRange(firstLine, 1, firstLine.length)).trim()); //$NON-NLS-1$
		lines.remove(0);
		for (String string : lines) {
			descriptionBuilder.append(string.trim());
		}
		description = descriptionBuilder.toString();
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