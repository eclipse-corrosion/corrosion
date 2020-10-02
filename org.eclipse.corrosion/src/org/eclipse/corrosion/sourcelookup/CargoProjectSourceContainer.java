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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceContainerType;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.containers.CompositeSourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.FolderSourceContainer;

/**
 * A project in the workspace. Source is searched for in the root project folder
 * and all folders within the project recursively. Optionally, referenced
 * projects may be searched as well.
 *
 * Source elements returned from <code>findSourceElements(...)</code> are
 * instances of <code>IFile</code>.
 * <p>
 * Clients may instantiate this class.
 * </p>
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
public class CargoProjectSourceContainer extends CompositeSourceContainer {
	/**
	 * Unique identifier for the project source container type (value
	 * <code>org.eclipse.corrosion.containerType.project</code>).
	 */
	public static final String TYPE_ID = CorrosionPlugin.PLUGIN_ID + ".containerType.project"; //$NON-NLS-1$
	private final IProject fOwnProject; // Project assigned to this container at construction time.
	private IProject fProject;
	private boolean fSearchReferencedProjects;
	private URI fRootURI;
	private IFileStore fRootFile;
	private IWorkspaceRoot fRoot;

	/**
	 * Constructs a project source container.
	 *
	 * @param project    the project to search for source in
	 * @param referenced whether referenced projects should be considered
	 */
	public CargoProjectSourceContainer(IProject project, boolean referenced) {
		fOwnProject = project;
		fProject = project;
		fSearchReferencedProjects = referenced;
	}

	/**
	 * Returns the project this source container references.
	 *
	 * @return the project this source container references
	 */
	public IProject getProject() {
		return fProject;
	}

	@Override
	public void init(ISourceLookupDirector director) {
		super.init(director);
		if (fProject == null && director != null) {
			fProject = CargoSourceUtils.getLaunchConfigurationProject(director);
		}
		if (fProject != null) {
			fRootURI = fProject.getLocationURI();
			if (fRootURI == null)
				return;
			try {
				fRootFile = EFS.getStore(fRootURI);
			} catch (CoreException e) {
				// Ignore
			}
			fRoot = ResourcesPlugin.getWorkspace().getRoot();
		}
	}

	@Override
	public void dispose() {
		fProject = fOwnProject;
		super.dispose();
	}

	@Override
	public Object[] findSourceElements(String name) throws CoreException {
		if (fProject == null)
			return EMPTY;

		ArrayList<Object> sources = new ArrayList<>();

		// An IllegalArgumentException is thrown from the "getFile" method
		// if the path created by appending the file name to the container
		// path doesn't conform with Eclipse resource restrictions.
		// To prevent the interruption of the search procedure we check
		// if the path is valid before passing it to "getFile".
		if (validateFile(name)) {
			IFile file = fProject.getFile(new Path(name));
			if (file.exists()) {
				sources.add(file);
			} else {
				if (fRootURI == null) {
					return EMPTY;
				}
				IFileStore target = fRootFile.getFileStore(new Path(name));
				if (target.fetchInfo().exists()) {
					IFile[] files = fRoot.findFilesForLocationURI(target.toURI());
					if (isFindDuplicates() && files.length > 1) {
						for (IFile f : files) {
							sources.add(f);
						}
					} else if (files.length > 0) {
						sources.add(files[0]);
					}
				}
			}
		}

		// Check sub-folders
		if ((isFindDuplicates() && true) || (sources.isEmpty() && true)) {
			ISourceContainer[] containers = getSourceContainers();
			for (ISourceContainer container : containers) {
				Object[] objects = container.findSourceElements(name);
				if (objects == null || objects.length == 0) {
					continue;
				}
				if (isFindDuplicates()) {
					for (Object object : objects)
						sources.add(object);
				} else {
					sources.add(objects[0]);
					break;
				}
			}
		}

		if (sources.isEmpty())
			return EMPTY;
		return sources.toArray();
	}

	@Override
	public String getName() {
		return fProject != null ? fProject.getName() : CargoSourceLookupMessages.Project_0;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj instanceof CargoProjectSourceContainer) {
			CargoProjectSourceContainer loc = (CargoProjectSourceContainer) obj;
			return fProject == null ? loc.fProject == null : fProject.equals(loc.fProject);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return TYPE_ID.hashCode() * 31 + (fProject == null ? 0 : fProject.hashCode());
	}

	@Override
	public boolean isComposite() {
		return true;
	}

	@Override
	protected ISourceContainer[] createSourceContainers() throws CoreException {
		if (fProject != null && fProject.isOpen()) {
			if (isSearchReferencedProjects()) {
				IProject[] projects = CargoSourceUtils.getAllReferencedProjects(fProject);
				ISourceContainer[] folders = createFolderSourceContainers(fProject);
				List<ISourceContainer> containers = new ArrayList<>(folders.length + projects.length);
				for (ISourceContainer folder : folders) {
					containers.add(folder);
				}
				for (IProject ref : projects) {
					if (ref.exists() && ref.isOpen()) {
						CargoProjectSourceContainer container = new CargoProjectSourceContainer(ref, false);
						container.init(getDirector());
						containers.add(container);
					}
				}
				return containers.toArray(new ISourceContainer[containers.size()]);
			}
			return createFolderSourceContainers(fProject);
		}
		return new ISourceContainer[0];
	}

	private ISourceContainer[] createFolderSourceContainers(IProject project) throws CoreException {
		IResource[] resources = project.members();
		List<FolderSourceContainer> list = new ArrayList<>(resources.length);
		for (IResource resource : resources) {
			if (resource.getType() == IResource.FOLDER) {
				list.add(new FolderSourceContainer((IFolder) resource, true));
			}
		}
		ISourceContainer[] containers = list.toArray(new ISourceContainer[list.size()]);
		for (ISourceContainer container : containers) {
			container.init(getDirector());
		}
		return containers;
	}

	/**
	 * Validates the given string as a path for a file in this container.
	 *
	 * @param name path name
	 */
	private boolean validateFile(String name) {
		if (fProject == null) {
			return false;
		}
		IPath path = fProject.getFullPath().append(name);
		return ResourcesPlugin.getWorkspace().validatePath(path.toOSString(), IResource.FILE).isOK();
	}

	/**
	 * Returns whether referenced projects are considered.
	 *
	 * @return whether referenced projects are considered
	 */
	public boolean isSearchReferencedProjects() {
		return fSearchReferencedProjects;
	}

	@Override
	public ISourceContainerType getType() {
		return getSourceContainerType(TYPE_ID);
	}
}
