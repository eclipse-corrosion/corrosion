/*********************************************************************
 * Copyright (c) 2017, 2021 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Lucas Bullen (Red Hat Inc.) - Initial implementation
 *  Max Bureck (Fraunhofer FOKUS) - Registering additional image for debug launcher
 *******************************************************************************/
package org.eclipse.corrosion;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.corrosion.cargo.core.CargoTools;
import org.eclipse.corrosion.resources.ResourceLookup;
import org.eclipse.corrosion.sourcelookup.CargoAbsolutePathSourceContainer;
import org.eclipse.corrosion.sourcelookup.CargoCommonSourceLookupDirector;
import org.eclipse.corrosion.sourcelookup.CargoProjectSourceContainer;
import org.eclipse.corrosion.sourcelookup.CargoSourceLookupDirector;
import org.eclipse.corrosion.sourcelookup.CargoSourceUtils;
import org.eclipse.corrosion.sourcelookup.ICargoSourceLocation;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.ecf.filetransfer.service.IRetrieveFileTransfer;
import org.eclipse.ecf.filetransfer.service.IRetrieveFileTransferFactory;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.util.tracker.ServiceTracker;

public class CorrosionPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.corrosion"; //$NON-NLS-1$

	private static BundleContext context;

	/**
	 * Status code indicating an unexpected internal error.
	 */
	public static final int INTERNAL_ERROR = 1000;

	// The shared instance
	private static CorrosionPlugin plugin;

	private ServiceTracker<IRetrieveFileTransferFactory, IRetrieveFileTransferFactory> retrievalFactoryTracker;

	private static synchronized void setSharedInstance(CorrosionPlugin newValue) {
		plugin = newValue;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		CorrosionPlugin.context = context;
		setSharedInstance(this);
		ResourceLookup.startup();
		initializeCommonSourceLookupDirector();
		Job.create("Import .cargo in workspace", //$NON-NLS-1$
				(ICoreRunnable) (monitor -> CargoTools.ensureDotCargoImportedAsProject(monitor))).schedule();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if (retrievalFactoryTracker != null) {
			retrievalFactoryTracker.close();
		}
		setSharedInstance(null);
		disposeCommonSourceLookupDirector();
		ResourceLookup.shutdown();
		super.stop(context);
	}

	public static BundleContext getContext() {
		return context;
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static CorrosionPlugin getDefault() {
		return plugin;
	}

	/**
	 * Logs the specified message with this plug-in's log.
	 *
	 * @param status status to log
	 */
	public static void logError(String message) {
		getDefault().getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, INTERNAL_ERROR, message, null));
	}

	public static void logError(Throwable t) {
		logError(new Status(IStatus.ERROR, PLUGIN_ID, t.getMessage(), t));
	}

	public static void logError(IStatus s) {
		getDefault().getLog().log(s);
	}

	public static void showError(String title, String message, Exception exception) {
		CorrosionPlugin.showError(title, message + '\n' + exception.getLocalizedMessage());
	}

	public static void showError(String title, String message) {
		Display.getDefault().asyncExec(() -> {
			MessageDialog dialog = new MessageDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
					title, null, message, MessageDialog.ERROR, 0, IDialogConstants.OK_LABEL);
			dialog.setBlockOnOpen(false);
			dialog.open();
		});
	}

	@Override
	protected void initializeImageRegistry(ImageRegistry reg) {
		super.initializeImageRegistry(reg);
		declareRegistryImage(reg, "images/cargo.png"); //$NON-NLS-1$
		declareRegistryImage(reg, "images/cargo16.png"); //$NON-NLS-1$
		declareRegistryImage(reg, "icons/rustEditorIcon.png"); //$NON-NLS-1$
	}

	private final static void declareRegistryImage(ImageRegistry reg, String image) {
		ImageDescriptor desc = ImageDescriptor.getMissingImageDescriptor();
		URL url = null;
		Bundle bundle = plugin.getBundle();
		if (bundle != null) {
			url = FileLocator.find(bundle, new Path(image), null);
			if (url != null) {
				desc = ImageDescriptor.createFromURL(url);
			}
		}
		reg.put(image, desc);
	}

	public static boolean validateCommandVersion(String[] commandStrings, Pattern matchPattern) {
		String[] command = new String[1 + commandStrings.length];

		System.arraycopy(commandStrings, 0, command, 0, commandStrings.length);
		command[commandStrings.length] = "--version"; //$NON-NLS-1$

		return matchPattern.matcher(getOutputFromCommand(command)).matches();
	}

	public static boolean validateCommandVersion(String commandPath, Pattern matchPattern) {
		return matchPattern.matcher(getOutputFromCommand(commandPath, "--version")).matches(); //$NON-NLS-1$
	}

	public static Process getProcessForCommand(String... commandStrings) throws IOException {
		ProcessBuilder builder = new ProcessBuilder(commandStrings);
		builder.directory(getWorkingDirectoryFromPreferences());
		return builder.start();
	}

	private static File getWorkingDirectoryFromPreferences() {
		String wdString = getDefault().getPreferenceStore()
				.getString(CorrosionPreferenceInitializer.WORKING_DIRECTORY_PREFERENCE);
		if (wdString == null) {
			return null;
		}
		File wdFile = new File(wdString);
		if (wdFile.exists() && wdFile.isDirectory()) {
			return wdFile;
		}
		return null;
	}

	public static String getOutputFromCommand(String... commandStrings) {
		try {
			Process process = getProcessForCommand(commandStrings);
			if (process.waitFor() == 0) {
				try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
					return in.readLine();
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (IOException e) {
			// Error will be caught with empty response
		}
		return ""; //$NON-NLS-1$
	}

	// activating UnitTestPlugin bundle

	public static void activateUnitTestCoreBundle() {
		Assert.isNotNull(Platform.getBundle("org.eclipse.unittest.ui")); //$NON-NLS-1$
	}

	// Cargo Source Lookup

	public static final String CONTENT_TYPE_CORROSION_RUST = "org.eclipse.corrosion.rust"; //$NON-NLS-1$
	public static final String CONTENT_TYPE_CORROSION_TOML = "org.eclipse.corrosion.toml"; //$NON-NLS-1$
	public static final String CONTENT_TYPE_CORROSION_MANIFEST = "org.eclipse.corrosion.manifest"; //$NON-NLS-1$

	/**
	 * Returns all content types that are registered with Corrosion.
	 */
	public static final String[] getRegisteredContentTypeIds() {
		Set<String> contentTypes = collectContentTypeIds();
		return contentTypes.toArray(new String[contentTypes.size()]);
	}

	private static final Set<String> collectContentTypeIds() {
		HashSet<String> allTypes = new HashSet<>();
		allTypes.add(CONTENT_TYPE_CORROSION_RUST);
		allTypes.add(CONTENT_TYPE_CORROSION_TOML);
		allTypes.add(CONTENT_TYPE_CORROSION_MANIFEST);
		return allTypes;
	}

	/**
	 * The identifier of the common source locations list
	 */
	public static final String PREF_SOURCE_LOCATIONS = PLUGIN_ID + "Cargo.Source.source_locations"; //$NON-NLS-1$

	/**
	 * String preference for the common source containers. This preference is
	 * superseded by PREF_DEFAULT_SOURCE_CONTAINERS. Kept for backward compatibility
	 * only.
	 */
	public static final String PREF_COMMON_SOURCE_CONTAINERS = PLUGIN_ID + ".Cargo.common_source_containers"; //$NON-NLS-1$

	/**
	 * String preference for the default source containers.
	 */
	public static final String PREF_DEFAULT_SOURCE_CONTAINERS = PLUGIN_ID + ".Cargo.default_source_containers"; //$NON-NLS-1$

	/**
	 * Dummy source lookup director needed to manage common source containers.
	 */
	private CargoCommonSourceLookupDirector fCommonSourceLookupDirector;

	@SuppressWarnings("deprecation")
	public void saveCommonSourceLocations(ICargoSourceLocation[] locations) {
		CorrosionPlugin.getDefault().getPluginPreferences().setValue(PREF_SOURCE_LOCATIONS,
				CargoSourceUtils.getCommonSourceLocationsMemento(locations));
	}

	@SuppressWarnings("deprecation")
	public ICargoSourceLocation[] getCommonSourceLocations() {
		return CargoSourceUtils.getCommonSourceLocationsFromMemento(
				CorrosionPlugin.getDefault().getPluginPreferences().getString(PREF_SOURCE_LOCATIONS));
	}

	@SuppressWarnings("deprecation")
	private void initializeCommonSourceLookupDirector() {
		if (fCommonSourceLookupDirector == null) {
			fCommonSourceLookupDirector = new CargoCommonSourceLookupDirector();
			boolean convertingFromLegacyFormat = false;
			String newMemento = CorrosionPlugin.getDefault().getPluginPreferences()
					.getString(PREF_DEFAULT_SOURCE_CONTAINERS);
			if (newMemento.isEmpty()) {
				newMemento = CorrosionPlugin.getDefault().getPluginPreferences()
						.getString(PREF_COMMON_SOURCE_CONTAINERS);
				convertingFromLegacyFormat = true;
			}
			if (newMemento.isEmpty()) {
				// Add the participant(s). This happens as part of
				// initializeFromMemento(), but since we're not calling it, we
				// need to do this explicitly. See 299583.
				fCommonSourceLookupDirector.initializeParticipants();

				// Convert source locations to source containers
				convertSourceLocations(fCommonSourceLookupDirector);
			} else {
				try {
					fCommonSourceLookupDirector.initializeFromMemento(newMemento);
				} catch (CoreException e) {
					logError(e.getStatus());
				}
			}
			if (convertingFromLegacyFormat) {
				// Add three source containers that used to be present implicitly.
				ISourceContainer[] oldContainers = fCommonSourceLookupDirector.getSourceContainers();
				ISourceContainer[] containers = new ISourceContainer[oldContainers.length + 2];
				int i = 0;
				containers[i++] = new CargoAbsolutePathSourceContainer();
				containers[i++] = new CargoProjectSourceContainer(null, true);
				System.arraycopy(oldContainers, 0, containers, i, oldContainers.length);
				fCommonSourceLookupDirector.setSourceContainers(containers);
			}
		}
	}

	private void disposeCommonSourceLookupDirector() {
		if (fCommonSourceLookupDirector != null)
			fCommonSourceLookupDirector.dispose();
	}

	public CargoSourceLookupDirector getCommonSourceLookupDirector() {
		return fCommonSourceLookupDirector;
	}

	private void convertSourceLocations(CargoCommonSourceLookupDirector director) {
		director.setSourceContainers(CargoSourceUtils.convertSourceLocations(getCommonSourceLocations()));
	}

	/**
	 * Get an ECF based file transfer service.
	 *
	 * @return retrieve file transfer
	 */
	public IRetrieveFileTransfer getFileTransferService() {
		IRetrieveFileTransferFactory factory = getFileTransferServiceTracker().getService();
		return factory.newInstance();
	}

	private synchronized ServiceTracker<IRetrieveFileTransferFactory, IRetrieveFileTransferFactory> getFileTransferServiceTracker() {
		if (retrievalFactoryTracker == null) {
			retrievalFactoryTracker = new ServiceTracker<>(getContext(), IRetrieveFileTransferFactory.class, null);
			retrievalFactoryTracker.open();
			startBundle("org.eclipse.ecf"); //$NON-NLS-1$
			startBundle("org.eclipse.ecf.provider.filetransfer"); //$NON-NLS-1$
		}
		return retrievalFactoryTracker;
	}

	private static boolean startBundle(String bundleId) {
		Bundle[] bundles = getContext().getBundles();
		for (Bundle bundle : bundles) {
			if (bundle.getSymbolicName().equals(bundleId)) {
				if ((bundle.getState() & Bundle.INSTALLED) == 0) {
					try {
						bundle.start(Bundle.START_ACTIVATION_POLICY);
						bundle.start(Bundle.START_TRANSIENT);
						return true;
					} catch (BundleException e) {
						return false;
					}
				}
			}
		}
		return false;
	}
}
