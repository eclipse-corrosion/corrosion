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
package org.eclipse.corrosion.wizards.importexisting;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

public class ImportWizard extends Wizard implements IImportWizard {

	@Override
	public void addPages() {
		addPage(new ImportPage());
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// Nothing to init as there is no action associated with this wizard
	}

	@Override
	public boolean performFinish() {
		// Unable to finish as there is no action associated with this wizard
		return false;
	}

}
