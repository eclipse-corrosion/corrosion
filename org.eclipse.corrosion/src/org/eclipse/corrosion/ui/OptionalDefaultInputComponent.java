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
package org.eclipse.corrosion.ui;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.util.function.Supplier;

import org.eclipse.corrosion.Messages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

public class OptionalDefaultInputComponent extends InputComponent {
	private Button defaultButton;
	private Supplier<String> defaultValueGenerator;

	public OptionalDefaultInputComponent(Composite container, String labelString, ModifyListener editListener,
			Supplier<String> defaultValueGenerator) {
		super(container, labelString, editListener);
		this.defaultValueGenerator = defaultValueGenerator;
	}

	@Override
	public void createComponent() {
		defaultButton = new Button(container, SWT.CHECK);
		defaultButton.setText(Messages.LaunchUI_useDefault);
		defaultButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 4, 1));
		defaultButton.addSelectionListener(widgetSelectedAdapter(e -> updateSelection(defaultButton.getSelection())));
		super.createComponent();
		GridData wdLabelData = getLabelGridData();
		wdLabelData.horizontalIndent = 25;
		setLabelGridData(wdLabelData);
		updateSelection(true);
	}

	public void updateSelection(boolean newSelection) {
		defaultButton.setSelection(newSelection);
		if (newSelection) {
			text.setText(defaultValueGenerator.get());
		}
		setEnabled(!newSelection);
	}

	@Override
	public void createVariableSelection() {
		super.createVariableSelection();
		variableButton.setEnabled(!defaultButton.getSelection());
	}

	@Override
	protected void createSelectionButton() {
		super.createSelectionButton();
		browseButton.setEnabled(!defaultButton.getSelection());
	}

	public boolean getSelection() {
		return defaultButton.getSelection();
	}
}
