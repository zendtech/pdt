/*******************************************************************************
 * Copyright (c) 2014 Zend Techologies Ltd.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Zend Technologies Ltd. - initial API and implementation
 *******************************************************************************/
package org.eclipse.php.formatter.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.php.formatter.ui.preferences.CodeFormatterConfigurationBlock;
import org.eclipse.php.formatter.ui.preferences.ProfileManager;
import org.eclipse.php.formatter.ui.preferences.ProfileManager.Profile;
import org.eclipse.php.formatter.ui.preferences.ProfileStore;
import org.eclipse.ui.IStartup;

/**
 * @author Michal Niewrzal, 2014
 * 
 */
public class FormatterPreferenceInitializer implements IStartup {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IStartup#earlyStartup()
	 */
	@Override
	public void earlyStartup() {
		// workaround preferences initialization
		ProfileManager manager = new ProfileManager(getCustomProfiles(),
				InstanceScope.INSTANCE);
		manager.commitChanges(InstanceScope.INSTANCE);
	}

	/**
	 * This method reads the custom profiles from preferences. It resolves
	 * ZSTD-4451.
	 * 
	 * @TODO refactoring - the code is copy-pasted from
	 *       {@link CodeFormatterConfigurationBlock#CodeFormatterConfigurationBlock}
	 */
	@SuppressWarnings({ "unchecked" })
	private List<Profile> getCustomProfiles() {
		List<Profile> profiles = null;
		try {
			profiles = ProfileStore.readProfiles(InstanceScope.INSTANCE);
		} catch (CoreException e) {
			Logger.logException(e);
		}
		if (profiles == null) {
			try {
				profiles = ProfileStore
						.readProfilesFromPreferences(DefaultScope.INSTANCE);
			} catch (CoreException e) {
				Logger.logException(e);
			}
		}

		if (profiles == null)
			profiles = new ArrayList<Profile>();

		return profiles;
	}

}
