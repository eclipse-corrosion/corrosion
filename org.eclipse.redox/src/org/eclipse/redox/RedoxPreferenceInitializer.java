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
package org.eclipse.redox;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

public class RedoxPreferenceInitializer extends AbstractPreferenceInitializer {

	public static String rlsPathPreference = "redox.rlsExplicitPath";

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = RedoxPlugin.getDefault().getPreferenceStore();
		store.setDefault(rlsPathPreference, getRLSPathBestGuess());
	}

	//TODO: instead use the rls from rustup associated with the user designated toolchain
	private String getRLSPathBestGuess() {
		return System.getProperty("user.home")+"/.cargo/bin/rls";
	}
}
