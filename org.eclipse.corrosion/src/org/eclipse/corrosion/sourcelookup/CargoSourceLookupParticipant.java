/*********************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.corrosion.sourcelookup;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupParticipant;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;

/**
 * A source lookup participant that searches for Rust source code.
 */
public class CargoSourceLookupParticipant extends AbstractSourceLookupParticipant {

	private Map<Object, Object[]> fCachedResults = Collections.synchronizedMap(new HashMap<Object, Object[]>());

	/**
	 * Constructor for CargoSourceLookupParticipant.
	 */
	public CargoSourceLookupParticipant() {
		super();
	}

	@Override
	public String getSourceName(Object object) throws CoreException {
		if (object instanceof String) {
			return (String) object;
		}
		return null;
	}

	@Override
	public Object[] findSourceElements(Object object) throws CoreException {
		// Check the cache
		Object[] results = fCachedResults.get(object);
		if (results != null)
			return results;

		// Actually query the source containers for the requested resource
		Object[] foundElements = super.findSourceElements(object);
		fCachedResults.put(object, foundElements);
		return foundElements;
	}

	@Override
	public void sourceContainersChanged(ISourceLookupDirector director) {
		// clear the cache
		fCachedResults.clear();
		super.sourceContainersChanged(director);
	}
}
