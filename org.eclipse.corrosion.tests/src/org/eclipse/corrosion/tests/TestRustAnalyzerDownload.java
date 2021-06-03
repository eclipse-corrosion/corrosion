/*********************************************************************
 * Copyright (c) 2021 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Victor Rubezhny   (Red Hat Inc.) - Initial implementation
 *******************************************************************************/
package org.eclipse.corrosion.tests;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;

import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.jface.preference.IPreferenceStore;
import org.junit.jupiter.api.Test;

public class TestRustAnalyzerDownload extends AbstractCorrosionTest {

	@Test
	void testRustAnalyzerDownload() {
		IPreferenceStore store = CorrosionPlugin.getDefault().getPreferenceStore();

		// Remove existing Rust Analyzer executable
		File rustAnalyzerExecutable = getRustAnalyzerExecutable(store);
		if (rustAnalyzerExecutable != null) {
			rustAnalyzerExecutable.delete();
		}

		setupRustAnalyzerExecutable(); // This fails on error

		// Extra check
		assertNotNull(getRustAnalyzerExecutable(store));
	}
}
