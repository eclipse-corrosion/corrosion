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

import org.eclipse.jface.text.templates.GlobalTemplateVariables;
import org.eclipse.jface.text.templates.TemplateContextType;

public class CorrosionContextType extends TemplateContextType {
	public static final String RUST_CONTEXT = "org.eclipse.corrosion.templates.rust"; //$NON-NLS-1$

	public CorrosionContextType() {
		addResolver(new GlobalTemplateVariables.Cursor());
	}
}
