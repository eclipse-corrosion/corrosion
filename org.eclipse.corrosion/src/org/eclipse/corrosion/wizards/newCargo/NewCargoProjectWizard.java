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
package org.eclipse.corrosion.wizards.newCargo;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.corrosion.CorrosionPreferenceInitializer;
import org.eclipse.corrosion.builder.IncrementalCargoBuilder;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

public class NewCargoProjectWizard extends Wizard implements INewWizard {
	private NewCargoProjectWizardPage wizardPage;
	public static final String ID = "org.eclipse.corrosion.wizards.newCargo";

	public NewCargoProjectWizard() {
		super();
		setNeedsProgressMonitor(true);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		wizardPage = new NewCargoProjectWizardPage();
		setWindowTitle("New Cargo Based Rust Project");

		Iterator<?> selectionIterator = selection.iterator();
		Set<IWorkingSet> workingSets = new HashSet<>();
		IResource selectedResource = null;

		while (selectionIterator.hasNext()) {
			Object element = selectionIterator.next();
			IResource asResource = toResource(element);

			if (asResource != null && selectedResource == null) {
				selectedResource = asResource;
			} else {
				IWorkingSet asWorkingSet = Adapters.adapt(element, IWorkingSet.class);
				if (asWorkingSet != null) {
					workingSets.add(asWorkingSet);
				}
			}
		}

		if (workingSets.isEmpty() && selectedResource != null) {
			workingSets.addAll(getWorkingSets(selectedResource));
		}
		wizardPage.setWorkingSets(workingSets);

		if (selectedResource != null) {
			wizardPage.setDirectory(toFile(selectedResource));
		} else {
			wizardPage.setDirectory(newFolderLocation());
		}
	}

	@Override
	public void addPages() {
		addPage(wizardPage);
	}

	@Override
	public boolean performFinish() {
		File location = wizardPage.getDirectory();
		String projectName = wizardPage.getProjectName();
		Boolean isBin = wizardPage.isBinaryTemplate();
		String vcs = wizardPage.getVCS();

		IPreferenceStore store = CorrosionPlugin.getDefault().getPreferenceStore();
		String cargo = store.getString(CorrosionPreferenceInitializer.cargoPathPreference);

		Boolean makeLocation = !location.exists();
		if (makeLocation) {
			location.mkdirs();
		}

		try {
			getContainer().run(true, true, monitor -> {
				monitor.beginTask("Creating Rust project", 0);
				List<String> commandLine = new ArrayList<>();
				commandLine.add(cargo);
				commandLine.add("init");

				commandLine.add("--name");
				commandLine.add(projectName);

				commandLine.add("--vcs");
				commandLine.add(vcs);

				if (isBin) {
					commandLine.add("--bin");
				} else {
					commandLine.add("--lib");
				}

				ProcessBuilder processBuilder = new ProcessBuilder(commandLine);
				processBuilder.directory(location);

				try {
					Process process = processBuilder.start();
					boolean isProcessDone = false;
					while (!isProcessDone) {
						if (monitor.isCanceled()) {
							process.destroyForcibly();
						}
						isProcessDone = process.waitFor(100, TimeUnit.MILLISECONDS);
					}
					if (process.exitValue() == 0) {
						String mainFileName;
						if (isBin) {
							mainFileName = "main.rs";
						} else {
							mainFileName = "lib.rs";
						}
						createProject(projectName, location, mainFileName, monitor);
					} else {
						String errorOutput = "";
						try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
							String errorLine;
							while ((errorLine = in.readLine()) != null) {
								errorOutput += errorLine + "\n";
							}
						}
						final String finalErrorOutput = errorOutput;
						Display.getDefault().asyncExec(() -> {
							MessageDialog.openError(getShell(), "Cannot Create Rust Project", "Create unsuccessful.`"
									+ String.join(" ", commandLine) + "` error log:\n\n" + finalErrorOutput);
						});
						if (makeLocation) {
							location.delete();
						}
					}
					monitor.done();
				} catch (IOException e) {
					monitor.done();
					MessageDialog.openError(getShell(), "Cannot Create Rust project", e.toString());
					if (makeLocation) {
						location.delete();
					}
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			MessageDialog.openError(getShell(), "Cannot Create Rust Project", e.toString());
			return false;
		}
		return true;
	}

	private void createProject(String name, File directory, String mainFileName, IProgressMonitor monitor) {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = root.getProject(name);
		try {
			if (!project.exists()) {
				final IWorkspace workspace = ResourcesPlugin.getWorkspace();
				IProjectDescription projectDescription = workspace.newProjectDescription(project.getName());

				String projectLocation = directory.getAbsolutePath();
				IPath projectPath = new Path(projectLocation);

				projectDescription.setLocation(projectPath);

				ICommand[] commands = projectDescription.getBuildSpec();
				ICommand command = projectDescription.newCommand();
				command.setBuilderName(IncrementalCargoBuilder.BUILDER_ID);
				ICommand[] nc = new ICommand[commands.length + 1];
				System.arraycopy(commands, 0, nc, 1, commands.length);
				nc[0] = command;
				projectDescription.setBuildSpec(nc);

				project.create(projectDescription, monitor);
			}
			project.open(monitor);
			project.refreshLocal(IResource.DEPTH_INFINITE, monitor);

		} catch (CoreException e) {
			MessageDialog.openError(getShell(), "Unable to load project description", e.toString());
		}

		IWorkingSetManager wsm = PlatformUI.getWorkbench().getWorkingSetManager();
		IFile rsPrgramFile = project.getFile("src/" + mainFileName);

		Display.getDefault().asyncExec(() -> {

			wsm.addToWorkingSets(project, wizardPage.getWorkingSets());

			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			if (page != null) {
				try {
					if (rsPrgramFile.exists()) {
						IDE.openEditor(page, rsPrgramFile);
					}
				} catch (CoreException e) {
					MessageDialog.openError(getShell(), "Cannot open project", e.toString());
				}
			}
		});
	}

	private File newFolderLocation() {
		IPath workspacePath = ResourcesPlugin.getWorkspace().getRoot().getLocation();
		int appendedNumber = 0;
		File newFile = workspacePath.append("new_rust_project").toFile();
		while (newFile.isDirectory()) {
			appendedNumber++;
			newFile = workspacePath.append("new_rust_project_" + appendedNumber).toFile();
		}
		return newFile;
	}

	private Set<IWorkingSet> getWorkingSets(IResource resource) {
		IWorkingSet[] allWorkingSets = PlatformUI.getWorkbench().getWorkingSetManager().getAllWorkingSets();
		Set<IWorkingSet> fileWorkingSets = new HashSet<>();

		for (IWorkingSet iWorkingSet : allWorkingSets) {
			IAdaptable[] elements = iWorkingSet.getElements();
			if (Arrays.asList(elements).contains(resource.getProject())) {
				fileWorkingSets.add(iWorkingSet);
			}
		}

		return fileWorkingSets;
	}

	private IResource toResource(Object o) {
		if (o instanceof IResource) {
			return (IResource) o;
		} else if (o instanceof IAdaptable) {
			return ((IAdaptable) o).getAdapter(IResource.class);
		} else {
			return null;
		}
	}

	private File toFile(IResource r) {
		IPath location = r.getLocation();
		if (location.toFile().isFile()) {
			return location.toFile().getParentFile().getAbsoluteFile();
		}
		return location == null ? null : location.toFile();
	}

}
