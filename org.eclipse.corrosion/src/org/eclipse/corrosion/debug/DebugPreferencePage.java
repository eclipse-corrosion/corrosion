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
 *  Mickael Istria (Red Hat Inc.) - Initial implementation
 *******************************************************************************/
package org.eclipse.corrosion.debug;

import org.eclipse.corrosion.Messages;
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

public class DebugPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	@Override public void init(IWorkbench workbench) {
		noDefaultAndApplyButton();
	}

	@Override protected Control createContents(Composite parent) {
		parent.setLayoutData(new GridData(SWT.NONE, SWT.TOP, true, false));
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(1, false));

		Link gdbLink = new Link(container, SWT.NONE);
		gdbLink.setText(Messages.DebugPreferencePage_seeGDBPage);
		gdbLink.addSelectionListener(new SelectionAdapter() {
			@Override public void widgetSelected(SelectionEvent e) {
				if (getContainer() instanceof IWorkbenchPreferenceContainer) {
					((IWorkbenchPreferenceContainer) getContainer()).openPage("org.eclipse.cdt.dsf.gdb.ui.preferences", null); //$NON-NLS-1$
				}
			}
		});

		return parent;
	}
}