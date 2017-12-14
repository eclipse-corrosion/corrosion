/*********************************************************************
 * Copyright (c) 2017 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Lucas Bullen (Red Hat Inc.) - Initial implementation
 *******************************************************************************/
package org.eclipse.redox.builder;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Platform;

public class TestCargoBuilderEnabled extends PropertyTester {

	private static final String IS_ENABLED = "isCargoBuilderEnabled";

	@Override
	public boolean test(final Object receiver, final String property, final Object[] args, final Object expectedValue) {
		if (IS_ENABLED.equals(property)) {
			final IProject project = Platform.getAdapterManager().getAdapter(receiver, IProject.class);
			if (project != null) {
				return AddCargoBuilder.hasBuilder(project);
			}
		}
		return false;
	}
}