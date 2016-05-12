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

/**
 * Index type (declarations or references).
 */
public enum IndexType {

	/**
	 * Index type for storing declarations data.
	 */
	DECLARATIONS("declarations"), //$NON-NLS-1$
	/**
	 * Index type for storing references data.
	 */
	REFERENCES("references"); //$NON-NLS-1$

	private final String fDirectory;

	private IndexType(String directory) {
		this.fDirectory = directory;
	}

	/**
	 * Returns related directory name.
	 * 
	 * @return related directory name
	 */
	public String getDirectory() {
		return fDirectory;
	}

}