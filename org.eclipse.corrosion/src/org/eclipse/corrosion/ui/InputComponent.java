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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Path;
import org.eclipse.corrosion.Messages;
import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.eclipse.ui.dialogs.ListSelectionDialog;
import org.eclipse.ui.dialogs.ResourceSelectionDialog;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class InputComponent {
	protected Label label;
	protected Text text;
	protected Button browseButton;
	protected Button variableButton;

	protected Composite container;
	protected String labelString;
	protected ModifyListener editListener;
	protected GridData labelGridData = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
	protected GridData textGridData = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);

	public InputComponent(Composite container, String labelString, ModifyListener editListener) {
		this.container = container;
		this.labelString = labelString;
		this.editListener = editListener;
	}

	public void createComponent() {
		label = new Label(container, SWT.NONE);
		label.setText(this.labelString);
		label.setLayoutData(labelGridData);

		text = new Text(container, SWT.BORDER);
		text.setLayoutData(textGridData);
		text.addModifyListener(editListener);
	}

	public String getValue() {
		return text.getText();
	}

	public void setValue(String value) {
		text.setText(value);
		editListener.modifyText(null);
	}

	public void createVariableSelection() {
		makeSpaceForButton();
		variableButton = new Button(container, SWT.NONE);
		variableButton.setText(Messages.LaunchUI_variables);
		variableButton.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
		variableButton.addSelectionListener(widgetSelectedAdapter(e -> {
			StringVariableSelectionDialog variableSelector = new StringVariableSelectionDialog(
					variableButton.getShell());
			int returnCode = variableSelector.open();
			String result = variableSelector.getVariableExpression();
			if (returnCode == 0 && result != null) {
				text.append(result);
				editListener.modifyText(null);
			}
		}));
	}

	public void createProjectSelection() {
		createSelectionButton();
		browseButton.addSelectionListener(widgetSelectedAdapter(e -> {
			ListSelectionDialog dialog = new ListSelectionDialog(browseButton.getShell(),
					ResourcesPlugin.getWorkspace().getRoot(), new BaseWorkbenchContentProvider(),
					new WorkbenchLabelProvider(), this.labelString);
			dialog.setTitle(Messages.LaunchUI_selection);
			int returnCode = dialog.open();
			Object[] results = dialog.getResult();
			if (returnCode == 0 && results.length > 0) {
				text.setText(((IProject) results[0]).getName());
				editListener.modifyText(null);
			}
		}));
	}

	public void createResourceSelection(Supplier<IAdaptable> rootSupplier) {
		createSelectionButton();
		browseButton.addSelectionListener(widgetSelectedAdapter(e -> {
			ResourceSelectionDialog dialog = new ResourceSelectionDialog(browseButton.getShell(), rootSupplier.get(),
					this.labelString);
			dialog.setTitle(Messages.LaunchUI_selection);
			int returnCode = dialog.open();
			Object[] results = dialog.getResult();
			if (returnCode == 0 && results.length > 0) {
				text.setText(((IResource) results[0]).getFullPath().makeRelative().toString());
				editListener.modifyText(null);
			}
		}));
	}

	public void createContainerSelection(Supplier<IContainer> containerSupplier) {
		createSelectionButton();
		browseButton.addSelectionListener(widgetSelectedAdapter(e -> {
			ContainerSelectionDialog dialog = new ContainerSelectionDialog(browseButton.getShell(),
					containerSupplier.get(), true, this.labelString);
			dialog.setTitle(Messages.LaunchUI_selection);
			int returnCode = dialog.open();
			Object[] results = dialog.getResult();
			if (returnCode == 0 && results.length > 0) {
				text.setText(((Path) results[0]).makeRelative().toString());
				editListener.modifyText(null);
			}
		}));
	}

	public void createFileSelection() {
		createSelectionButton();
		browseButton.addSelectionListener(widgetSelectedAdapter(e -> {
			FileDialog dialog = new FileDialog(browseButton.getShell());
			String path = dialog.open();
			if (path != null) {
				text.setText(path);
				editListener.modifyText(null);
			}
		}));
	}

	public void createFolderSelection() {
		createSelectionButton();
		browseButton.addSelectionListener(widgetSelectedAdapter(e -> {
			DirectoryDialog dialog = new DirectoryDialog(browseButton.getShell());
			String path = dialog.open();
			if (path != null) {
				text.setText(path);
				editListener.modifyText(null);
			}
		}));
	}

	public GridData getLabelGridData() {
		return labelGridData;
	}

	public void setLabelGridData(GridData labelGridData) {
		this.labelGridData = labelGridData;
		label.setLayoutData(this.labelGridData);
	}

	public GridData getTextGridData() {
		return textGridData;
	}

	public void setTextGridData(GridData textGridData) {
		this.textGridData = textGridData;
		text.setLayoutData(this.textGridData);
	}

	public void setEnabled(boolean enabled) {
		label.setEnabled(enabled);
		text.setEnabled(enabled);
		if (browseButton != null) {
			browseButton.setEnabled(enabled);
		}
		if (variableButton != null) {
			variableButton.setEnabled(enabled);
		}
	}

	public void makeSpaceForButton() {
		GridData data = getTextGridData();
		int horizontalSpan = data.horizontalSpan;
		if (horizontalSpan > 1)
			data.horizontalSpan = horizontalSpan - 1;
		setTextGridData(data);
	}

	protected void createSelectionButton() {
		makeSpaceForButton();
		browseButton = new Button(container, SWT.NONE);
		browseButton.setText(Messages.CorrosionPreferencePage_browse);
		browseButton.setToolTipText(Messages.LaunchUI_browse);
		browseButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
	}
}
