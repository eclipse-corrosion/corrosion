/*********************************************************************
 * Copyright (c) 2017, 2018 Red Hat Inc. and others.
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
package org.eclipse.corrosion;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.corrosion.extensions.ProgressIndicatorJob;
import org.eclipse.corrosion.extensions.ProgressParams;
import org.eclipse.lsp4e.LanguageClientImpl;
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;

@SuppressWarnings("restriction")
public class RLSClientImplementation extends LanguageClientImpl {
	private Map<String, ProgressIndicatorJob> progressJobs = new HashMap<>();

	@JsonNotification("window/progress")
	public void progress(ProgressParams progress) {
		String id = progress.getId();
		if (!progressJobs.containsKey(id)) {
			ProgressIndicatorJob job = new ProgressIndicatorJob(progress.getTitle());
			job.schedule();
			progressJobs.put(id, job);
		}
		progressJobs.get(id).update(progress);
	}
}
