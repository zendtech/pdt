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
package org.eclipse.php.internal.server;

/**
 * @author Wojciech Galanciak, 2014
 * 
 */
public interface IHelpContextIds {

	String PREFIX = "http://files.zend.com/help/Zend-Studio/zend-studio.htm#"; //$NON-NLS-1$
	String SUFFIX = ".htm?zs"; //$NON-NLS-1$

	String ADDING_A_SERVER_GENERIC_PHP_SERVER = PREFIX
			+ "adding_a_php_server" + SUFFIX; //$NON-NLS-1$

	String EDIT_PHP_SERVER = PREFIX + "editing_php_servers" + SUFFIX; //$NON-NLS-1$

	String ADDING_PHP_SERVERS = PREFIX + "adding_php_servers" + SUFFIX; //$NON-NLS-1$

}
