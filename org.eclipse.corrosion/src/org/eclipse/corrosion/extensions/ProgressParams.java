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
package org.eclipse.corrosion.extensions;

public class ProgressParams {
	private String id;
	private String title;
	private String message;
	private int percentage = 0;
	private Boolean done = false;

	public ProgressParams(String id, String title) {
		this.id = id;
		this.title = title;
	}

	public ProgressParams(String id, String title, String message, int percentage) {
		this(id, title);
		this.message = message;
		this.percentage = percentage;
	}

	public ProgressParams(String id, String title, Boolean done) {
		this(id, title);
		this.done = done;
	}

	public String getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getMessage() {
		return message;
	}

	public int getPercentage() {
		return percentage;
	}

	public boolean isDone() {
		if (done == null) {
			return false;
		}
		return done;
	}
}
