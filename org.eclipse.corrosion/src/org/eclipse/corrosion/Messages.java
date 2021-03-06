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
 *  Nicola Orru - Added support for external RLS startup configuration
 *  Max Bureck (Fraunhofer FOKUS) - Added message for GDB failure
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
	public static String LaunchUI_main;
	public static String LaunchUI_useDefault;
	public static String LaunchUI_variables;
	public static String LaunchUI_selection;
	public static String LaunchUI_browse;
	public static String LaunchUI_project;
	public static String LaunchUI_workingDirectory;
	public static String LaunchUI_invalidCargoProjectName;
	public static String LaunchUI_invalidWorkingDirectory;
	public static String LaunchUI_arguments;
	public static String LaunchUI_options;
	public static String LaunchUI_optionsColon;
	public static String LaunchUI_projectSelection;
	public static String LaunchUI_selectProject;
	public static String DebugPreferencePage_seeGDBPage;
	public static String DebugPreferencePage_defaultGDB;
	public static String ImplementationsSearchQuery_implementations;
	public static String ImplementationsSearchQuery_oneReference;
	public static String ImplementationsSearchQuery_severalReferences;
	public static String ImportPage_title;
	public static String ImportPage_description;
	public static String ImportPage_label_1;
	public static String ImportPage_label_2;
	public static String OptionSelector_title;
	public static String OptionSelector_message;
	public static String OptionSelector_arguments;
	public static String OptionSelector_optionDescription;
	public static String RLSStreamConnectionProvider_OpenPreferences;
	public static String RLSStreamConnectionProvider_requirementsNotFound;
	public static String RLSStreamConnectionProvider_rlsNotFound;
	public static String RLSStreamConnectionProvider_rlsConfigurationNotFound;
	public static String RLSStreamConnectionProvider_rlsConfigurationNotSet;
	public static String RLSStreamConnectionProvider_rlsConfigurationError;
	public static String RLSStreamConnectionProvider_rustSupportNotFound;
	public static String RLSStreamConnectionProvider_unableToSet;
	public static String RustDebugDelegate_unableToLaunch_title;
	public static String RustDebugDelegate_unableToLaunch_message;
	public static String RustDebugTab_selectExecutable_title;
	public static String RustDebugTab_selectExecutable_message;
	public static String RustDebugTab_buildCommand;
	public static String RustDebugTab_cargoCommandConnotBeEmpty;
	public static String RustDebugTab_Executable;
	public static String RustDebugTab_InvalidProjectExecutablePath;
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
	public static String CorrosionPreferencePage_browse;
	public static String CorrosionPreferencePage_browser;
	public static String CorrosionPreferencePage_cannotInstallRustupCargo;
	public static String CorrosionPreferencePage_cannotInstallRustupCargo_details;
	public static String CorrosionPreferencePage_cargoNonExecutable;
	public static String CorrosionPreferencePage_caro;
	public static String CorrosionPreferencePage_workingDirectory;
	public static String CorrosionPreferencePage_emptyPath;
	public static String CorrosionPreferencePage_emptyWorkingDirectory;
	public static String CorrosionPreferencePage_invaildWorkingDirectory;
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
	public static String CorrosionPreferencePage_invalidVersion;
	public static String CorrosionPreferencePage_otherInstallation;
	public static String CorrosionPreferencePage_rlsNonExecutable;
	public static String CorrosionPreferencePage_rlsPath;
	public static String CorrosionPreferencePage_rlsConfigurationPath;
	public static String CorrosionPreferencePage_Rustup;
	public static String CorrosionPreferencePage_rustupNonExecutable;
	public static String CorrosionPreferencePage_rustupMissingRLS;
	public static String CorrosionPreferencePage_sysrootPath;
	public static String CorrosionPreferencePage_toolchain;
	public static String CorrosionPreferencePage_downloadRustAnalyzer;
	public static String CorrosionPreferencePage_downloadingRustAnalyzer;
	public static String NewCargoProjectWizard_cannotCreateRustProject;
	public static String NewCargoProjectWizard_cannotCreateRustProject_partialDeletion;
	public static String NewCargoProjectWizard_cannotOpenProject;
	public static String NewCargoProjectWizard_task;
	public static String NewCargoProjectWizard_title;
	public static String NewCargoProjectWizard_unableToLoadProjectDescriptor;
	public static String NewCargoProjectWizard_cannotCreateRustProject_commandFailedDetails;
	public static String NewCargoProjectWizard_cannotCreateRustProject_commandFailedDetails_partialDeletion;
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
	public static String CargoRunTab_Title;
	public static String CargoTestTab_Title;
	public static String RustDebugTabGroup_gdbErrorMsg;
	public static String LaunchHandler_unableToLaunch;
	public static String LaunchHandler_launchingFromCargoRegistryUnsupported;
	public static String LaunchHandler_unsupportedProjectLocation;
	public static String LaunchHandler_unableToLaunchCommand;

}
