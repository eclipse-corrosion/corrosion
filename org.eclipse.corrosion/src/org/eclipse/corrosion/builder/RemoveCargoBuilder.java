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
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;

public class RemoveCargoBuilder extends AbstractHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final IProject project = AddCargoBuilder.getProject(event);

		if (project != null) {
			try {
				final IProjectDescription description = project.getDescription();
				final List<ICommand> commands = new ArrayList<>();
				commands.addAll(Arrays.asList(description.getBuildSpec()));

				for (final ICommand buildSpec : description.getBuildSpec()) {
					if (IncrementalCargoBuilder.BUILDER_ID.equals(buildSpec.getBuilderName())) {
						commands.remove(buildSpec);
					}
				}

				description.setBuildSpec(commands.toArray(new ICommand[commands.size()]));
				project.setDescription(description, null);
			} catch (final CoreException e) {
			}
		}

		return null;
	}
}