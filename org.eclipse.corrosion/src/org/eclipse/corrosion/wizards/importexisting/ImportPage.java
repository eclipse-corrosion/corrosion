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

import org.eclipse.corrosion.Messages;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class ImportPage extends WizardPage {
	public ImportPage() {
		super(Messages.ImportPage_title);
		setTitle(Messages.ImportPage_title);
		setDescription(Messages.ImportPage_description);
	}

	@Override
	public void createControl(Composite parent) {
		parent.setLayoutData(new GridData(SWT.NONE, SWT.TOP, true, false));
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(1, false));
		setControl(container);
		Label label1 = new Label(container, SWT.NONE);
		label1.setText(Messages.ImportPage_label_1);
		Label label2 = new Label(container, SWT.NONE);
		label2.setText(Messages.ImportPage_label_2);

	}
}
