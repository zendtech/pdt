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

import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.php.formatter.ui.preferences.ProfileManager;
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
		// workaround preferences initalization
		IScopeContext instanceScope = InstanceScope.INSTANCE;
		ProfileManager manager = new ProfileManager(
				new ArrayList<ProfileManager.Profile>(), instanceScope);
		manager.commitChanges(instanceScope);
	}

}
