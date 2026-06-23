/*******************************************************************************
 * Copyright (c) 2025 Contributors to the Eclipse Foundation.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   See git history
 *******************************************************************************/

package org.eclipse.corrosion.edit;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.corrosion.CorrosionPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.lsp4e.format.IFormatRegionsProvider;
import org.osgi.service.component.annotations.Component;

@Component(property = { "serverDefinitionId:String=org.eclipse.corrosion.rls" })
public class FormatOnSave implements IFormatRegionsProvider {

	@Override
	public IRegion[] getFormattingRegions(IDocument document) {
		IPreferenceStore store = CorrosionPlugin.getDefault().getPreferenceStore();
		var file = LSPEclipseUtils.getFile(document);
		if (file != null) {
			if (store.getBoolean(CorrosionPreferenceInitializer.EDIT_FORMAT_ON_SAVE_PREFERENCE)) {
				if (store.getBoolean(CorrosionPreferenceInitializer.EDIT_FORMAT_ALL_ON_SAVE_PREFERENCE)) {
					return IFormatRegionsProvider.allLines(document);
				}
				if (store.getBoolean(CorrosionPreferenceInitializer.EDIT_FORMAT_EDITED_ON_SAVE_PREFERENCE)) {
					return IFormatRegionsProvider.calculateEditedLineRegions(document, new NullProgressMonitor());
				}
			}
		}
		return null;
	}

}