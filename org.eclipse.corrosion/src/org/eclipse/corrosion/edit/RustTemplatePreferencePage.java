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
package org.eclipse.corrosion.edit;

import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.corrosion.CorrosionPreferenceInitializer;
import org.eclipse.corrosion.Messages;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.texteditor.templates.TemplatePreferencePage;

public class RustTemplatePreferencePage extends TemplatePreferencePage implements IWorkbenchPreferencePage {

	private Button enableSnippets;
	private IPreferenceStore store;

	public RustTemplatePreferencePage() {
		setTemplateStore(TemplateHelper.getTemplateStore("rust")); //$NON-NLS-1$
		setContextTypeRegistry(TemplateHelper.getContextTypeRegistry());
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return CorrosionPlugin.getDefault().getPreferenceStore();
	}

	@Override
	public void init(IWorkbench workbench) {
		store = getPreferenceStore();
	}

	@Override
	protected boolean isShowFormatterSetting() {
		return false;
	}

	@Override
	protected Control createContents(Composite ancestor) {
		Composite container = new Composite(ancestor, SWT.NULL);
		container.setLayout(new GridLayout(1, false));
		enableSnippets = new Button(container, SWT.CHECK);
		enableSnippets.setText(Messages.TemplatesPreferencePage_enable);
		enableSnippets.setSelection(store.getBoolean(CorrosionPreferenceInitializer.enableTemplates));
		return super.createContents(ancestor);
	}

	@Override
	protected void performDefaults() {
		Boolean enabled = store.getDefaultBoolean(CorrosionPreferenceInitializer.enableTemplates);
		enableSnippets.setSelection(enabled);
		super.performDefaults();
	}

	@Override
	public boolean performOk() {
		store.setValue(CorrosionPreferenceInitializer.enableTemplates, enableSnippets.getSelection());
		return super.performOk();
	}
}