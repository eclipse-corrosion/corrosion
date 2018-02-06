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
package org.eclipse.corrosion.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

public class AddCargoBuilder extends AbstractHandler {

	@Override
	public Object execute(final ExecutionEvent event) {
		final IProject project = getProject(event);

		if (project != null) {
			try {
				if (hasBuilder(project)) {
					return null;
				}

				IProjectDescription description = project.getDescription();
				final ICommand buildCommand = description.newCommand();
				buildCommand.setBuilderName(IncrementalCargoBuilder.BUILDER_ID);

				final List<ICommand> commands = new ArrayList<>();
				commands.addAll(Arrays.asList(description.getBuildSpec()));
				commands.add(buildCommand);

				description.setBuildSpec(commands.toArray(new ICommand[commands.size()]));
				project.setDescription(description, null);

			} catch (final CoreException e) {
			}
		}

		return null;
	}

	public static IProject getProject(final ExecutionEvent event) {
		final ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof IStructuredSelection) {
			final Object element = ((IStructuredSelection) selection).getFirstElement();

			return Platform.getAdapterManager().getAdapter(element, IProject.class);
		}

		return null;
	}

	public static final boolean hasBuilder(final IProject project) {
		try {
			for (final ICommand buildSpec : project.getDescription().getBuildSpec()) {
				if (IncrementalCargoBuilder.BUILDER_ID.equals(buildSpec.getBuilderName())) {
					return true;
				}
			}
		} catch (final CoreException e) {
		}

		return false;
	}
}