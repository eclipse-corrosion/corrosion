/*********************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.corrosion.sourcelookup;

import org.eclipse.osgi.util.NLS;

public class CargoSourceLookupMessages extends NLS {
	static {
		NLS.initializeMessages(CargoSourceLookupMessages.class.getName(), CargoSourceLookupMessages.class);
	}

	private CargoSourceLookupMessages() {
		// Do not instantiate
	}

	public static String AbsolutePathSourceContainer_0;

	public static String CargoProjectSourceContainerType_1;
	public static String CargoProjectSourceContainerType_2;

	public static String Project_0;

	public static String MappingSourceContainer_0;
}