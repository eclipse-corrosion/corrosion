package org.eclipse.corrosion.edit;

import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.corrosion.CorrosionPreferenceInitializer;
import org.eclipse.corrosion.Messages;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class SaveActionsPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	private IPreferenceStore store;

	private Button format;
	private Button formatAll;
	private Button formatEdited;

	@Override
	public void init(IWorkbench workbench) {
		store = CorrosionPlugin.getDefault().getPreferenceStore();
	}

	@Override
	protected Control createContents(Composite parent) {
		parent.setLayoutData(new GridData(SWT.NONE, SWT.TOP, true, false));

		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		composite.setLayout(GridLayoutFactory.fillDefaults().create());

		format = createButton(Messages.SaveActionsConfigurationPage_FormatSourceCode,
				Messages.SaveActionsConfigurationPage_FormatSourceCode_description, composite, SWT.CHECK, 0);
		formatAll = createButton(Messages.SaveActionsConfigurationPage_FormatAllLines,
				Messages.SaveActionsConfigurationPage_FormatAllLines_description, composite, SWT.RADIO, 15);
		formatEdited = createButton(Messages.SaveActionsConfigurationPage_FormatEditedLines,
				Messages.SaveActionsConfigurationPage_FormatEditedLines_description, composite, SWT.RADIO, 15);

		format.setSelection(store.getBoolean(CorrosionPreferenceInitializer.EDIT_FORMAT_ON_SAVE_PREFERENCE));
		formatAll.setSelection(store.getBoolean(CorrosionPreferenceInitializer.EDIT_FORMAT_ALL_ON_SAVE_PREFERENCE));
		formatEdited
				.setSelection(store.getBoolean(CorrosionPreferenceInitializer.EDIT_FORMAT_EDITED_ON_SAVE_PREFERENCE));

		// listener to update enable state for the radio buttons
		format.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateEnabledState();
			}
		});

		updateEnabledState();
		return parent;

	}

	@Override
	protected void performDefaults() {
		format.setSelection(store.getDefaultBoolean(CorrosionPreferenceInitializer.EDIT_FORMAT_ON_SAVE_PREFERENCE));
		formatAll.setSelection(
				store.getDefaultBoolean(CorrosionPreferenceInitializer.EDIT_FORMAT_ALL_ON_SAVE_PREFERENCE));
		formatEdited.setSelection(
				store.getDefaultBoolean(CorrosionPreferenceInitializer.EDIT_FORMAT_EDITED_ON_SAVE_PREFERENCE));
		updateEnabledState();
		super.performDefaults();
	}

	@Override
	public boolean performOk() {
		store.setValue(CorrosionPreferenceInitializer.EDIT_FORMAT_ON_SAVE_PREFERENCE, format.getSelection());
		store.setValue(CorrosionPreferenceInitializer.EDIT_FORMAT_ALL_ON_SAVE_PREFERENCE, formatAll.getSelection());
		store.setValue(CorrosionPreferenceInitializer.EDIT_FORMAT_EDITED_ON_SAVE_PREFERENCE,
				formatEdited.getSelection());
		return true;
	}

	private static Button createButton(String name, String description, Composite composite, int style,
			int horizontalIndent) {
		Button button = new Button(composite, style);
		button.setLayoutData(GridDataFactory.fillDefaults().indent(horizontalIndent, 0).create());
		button.setText(name);
		button.setToolTipText(description);
		return button;
	}

	private void updateEnabledState() {
		boolean formatOnSaveIsEnabled = format.getSelection();
		formatAll.setEnabled(formatOnSaveIsEnabled);
		formatEdited.setEnabled(formatOnSaveIsEnabled);
	}
}
