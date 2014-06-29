/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Zend Technologies
 *******************************************************************************/
package org.eclipse.php.server.ui.types;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.php.internal.server.PHPServerUIMessages;
import org.eclipse.php.internal.server.ui.ServersPluginImages;
import org.eclipse.swt.graphics.Image;

/**
 * Server type implementation for default basic PHP server.
 * 
 * @author Wojciech Galanciak, 2014
 * 
 */
public class BasicServerType implements IServerType {

	public static final String ID = "org.eclipse.php.server.ui.types.BasicServerType"; //$NON-NLS-1$

	public String getName() {
		return PHPServerUIMessages.getString("BasicServerType.Name"); //$NON-NLS-1$
	}

	public String getDescription() {
		return PHPServerUIMessages.getString("BasicServerType.Description"); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.php.server.ui.types.IServerType#getViewIcon()
	 */
	public Image getViewIcon() {
		return ServersPluginImages.get(ServersPluginImages.IMG_SERVER);
	}

	public Image getTypeIcon() {
		return ServersPluginImages.get(ServersPluginImages.IMG_SERVER_TYPE);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.php.server.ui.types.IServerType#getWizardImage()
	 */
	public ImageDescriptor getWizardImage() {
		return ServersPluginImages.DESC_WIZ_SERVER;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.php.server.ui.types.IServerType#getId()
	 */
	public String getId() {
		return ID;
	}

}
