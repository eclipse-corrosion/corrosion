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
	private static final Path DOWNLOAD_LOCATION = Path
			.of(System.getProperty("user.home"), ".cargo", "bin", "rust-analyzer") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			.toAbsolutePath().normalize();

	/** Original file -> its backup */
	private final Map<Path, Path> backups = new LinkedHashMap<>();
	private Path backupFolder;
	private String originalPreference;
	/**
	 * True when the original file at the download location is in the backup, or
	 * when there was no file. Only then the file there after the test is the
	 * download, and the restore can delete it.
	 */
	private boolean downloadLocationKept;

	/**
	 * The test removes the installed rust-analyzer and downloads another one. Keep
	 * the installed one, so that the next tests and the developer's machine use it
	 * again after the test.
	 */
	@BeforeEach
	void keepInstalledRustAnalyzer() throws IOException {
		IPreferenceStore store = CorrosionPlugin.getDefault().getPreferenceStore();
		originalPreference = store.getString(CorrosionPreferenceInitializer.RLS_PATH_PREFERENCE);
		// Next to the file, so that a move does not copy the file to another file
		// system
		Files.createDirectories(DOWNLOAD_LOCATION.getParent());
		backupFolder = Files.createTempDirectory(DOWNLOAD_LOCATION.getParent(), "rust-analyzer-backup"); //$NON-NLS-1$
		// First the download location: the restore deletes the file there
		backup(DOWNLOAD_LOCATION);
		downloadLocationKept = true;
		File installed = getRustAnalyzerExecutable(store);
		if (installed != null) {
			backup(installed.toPath());
		}
	}

	@AfterEach
	void restoreInstalledRustAnalyzer() {
		try {
			if (downloadLocationKept) {
				try {
					Files.deleteIfExists(DOWNLOAD_LOCATION);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			for (Map.Entry<Path, Path> backup : backups.entrySet()) {
				try {
					Files.deleteIfExists(backup.getKey());
					Files.move(backup.getValue(), backup.getKey());
				} catch (IOException e) {
					System.err.println("Could not restore " + backup.getKey() + " from " + backup.getValue()); //$NON-NLS-1$ //$NON-NLS-2$
					e.printStackTrace();
				}
			}
			if (backupFolder != null) {
				try {
					Files.deleteIfExists(backupFolder);
				} catch (IOException e) {
					e.printStackTrace(); // not empty: a restore failed, keep the backup
				}
			}
		} finally {
			CorrosionPlugin.getDefault().getPreferenceStore()
					.setValue(CorrosionPreferenceInitializer.RLS_PATH_PREFERENCE, originalPreference);
		}
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
		Path normalized = file.toAbsolutePath().normalize();
		if (backups.containsKey(normalized) || !Files.exists(normalized, LinkOption.NOFOLLOW_LINKS)) {
			return;
		}
		Path backup = backupFolder.resolve(backups.size() + "-" + normalized.getFileName()); //$NON-NLS-1$
		Files.move(normalized, backup);
		backups.put(normalized, backup);
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
