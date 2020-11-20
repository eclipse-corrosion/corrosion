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
 *  Lucas Bullen (Red Hat Inc.) - Initial implementation
 *******************************************************************************/
package org.eclipse.corrosion.debug;

import java.io.File;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.eclipse.cdt.dsf.gdb.launching.GdbLaunch;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;

public class RustGDBLaunchWrapper extends GdbLaunch {
	public RustGDBLaunchWrapper(ILaunch launch) {
		super(launch.getLaunchConfiguration(), launch.getLaunchMode(), launch.getSourceLocator());
	}

	/**
	 * Merge the default environment from parent (ie from project configuration)
	 * with the one from the launch configuration (inheriting PATH). This is
	 * necessary to run `rust-gdb --version`, which needs full PATH, not available
	 * in project confiuration.
	 *
	 * @return environment variables with required paths appended to the path
	 *         variable
	 */
	@Override
	public String[] getLaunchEnvironment() throws CoreException {
		String[] cProjectEnv = super.getLaunchEnvironment();
		String[] launchConfigEnv = getLaunchManager().getEnvironment(getLaunchConfiguration());
		Map<String, String> env = new HashMap<>(cProjectEnv.length + launchConfigEnv.length);
		Stream.of(cProjectEnv, launchConfigEnv) //
				.flatMap(Arrays::stream) //
				.map(exp -> {
					int index = exp.indexOf('=');
					String key = exp.substring(0, index);
					String value = exp.substring(index + 1, exp.length());
					return new SimpleEntry<>(key, value);
				}).forEach(
						entry -> env.merge(entry.getKey(), entry.getValue(), (v1, v2) -> v1 + File.pathSeparator + v2));
		return env.entrySet().stream().map(entry -> entry.getKey() + '=' + entry.getValue()).toArray(String[]::new);
	}

}
