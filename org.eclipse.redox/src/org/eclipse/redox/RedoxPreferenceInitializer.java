/*******************************************************************************
 * Copyright (c) 2017 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
	private String getRLSPathBestGuess() {
		//TODO: make a best guess for the location
		return "";
	}
}
