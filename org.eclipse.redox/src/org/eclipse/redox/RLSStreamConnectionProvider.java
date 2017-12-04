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

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.lsp4e.server.StreamConnectionProvider;

public class RLSStreamConnectionProvider implements StreamConnectionProvider {

	private boolean DEBUG = Boolean.parseBoolean(System.getProperty("rsl.lsp.debug"));
	private Process process;

	@Override
	public void start() throws IOException {
		RedoxPlugin plugin =  RedoxPlugin.getDefault();
		IPreferenceStore preferenceStore = plugin.getPreferenceStore();
		String path = preferenceStore.getString(RedoxPreferenceInitializer.rlsPathPreference);
		if(path.isEmpty()) {
			RedoxPlugin.getDefault().getLog().log(new Status(IStatus.ERROR,
					RedoxPlugin.getDefault().getBundle().getSymbolicName(),
					"Path to cargo not found. Go to https://rustup.rs/ to install."));
		}
		String[] command = new String[] {"/bin/bash", "-c", path};
		if (Platform.getOS().equals(Platform.OS_WIN32)) {
			command = new String[] {"cmd", "/c", path};
		}
		this.process = Runtime.getRuntime().exec(command);
	}

	@Override
	public InputStream getInputStream() {
		if (DEBUG) {
			return new FilterInputStream(process.getInputStream()) {
				@Override
				public int read() throws IOException {
					int res = super.read();
					System.err.print((char) res);
					return res;
				}

				@Override
				public int read(byte[] b, int off, int len) throws IOException {
					int bytes = super.read(b, off, len);
					byte[] payload = new byte[bytes];
					System.arraycopy(b, off, payload, 0, bytes);
					System.err.print(new String(payload));
					return bytes;
				}

				@Override
				public int read(byte[] b) throws IOException {
					int bytes = super.read(b);
					byte[] payload = new byte[bytes];
					System.arraycopy(b, 0, payload, 0, bytes);
					System.err.print(new String(payload));
					return bytes;
				}
			};
		} else {
			return process.getInputStream();
		}
	}

	@Override
	public OutputStream getOutputStream() {
		if (DEBUG) {
			return new FilterOutputStream(process.getOutputStream()) {
				@Override
				public void write(int b) throws IOException {
					System.err.print((char) b);
					super.write(b);
				}

				@Override
				public void write(byte[] b) throws IOException {
					System.err.print(new String(b));
					super.write(b);
				}

				@Override
				public void write(byte[] b, int off, int len) throws IOException {
					byte[] actual = new byte[len];
					System.arraycopy(b, off, actual, 0, len);
					System.err.print(new String(actual));
					super.write(b, off, len);
				}
			};
		} else {
			return process.getOutputStream();
		}
	}

	@Override
	public void stop() {
		process.destroy();
	}

}
