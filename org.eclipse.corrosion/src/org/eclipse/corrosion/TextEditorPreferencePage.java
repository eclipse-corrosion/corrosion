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
package org.eclipse.corrosion;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

public class TextEditorPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	@Override
	public void init(IWorkbench workbench) {
		noDefaultAndApplyButton();
	}

	@Override
	protected Control createContents(Composite parent) {
		parent.setLayoutData(new GridData(SWT.NONE, SWT.TOP, true, false));
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(1, false));

		Link textEditorsLink = new Link(container, SWT.NONE);
		textEditorsLink.setText(
				"See <A>Text Editors</A> for general text editor settings (including setting tabs/spaces for formatting)");
		textEditorsLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (getContainer() instanceof IWorkbenchPreferenceContainer) {
					((IWorkbenchPreferenceContainer) getContainer())
					.openPage("org.eclipse.ui.preferencePages.GeneralTextEditor", null);
				}
			}
		});

		Link textMateLink = new Link(container, SWT.NONE);
		textMateLink.setText("See <A>TextMate Theme</A> to configure the syntax highlighting");
		textMateLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (getContainer() instanceof IWorkbenchPreferenceContainer) {
					((IWorkbenchPreferenceContainer) getContainer())
					.openPage("org.eclipse.tm4e.ui.preferences.ThemePreferencePage", null);
				}
			}
		});

		Link fontsLink = new Link(container, SWT.NONE);
		fontsLink.setText("See <A>Colors and Fonts</A> to configure the font");
		fontsLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (getContainer() instanceof IWorkbenchPreferenceContainer) {
					((IWorkbenchPreferenceContainer) getContainer())
					.openPage("org.eclipse.ui.preferencePages.ColorsAndFonts", null);
				}
			}
		});

		return parent;
	}
}