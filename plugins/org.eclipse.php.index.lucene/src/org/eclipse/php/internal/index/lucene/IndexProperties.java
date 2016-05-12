/*******************************************************************************
 * Copyright (c) 2016 Zend Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Zend Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.php.internal.index.lucene;

import org.apache.lucene.util.Version;
import org.eclipse.php.index.lucene.LucenePlugin;

/**
 * Lucene support properties, i.e. model & Lucene engine version.
 * 
 * @author Bartlomiej Laczkowski
 */
public final class IndexProperties {

	private static final String PREFIX = LucenePlugin.ID + ".property."; //$NON-NLS-1$

	public static final String KEY_MODEL_VERSION = PREFIX + "model.version"; //$NON-NLS-1$
	public static final String KEY_LUCENE_VERSION = PREFIX + "lucene.version"; //$NON-NLS-1$

	public static final String MODEL_VERSION = "2.0"; //$NON-NLS-1$
	public static final String LUCENE_VERSION = Version.LATEST.toString();

}