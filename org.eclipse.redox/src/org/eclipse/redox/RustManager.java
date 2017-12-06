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

public class RustManager {
	public static void download(String id) {
		// TODO: perform:
		// "curl https://sh.rustup.rs -sSf | sh" and follow up
		// rustup toolchain install nightly-${date of last including rls}
		// rustup default nightly-${date of last including rls}
		// TODO: perform:
		// rustup component add rls-preview
		// rustup component add rust-analysis
		// rustup component add rust-src
	}

	public static String getLatestNightlyId() {
		// TODO: return the id of the latest nightly version
		return "nightly-2012-12-01";
	}

	public static String getDownloadedNightlyId() {
		// TODO: return the id of the nightly version downloaded
		return "nightly-2012-12-01";
	}

	public static boolean isToolchainDownloaded(String id) {
		// TODO: check if downloaded
		return true;
	}
}
