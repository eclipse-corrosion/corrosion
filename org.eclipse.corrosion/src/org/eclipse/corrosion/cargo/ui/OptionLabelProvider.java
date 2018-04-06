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
package org.eclipse.corrosion.cargo.ui;

import org.eclipse.corrosion.cargo.core.CLIOption;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

public class OptionLabelProvider extends LabelProvider {

	@Override
	public Image getImage(Object element) {
		return null;
	}

	@Override
	public String getText(Object element) {
		String returnString;
		if (element instanceof CLIOption) {
			String flag = ((CLIOption) element).getFlag();
			if (flag.startsWith("--")) {
				returnString = flag.substring(2);
			} else if (flag.startsWith("-")) {
				returnString = flag.substring(1);
			} else {
				returnString = flag;
			}
			String[] arguments = ((CLIOption) element).getArguments();
			if (arguments != null) {
				returnString += " " + String.join(" ", arguments);
			}
			return returnString;
		}
		return "";
	}
}
