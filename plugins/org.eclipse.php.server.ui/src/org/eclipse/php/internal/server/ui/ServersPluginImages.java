/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Zend Technologies
 *******************************************************************************/
package org.eclipse.php.internal.server.ui;

import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;

/**
 * Bundle of most images used by the PHP Debug UI plug-in.
 */
public class ServersPluginImages {

	private static URL ICON_BASE_URL = Activator.getDefault().getBundle()
			.getEntry("/icons/full/"); //$NON-NLS-1$

	private static final String IMG_SERVER = ICON_BASE_URL + "obj16/server.gif"; //$NON-NLS-1$
	private static final String IMG_WIZ_SERVER = ICON_BASE_URL
			+ "wizban/server_wiz.gif"; //$NON-NLS-1$

	public static final ImageDescriptor DESC_SERVER = Activator.getDefault()
			.getImageDescriptor(IMG_SERVER);
	public static final ImageDescriptor DESC_WIZ_SERVER = Activator
			.getDefault().getImageDescriptor(IMG_WIZ_SERVER);

}
