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
 *  Lucas Bullen   (Red Hat Inc.) - Initial implementation
 *******************************************************************************/
package org.eclipse.corrosion.cargo.ui;

import java.util.List;

import org.eclipse.corrosion.Messages;
import org.eclipse.corrosion.cargo.core.CLIOption;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

@SuppressWarnings("restriction")
public class OptionSelector extends ElementListSelectionDialog {
	private Text fArgumentText;
	private Text fDescriptionText;

	private CLIOption selection;
	private String argumentString;

	public OptionSelector(Shell parent, List<CLIOption> options) {
		super(parent, new OptionLabelProvider());
		setShellStyle(getShellStyle() | SWT.RESIZE);
		setTitle(Messages.OptionSelector_title);
		setMessage(Messages.OptionSelector_message);
		setMultipleSelection(false);
		setElements(options.toArray(new CLIOption[options.size()]));
	}

	/**
	 * Returns the option the user generated from this dialog, or <code>null</code> if none.
	 *
	 * @return option expression the user generated from this dialog, or <code>null</code> if none
	 */
	public String returnOptionSelection() {
		if (selection == null) {
			return null;
		}
		String returnString = selection.getFlag();
		if (argumentString != null && !argumentString.isEmpty()) {
			returnString += ' ' + argumentString;
		}
		return returnString;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets. Composite)
	 */
	@Override protected Control createDialogArea(Composite parent) {
		Control control = super.createDialogArea(parent);
		createArgumentArea((Composite) control);
		return control;
	}

	/**
	 * Creates an area to display a description of the selected option and a field to configure the options's argument.
	 *
	 * @param parent
	 *            parent widget
	 */
	private void createArgumentArea(Composite parent) {
		Composite container = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_HORIZONTAL);
		new Label(container, SWT.NONE).setText(Messages.OptionSelector_arguments);
		fArgumentText = new Text(container, SWT.BORDER);
		fArgumentText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fArgumentText.setEnabled(false);
		fArgumentText.addModifyListener(e -> {
			argumentString = fArgumentText.getText();
		});

		new Label(container, SWT.NONE).setText(Messages.OptionSelector_optionDescription);
		fDescriptionText = new Text(container, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		fDescriptionText.setFont(container.getFont());
		fDescriptionText.setEditable(false);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		gd.heightHint = 50;
		fDescriptionText.setLayoutData(gd);
	}

	@Override protected void handleSelectionChanged() {
		fArgumentText.setText(""); //$NON-NLS-1$
		fArgumentText.setEnabled(false);
		fDescriptionText.setText(""); //$NON-NLS-1$
		if (getSelectionIndex() == -1) {
			selection = null;
			return;
		}
		selection = (CLIOption) getSelectedElements()[0];
		String[] arguments = selection.getArguments();
		if (arguments != null) {
			fArgumentText.setText(String.join(" ", arguments)); //$NON-NLS-1$
			fArgumentText.setEnabled(true);
		}
		String description = selection.getDescription();
		if (description != null) {
			fDescriptionText.setText(description);
		}
		super.handleSelectionChanged();
	}
}
