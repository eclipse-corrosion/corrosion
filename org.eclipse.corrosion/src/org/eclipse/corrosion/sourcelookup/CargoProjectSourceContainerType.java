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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.AbstractSourceContainerTypeDelegate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * The type for creating/restoring a project source container.
 */
public class CargoProjectSourceContainerType extends AbstractSourceContainerTypeDelegate {

	@Override
	public String getMemento(ISourceContainer container) throws CoreException {
		CargoProjectSourceContainer sourceContainer = (CargoProjectSourceContainer) container;
		Document document = newDocument();
		Element element = document.createElement("project"); //$NON-NLS-1$
		IProject project = sourceContainer.getProject();
		if (project != null) {
			element.setAttribute("name", project.getName()); //$NON-NLS-1$
		}
		element.setAttribute("referencedProjects", String.valueOf(sourceContainer.isSearchReferencedProjects())); //$NON-NLS-1$
		document.appendChild(element);
		return serializeDocument(document);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.debug.internal.core.sourcelookup.ISourceContainerType#
	 * createSourceContainer(java.lang.String)
	 */
	@Override
	public ISourceContainer createSourceContainer(String memento) throws CoreException {
		Node node = parseDocument(memento);
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			Element element = (Element) node;
			if ("project".equals(element.getNodeName())) { //$NON-NLS-1$
				String string = element.getAttribute("name"); //$NON-NLS-1$
				IProject project = null;
				if (string != null && string.length() > 0) {
					project = ResourcesPlugin.getWorkspace().getRoot().getProject(string);
				}
				String nest = element.getAttribute("referencedProjects"); //$NON-NLS-1$
				boolean ref = Boolean.parseBoolean(nest);
				return new CargoProjectSourceContainer(project, ref);
			}
			abort(CargoSourceLookupMessages.CargoProjectSourceContainerType_1, null);
		}
		abort(CargoSourceLookupMessages.CargoProjectSourceContainerType_2, null);
		return null;
	}
}
