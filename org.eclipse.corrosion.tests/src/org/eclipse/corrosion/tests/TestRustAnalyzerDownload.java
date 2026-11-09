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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.corrosion.CorrosionPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestRustAnalyzerDownload extends AbstractCorrosionTest {

	/** The place where Corrosion installs the downloaded rust-analyzer */
	private static final Path DOWNLOAD_LOCATION = Path.of(System.getProperty("user.home"), ".cargo", "bin", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			"rust-analyzer"); //$NON-NLS-1$

	/** Original file -> its backup */
	private final Map<Path, Path> backups = new LinkedHashMap<>();
	private Path backupFolder;
	private String originalPreference;

	/**
	 * The test removes the installed rust-analyzer and downloads another one. Keep
	 * the installed one, so that the next tests and the developer's machine use it
	 * again after the test.
	 */
	@BeforeEach
	void keepInstalledRustAnalyzer() throws IOException {
		IPreferenceStore store = CorrosionPlugin.getDefault().getPreferenceStore();
		originalPreference = store.getString(CorrosionPreferenceInitializer.RLS_PATH_PREFERENCE);
		backupFolder = Files.createTempDirectory("rust-analyzer-backup"); //$NON-NLS-1$
		File installed = getRustAnalyzerExecutable(store);
		if (installed != null) {
			backup(installed.toPath());
		}
		backup(DOWNLOAD_LOCATION);
	}

	@AfterEach
	void restoreInstalledRustAnalyzer() throws IOException {
		Files.deleteIfExists(DOWNLOAD_LOCATION);
		for (Map.Entry<Path, Path> backup : backups.entrySet()) {
			Files.deleteIfExists(backup.getKey());
			Files.move(backup.getValue(), backup.getKey());
		}
		Files.deleteIfExists(backupFolder);
		CorrosionPlugin.getDefault().getPreferenceStore().setValue(CorrosionPreferenceInitializer.RLS_PATH_PREFERENCE,
				originalPreference);
	}

	@Test
	void testRustAnalyzerDownload() {
		assumeTrue(canReachGitHub(), "GitHub cannot be reached, so the download cannot be tested"); //$NON-NLS-1$
		IPreferenceStore store = CorrosionPlugin.getDefault().getPreferenceStore();
		// The backup moved the installed rust-analyzer away
		store.setValue(CorrosionPreferenceInitializer.RLS_PATH_PREFERENCE, ""); //$NON-NLS-1$

		setupRustAnalyzerExecutable(); // This fails on error

		// Extra check
		assertNotNull(getRustAnalyzerExecutable(store));
	}

	/** Moves the file (or link) to the backup folder, if it exists */
	private void backup(Path file) throws IOException {
		if (backups.containsKey(file) || !Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
			return;
		}
		Path backup = backupFolder.resolve(backups.size() + "-" + file.getFileName()); //$NON-NLS-1$
		Files.move(file, backup);
		backups.put(file, backup);
	}

	private static boolean canReachGitHub() {
		try {
			HttpURLConnection connection = (HttpURLConnection) URI.create("https://github.com").toURL() //$NON-NLS-1$
					.openConnection();
			connection.setRequestMethod("HEAD"); //$NON-NLS-1$
			connection.setConnectTimeout(10_000);
			connection.setReadTimeout(10_000);
			return connection.getResponseCode() < 500;
		} catch (IOException e) {
			return false;
		}
	}
}
