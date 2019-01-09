/*********************************************************************
 * Copyright (c) 2017 Red Hat Inc. and others.
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
package org.eclipse.corrosion.test;

import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.corrosion.Messages;
import org.eclipse.corrosion.ui.launch.AbstractCargoLaunchConfigurationTab;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class CargoTestTab extends AbstractCargoLaunchConfigurationTab {
	private Text testnameText;

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(CargoTestDelegate.TEST_NAME_ATTRIBUTE, testnameText.getText());
		super.performApply(configuration);
	}

	@Override
	protected String getCommandGroupText() {
		return "cargo test [options] [test name] [--] [arguments]"; //$NON-NLS-1$
	}

	@Override
	protected String getCargoSubcommand() {
		return "test"; //$NON-NLS-1$
	}

	@Override
	protected Group createExtraControlsGroup(Composite container) {
		Group commandGroup = super.createExtraControlsGroup(container);
		Label testnameLabel = new Label(commandGroup, SWT.NONE);
		testnameLabel.setText(Messages.CargoTestTab_testName);
		testnameLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		testnameText = new Text(commandGroup, SWT.BORDER);
		testnameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		testnameText.addModifyListener(e -> {
			setDirty(true);
			updateLaunchConfigurationDialog();
		});
		new Label(commandGroup, SWT.NONE);

		new Label(commandGroup, SWT.NONE);
		Label testnameExplanation = new Label(commandGroup, SWT.NONE);
		testnameExplanation.setText(Messages.CargoTestTab_testNameDescription);
		testnameExplanation.setEnabled(false);
		testnameExplanation.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1));
		return commandGroup;
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		super.setDefaults(configuration);
		configuration.setAttribute(CargoTestDelegate.TEST_NAME_ATTRIBUTE, ""); //$NON-NLS-1$
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		super.initializeFrom(configuration);
		try {
			testnameText.setText(configuration.getAttribute(CargoTestDelegate.TEST_NAME_ATTRIBUTE, "")); //$NON-NLS-1$
		} catch (CoreException ce) {
			testnameText.setText(""); //$NON-NLS-1$
		}
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
		return Messages.CargoTestTab_Title;
	}
}
