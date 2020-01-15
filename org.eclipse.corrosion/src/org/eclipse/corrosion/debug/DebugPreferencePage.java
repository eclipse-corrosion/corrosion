/*********************************************************************
 * Copyright (c) 2018, 2020 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Mickael Istria (Red Hat Inc.) - Initial implementation
 *  Max Bureck - Adding default GDB property
 *******************************************************************************/
package org.eclipse.corrosion.debug;

import org.eclipse.cdt.dsf.gdb.launching.LaunchUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.corrosion.CorrosionPreferenceInitializer;
import org.eclipse.corrosion.Messages;
import org.eclipse.corrosion.ui.InputComponent;
import org.eclipse.jface.preference.IPreferencePageContainer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

public class DebugPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private InputComponent gdbInput = null;
	private IPreferenceStore store = null;

	@Override
	public void init(IWorkbench workbench) {
		store = doGetPreferenceStore();
	}

	@Override
	public boolean performOk() {
		store.setValue(CorrosionPreferenceInitializer.DEFAULT_GDB_PREFERENCE, gdbInput.getValue());
		return true;
	}

	@Override
	protected Control createContents(Composite parent) {
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(4, false));
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));

		gdbInput = new InputComponent(container, Messages.DebugPreferencePage_defaultGDB, e -> isPageValid());
		gdbInput.createComponent();
		gdbInput.createFileSelection();
		gdbInput.setValue(store.getString(CorrosionPreferenceInitializer.DEFAULT_GDB_PREFERENCE));

		Link gdbLink = new Link(container, SWT.NONE);
		gdbLink.setText(Messages.DebugPreferencePage_seeGDBPage);
		gdbLink.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			IPreferencePageContainer prefContainer = getContainer();
			if (prefContainer instanceof IWorkbenchPreferenceContainer) {
				((IWorkbenchPreferenceContainer) prefContainer).openPage("org.eclipse.cdt.dsf.gdb.ui.preferences", //$NON-NLS-1$
						null);
			}
		}));
		gdbLink.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1));
		return parent;
	}

	private boolean isPageValid() {
		setErrorMessage(null);
		String gdbCommand = gdbInput.getValue();
		try {
			LaunchUtils.getGDBVersion(gdbCommand, new String[] {});
		} catch (CoreException e) {
			final String msg = DebugUtil.getMessageFromGdbExecutionException(e);
			setErrorMessage(msg);
			return false;
		} finally {
			getContainer().updateMessage();
		}
		return true;
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return CorrosionPlugin.getDefault().getPreferenceStore();
	}

	@Override
	protected void performDefaults() {
		gdbInput.setValue(store.getDefaultString(CorrosionPreferenceInitializer.DEFAULT_GDB_PREFERENCE));
		super.performDefaults();
	}
}