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
package org.eclipse.corrosion.debug;

import org.eclipse.cdt.debug.core.ICDTLaunchConfigurationConstants;
import org.eclipse.cdt.dsf.gdb.IGDBLaunchConfigurationConstants;
import org.eclipse.cdt.dsf.gdb.internal.ui.launching.LocalApplicationCDebuggerTab;
import org.eclipse.cdt.launch.ui.CArgumentsTab;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.sourcelookup.SourceLookupTab;

@SuppressWarnings("restriction")
public class RustDebugTabGroup extends AbstractLaunchConfigurationTabGroup {

	@Override
	public void createTabs(ILaunchConfigurationDialog arg0, String arg1) {
		setTabs(new ILaunchConfigurationTab[] { new RustDebugTab(), new CArgumentsTab(), new EnvironmentTab(),
				new RustLocalApplicationCDebuggerTab(), new SourceLookupTab(), new CommonTab() });
	}

	protected class RustLocalApplicationCDebuggerTab extends LocalApplicationCDebuggerTab {

		@Override
		public void setDefaults(ILaunchConfigurationWorkingCopy config) {
			super.setDefaults(config);
			config.setAttribute(ICDTLaunchConfigurationConstants.ATTR_DEBUGGER_STOP_AT_MAIN, false);
			config.setAttribute(IGDBLaunchConfigurationConstants.ATTR_DEBUG_NAME, "rust-gdb"); //$NON-NLS-1$
		}
	}
}
