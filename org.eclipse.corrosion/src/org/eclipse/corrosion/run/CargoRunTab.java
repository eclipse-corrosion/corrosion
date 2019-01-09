/*********************************************************************
 * Copyright (c) 2017, 2019 Red Hat Inc. and others.
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
package org.eclipse.corrosion.run;

import java.net.URL;

import org.eclipse.corrosion.Messages;
import org.eclipse.corrosion.ui.launch.AbstractCargoLaunchConfigurationTab;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class CargoRunTab extends AbstractCargoLaunchConfigurationTab {

	@Override
	protected String getCommandGroupText() {
		return "cargo run [options] [--] [arguments]"; //$NON-NLS-1$
	}

	@Override
	protected String getCargoSubcommand() {
		return "run"; //$NON-NLS-1$
	}

	@Override
	public Image getImage() {
		Bundle bundle = FrameworkUtil.getBundle(this.getClass());
		URL url = bundle.getEntry("images/cargo16.png"); //$NON-NLS-1$
		ImageDescriptor imageDescriptor = ImageDescriptor.createFromURL(url);
		return imageDescriptor.createImage();
	}

	@Override
	public String getName() {
		return Messages.CargoRunTab_Title;
	}

}
