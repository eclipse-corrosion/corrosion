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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.corrosion.launch.RustLaunchDelegateTools;
import org.eclipse.corrosion.resources.ResourceLookup;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.containers.LocalFileStorage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class CargoSourceUtils {
	private static final String NAME_COMMON_SOURCE_LOCATIONS = "commonSourceLocations"; //$NON-NLS-1$
	private static final String NAME_SOURCE_LOCATION = "sourceLocation"; //$NON-NLS-1$
	private static final String ATTR_CLASS = "class"; //$NON-NLS-1$
	private static final String ATTR_MEMENTO = "memento"; //$NON-NLS-1$

	public static String getCommonSourceLocationsMemento(ICargoSourceLocation[] locations) {
		Document document = null;
		Throwable ex = null;
		try {
			document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			Element element = document.createElement(NAME_COMMON_SOURCE_LOCATIONS);
			document.appendChild(element);
			saveSourceLocations(document, element, locations);
			return serializeDocument(document, true);
		} catch (ParserConfigurationException e) {
			ex = e;
		} catch (IOException e) {
			ex = e;
		} catch (TransformerException e) {
			ex = e;
		}
		CorrosionPlugin.logError(
				new Status(IStatus.ERROR, CorrosionPlugin.PLUGIN_ID, 0, "Error saving common source settings.", ex)); //$NON-NLS-1$
		return null;
	}

	private static void saveSourceLocations(Document doc, Element node, ICargoSourceLocation[] locations) {
		for (ICargoSourceLocation location : locations) {
			Element child = doc.createElement(NAME_SOURCE_LOCATION);
			child.setAttribute(ATTR_CLASS, location.getClass().getName());
			try {
				child.setAttribute(ATTR_MEMENTO, location.getMemento());
			} catch (CoreException e) {
				CorrosionPlugin.logError(e);
				continue;
			}
			node.appendChild(child);
		}
	}

	public static ICargoSourceLocation[] getCommonSourceLocationsFromMemento(String memento) {
		ICargoSourceLocation[] result = new ICargoSourceLocation[0];
		if (memento != null && !memento.isEmpty()) {
			try {
				DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				StringReader reader = new StringReader(memento);
				InputSource source = new InputSource(reader);
				Element root = parser.parse(source).getDocumentElement();
				if (root.getNodeName().equalsIgnoreCase(NAME_COMMON_SOURCE_LOCATIONS))
					result = initializeSourceLocations(root);
			} catch (ParserConfigurationException e) {
				CorrosionPlugin.logError(new Status(IStatus.ERROR, CorrosionPlugin.PLUGIN_ID, 0,
						"Error initializing common source settings.", e)); //$NON-NLS-1$
			} catch (SAXException e) {
				CorrosionPlugin.logError(new Status(IStatus.ERROR, CorrosionPlugin.PLUGIN_ID, 0,
						"Error initializing common source settings.", e)); //$NON-NLS-1$
			} catch (IOException e) {
				CorrosionPlugin.logError(new Status(IStatus.ERROR, CorrosionPlugin.PLUGIN_ID, 0,
						"Error initializing common source settings.", e)); //$NON-NLS-1$
			}
		}
		return result;
	}

	public static ICargoSourceLocation[] initializeSourceLocations(Element root) {
		List<ICargoSourceLocation> sourceLocations = new LinkedList<>();
		NodeList list = root.getChildNodes();
		int length = list.getLength();
		for (int i = 0; i < length; ++i) {
			Node node = list.item(i);
			short type = node.getNodeType();
			if (type == Node.ELEMENT_NODE) {
				Element entry = (Element) node;
				if (entry.getNodeName().equalsIgnoreCase(NAME_SOURCE_LOCATION)) {
					String className = entry.getAttribute(ATTR_CLASS);
					String data = entry.getAttribute(ATTR_MEMENTO);
					if (className == null || className.trim().length() == 0) {
						CorrosionPlugin.logError("Unable to restore common source locations - invalid format."); //$NON-NLS-1$
						continue;
					}
					Class<?> clazz = null;
					try {
						clazz = CorrosionPlugin.getDefault().getBundle().loadClass(className);
					} catch (ClassNotFoundException e) {
						CorrosionPlugin.logError(
								MessageFormat.format("Unable to restore source location - class not found {0}", //$NON-NLS-1$
										(Object[]) new String[] { className }));
						continue;
					}
					ICargoSourceLocation location = null;
					try {
						location = (ICargoSourceLocation) clazz.newInstance();
					} catch (IllegalAccessException e) {
						CorrosionPlugin.logError("Unable to restore source location: " + e.getMessage()); //$NON-NLS-1$
						continue;
					} catch (InstantiationException e) {
						CorrosionPlugin.logError("Unable to restore source location: " + e.getMessage()); //$NON-NLS-1$
						continue;
					}
					try {
						location.initializeFrom(data);
						sourceLocations.add(location);
					} catch (CoreException e) {
						CorrosionPlugin.logError("Unable to restore source location: " + e.getMessage()); //$NON-NLS-1$
					}
				}
			}
		}
		return sourceLocations.toArray(new ICargoSourceLocation[sourceLocations.size()]);
	}

	static public ISourceContainer[] convertSourceLocations(ICargoSourceLocation[] locations) {
		ArrayList<ISourceContainer> containers = new ArrayList<>(locations.length);
		for (ICargoSourceLocation location : locations) {
			if (location instanceof IProjectSourceLocation) {
				containers
						.add(new CargoProjectSourceContainer(((IProjectSourceLocation) location).getProject(), false));
			}
		}
		return containers.toArray(new ISourceContainer[containers.size()]);
	}

	/**
	 * Returns the project from the launch configuration, or {@code null} if it's
	 * not available.
	 */
	public static IProject getLaunchConfigurationProject(ISourceLookupDirector director) {
		String name = getLaunchConfigurationProjectName(director);
		return name != null ? ResourcesPlugin.getWorkspace().getRoot().getProject(name) : null;
	}

	/**
	 * Returns the project name from the launch configuration, or {@code null} if
	 * it's not available.
	 */
	public static String getLaunchConfigurationProjectName(ISourceLookupDirector director) {
		ILaunchConfiguration config = director.getLaunchConfiguration();
		if (config != null) {
			try {
				String name = config.getAttribute(RustLaunchDelegateTools.PROJECT_ATTRIBUTE, ""); //$NON-NLS-1$
				if (name.length() > 0)
					return name;
			} catch (CoreException e) {
				CorrosionPlugin.logError(e);
			}
		}
		return null;
	}

	public static IProject[] getAllReferencedProjects(IProject project) throws CoreException {
		Set<IProject> all = new HashSet<>();
		getAllReferencedProjects(all, project);
		return all.toArray(new IProject[all.size()]);
	}

	private static void getAllReferencedProjects(Set<IProject> all, IProject project) throws CoreException {
		for (IProject ref : project.getReferencedProjects()) {
			if (!all.contains(ref) && ref.exists() && ref.isOpen()) {
				all.add(ref);
				getAllReferencedProjects(all, ref);
			}
		}
	}

	/**
	 * Serializes a XML document into a string - encoded in UTF8 format, with
	 * platform line separators.
	 *
	 * @param doc    document to serialize
	 * @param indent if the xml text should be indented.
	 *
	 * @return the document as a string
	 */
	public static String serializeDocument(Document doc, boolean indent) throws IOException, TransformerException {
		ByteArrayOutputStream s = new ByteArrayOutputStream();
		TransformerFactory factory = TransformerFactory.newInstance();
		Transformer transformer = factory.newTransformer();
		transformer.setOutputProperty(OutputKeys.METHOD, "xml"); //$NON-NLS-1$
		transformer.setOutputProperty(OutputKeys.INDENT, indent ? "yes" : "no"); //$NON-NLS-1$ //$NON-NLS-2$
		DOMSource source = new DOMSource(doc);
		StreamResult outputTarget = new StreamResult(s);
		transformer.transform(source, outputTarget);
		return s.toString("UTF8"); //$NON-NLS-1$
	}

	/**
	 * Returns source elements corresponding to a file.
	 *
	 * @param file     A source or header file.
	 * @param director A source lookup director.
	 * @return An array of source elements sorted in relevance order. The elements
	 *         of the array can be either instances of IFile, ITranslationUnit or
	 *         LocalFileStorage. The returned array can be empty if no source
	 *         elements match the given file.
	 */
	public static Object[] findSourceElements(File file, ISourceLookupDirector director) {
		IFile[] wfiles = ResourceLookup.findFilesForLocation(new Path(file.getAbsolutePath()));
		IProject lcProject = null;
		if (director != null) {
			lcProject = getLaunchConfigurationProject(director);
		}

		if (wfiles.length > 0) {
			ResourceLookup.sortFilesByRelevance(wfiles, lcProject);
//			return updateUnavailableResources(wfiles, lcProject);
			return wfiles;
		}

		try {
			// Check the canonical path as well to support case insensitive file
			// systems like Windows.
			wfiles = ResourceLookup.findFilesForLocation(new Path(file.getCanonicalPath()));
			if (wfiles.length > 0) {
				ResourceLookup.sortFilesByRelevance(wfiles, lcProject);
//				return updateUnavailableResources(wfiles, lcProject);
				return wfiles;
			}

//			// The file is not already in the workspace so try to create an external
//			// translation unit for it.
//			if (lcProject != null) {
//				ICProject project = CoreModel.getDefault().create(lcProject);
//				if (project != null) {
//					ITranslationUnit translationUnit = CoreModel.getDefault().createTranslationUnitFrom(project,
//							URIUtil.toURI(file.getCanonicalPath(), true));
//					if (translationUnit != null) // if we failed do not return array with null in it
//						return new ITranslationUnit[] { translationUnit };
//				}
//			}
		} catch (IOException e) { // ignore if getCanonicalPath throws
		}

		// If we can't create an ETU then fall back on LocalFileStorage.
		return new LocalFileStorage[] { new LocalFileStorage(file) };
	}

}
