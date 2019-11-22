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
 *  Max Bureck (Fraunhofer FOKUS) - Check for valid GDB executable/script
 *******************************************************************************/
package org.eclipse.corrosion.debug;

import static org.eclipse.corrosion.debug.DebugUtil.DEFAULT_DEBUGGER;

import org.eclipse.cdt.debug.core.ICDTLaunchConfigurationConstants;
import org.eclipse.cdt.dsf.gdb.IGDBLaunchConfigurationConstants;
import org.eclipse.cdt.dsf.gdb.internal.ui.launching.LocalApplicationCDebuggerTab;
import org.eclipse.cdt.dsf.gdb.launching.LaunchUtils;
import org.eclipse.cdt.launch.ui.CArgumentsTab;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.corrosion.Messages;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.sourcelookup.SourceLookupTab;
import org.eclipse.osgi.util.NLS;

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
			config.setAttribute(IGDBLaunchConfigurationConstants.ATTR_DEBUG_NAME, DEFAULT_DEBUGGER);
		}

		@Override
		public boolean isValid(ILaunchConfiguration config) {
			if (!super.isValid(config)) {
				return false;
			}
			// Check for a valid GDB executable/script
			String gdbCommand = getGdbCommand(config);
			try {
				LaunchUtils.getGDBVersion(gdbCommand, new String[] {});
			} catch (CoreException e) {
				final String msg = getMessageFromException(e);
				setErrorMessage(msg);
				return false;
			}
			return true;
		}

		private String getMessageFromException(CoreException e) {
			final IStatus status = e.getStatus();
			final String statusMessage = status.getMessage();
			final Throwable statusException = status.getException();
			if (statusException != null) {
				String exceptionMessage = statusException.getLocalizedMessage();
				return NLS.bind(Messages.RustDebugTabGroup_gdbErrorMsg, statusMessage, exceptionMessage);
			}
			// else
			return statusMessage;
		}

		/**
		 * Reads and returns the user specified gdb executable from the given
		 * {@code config}.
		 */
		private String getGdbCommand(ILaunchConfiguration config) {
			try {
				return config.getAttribute(IGDBLaunchConfigurationConstants.ATTR_DEBUG_NAME, DEFAULT_DEBUGGER);
			} catch (CoreException e) {
				// we were not able to find command
				return DEFAULT_DEBUGGER;
			}
		}
	}
}
