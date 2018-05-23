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
package org.eclipse.corrosion;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	static {
		NLS.initializeMessages(Messages.class.getPackage().getName() + ".messages", Messages.class); //$NON-NLS-1$
	}

	public static String CargoExportWizard_cannotCreateProject;
	public static String CargoExportWizard_cannotCreateProject_details;
	public static String CargoExportWizard_cannotPackageProject;
	public static String CargoExportWizard_title;
	public static String CargoExportWizard_unableToGenerateLog;
	public static String CargoRunDelegate_unableToFindProject;
	public static String CargoRunDelegate_unableToFindToml;
	public static String CargoRunDelegate_unableToLaunch;
	public static String CargoRunTab_arguments;
	public static String CargoRunTab_browse;
	public static String CargoRunTab_invalidCargoProjectName;
	public static String CargoRunTab_options;
	public static String CargoRunTab_optionsColon;
	public static String CargoRunTab_project;
	public static String CargoRunTab_projectSelection;
	public static String CargoRunTab_selectProject;
	public static String CargoRunTab_variables;
	public static String DebugPreferencePage_seeGDBPage;
	public static String ImplementationsSearchQuery_implementations;
	public static String ImplementationsSearchQuery_oneReference;
	public static String ImplementationsSearchQuery_severalReferences;
	public static String OptionSelector_title;
	public static String OptionSelector_message;
	public static String OptionSelector_arguments;
	public static String OptionSelector_optionDescription;
	public static String RLSStreamConnectionProvider_OpenPreferences;
	public static String RLSStreamConnectionProvider_requirementsNotFound;
	public static String RLSStreamConnectionProvider_rlsNotFound;
	public static String RLSStreamConnectionProvider_rustSupportNotFound;
	public static String RLSStreamConnectionProvider_unableToSet;
	public static String RustDebugDelegate_unableToLaunch_title;
	public static String RustDebugDelegate_unableToLaunch_message;
	public static String RustDebugTab_project;
	public static String RustDebugTab_browse;
	public static String RustDebugTab_BrowseExecutable;
	public static String RustDebugTab_selectProject;
	public static String RustDebugTab_projectSelection;
	public static String RustDebugTab_buildCommand;
	public static String RustDebugTab_cargoCommandConnotBeEmpty;
	public static String RustDebugTab_Executable;
	public static String RustDebugTab_ExecutableNotExecutable;
	public static String RustDebugTab_InvalidCargoProjectName;
	public static String RustDebugTab_InvalidProjectExecutablePath;
	public static String RustDebugTab_useDefaultPathToExecutable;
	public static String RustDebugTab_variables;
	public static String RustManager_addingRLSPrevios;
	public static String RustManager_addingRustAnalysisRustSrc;
	public static String RustManager_installingToolchain;
	public static String RustManager_rootToolchainSelectionFailure;
	public static String RustManager_settingDefaultToolchain;
	public static String RustManager_settingRLSToolchain;
	public static String RustManager_unableToAddComponent;
	public static String TextEditorPreferencePage_linkColorAndFontsPref;
	public static String TextEditorPreferencePage_linkTextEditorsPref;
	public static String TextEditorPreferencePage_linkTextMatePref;
	public static String ToggleBreakpointsTargetFactory_breakpoint;
	public static String ToggleBreakpointsTargetFactory_breakpointTarget;
	public static String CargoTestTab_testName;
	public static String CargoTestTab_testNameDescription;
	public static String CargoExportWizard_commandFailed;
	public static String CargoExportWizardPage_allowDirtyDirectories;
	public static String CargoExportWizardPage_browse;
	public static String CargoExportWizardPage_cargoCommandNotFound;
	public static String CargoExportWizardPage_description;
	public static String CargoExportWizardPage_dontVerifyContent;
	public static String CargoExportWizardPage_ignoreWarningMatadata;
	public static String CargoExportWizardPage_invalidCargoProject;
	public static String CargoExportWizardPage_project;
	public static String CargoExportWizardPage_projectSelection;
	public static String CargoExportWizardPage_selectProject;
	public static String CargoExportWizardPage_title;
	public static String CargoExportWizardPage_toolchain;
	public static String CargoExportWizardPage_currentToolchain;
	public static String CargoExportWizardPage_outputLocation;
	public static String CLIOption_lineIsNotHelp;
	public static String CorrosionPreferenceInitializer_29;
	public static String CorrosionPreferenceInitializer_30;
	public static String CorrosionPreferenceInitializer_31;
	public static String CorrosionPreferencePage_browse;
	public static String CorrosionPreferencePage_browser;
	public static String CorrosionPreferencePage_cannotInstallRustupCargo;
	public static String CorrosionPreferencePage_cannotInstallRustupCargo_details;
	public static String CorrosionPreferencePage_cargoNonExecutable;
	public static String CorrosionPreferencePage_caro;
	public static String CorrosionPreferencePage_disableRustEdition;
	public static String CorrosionPreferencePage_emptyPath;
	public static String CorrosionPreferencePage_emptyRustupCargoPath;
	public static String CorrosionPreferencePage_emptyToolchain;
	public static String CorrosionPreferencePage_id;
	public static String CorrosionPreferencePage_install;
	public static String CorrosionPreferencePage_installed;
	public static String CorrosionPreferencePage_installing;
	public static String CorrosionPreferencePage_installingRustupCargo;
	public static String CorrosionPreferencePage_invalidCargo;
	public static String CorrosionPreferencePage_invalidRlsPath;
	public static String CorrosionPreferencePage_invalidRustup;
	public static String CorrosionPreferencePage_invalidSysroot;
	public static String CorrosionPreferencePage_otherInstallation;
	public static String CorrosionPreferencePage_rlsLocation;
	public static String CorrosionPreferencePage_rlsNonExecutable;
	public static String CorrosionPreferencePage_rlsPath;
	public static String CorrosionPreferencePage_Rustup;
	public static String CorrosionPreferencePage_rustupNonExecutable;
	public static String CorrosionPreferencePage_sysrootPath;
	public static String CorrosionPreferencePage_toolchain;
	public static String CorrosionPreferencePage_useDefaultPathsRustupCargo;
	public static String CorrosionPreferencePage_useRustup;
	public static String NewCargoProjectWizard_cannotCreateRustProject;
	public static String NewCargoProjectWizard_cannotOpenProject;
	public static String NewCargoProjectWizard_task;
	public static String NewCargoProjectWizard_title;
	public static String NewCargoProjectWizard_unableToLoadProjectDescriptor;
	public static String NewCargoProjectWizard_cannotCreateRustProject_commandFailedDetails;
	public static String NewCargoProjectWizardPage_browse;
	public static String NewCargoProjectWizardPage_cannotCreateDirectory;
	public static String NewCargoProjectWizardPage_cannotWriteInDirectory;
	public static String NewCargoProjectWizardPage_cargoCommandNotFound;
	public static String NewCargoProjectWizardPage_description;
	public static String NewCargoProjectWizardPage_emptyDirectory;
	public static String NewCargoProjectWizardPage_emptyProjectName;
	public static String NewCargoProjectWizardPage_fileExisting;
	public static String NewCargoProjectWizardPage_InvalidDotProjectInDirectory;
	public static String NewCargoProjectWizardPage_invalidProjectName;
	public static String NewCargoProjectWizardPage_linkNameAndFolder;
	public static String NewCargoProjectWizardPage_location;
	public static String NewCargoProjectWizardPage_projectName;
	public static String NewCargoProjectWizardPage_projectNameAlreadyUsed;
	public static String NewCargoProjectWizardPage_projectNameDoesntMatchDotProject;
	public static String NewCargoProjectWizardPage_title;
	public static String NewCargoProjectWizardPage_useTemplate;
	public static String NewCargoProjectWizardPage_useVCS;
	public static String RustManager_unableToInstallToolchain;
	public static String RustManager_unableToSetDefaultToolchain;
	public static String RustManager_toolchainDoesntIncludeRLS;
}
