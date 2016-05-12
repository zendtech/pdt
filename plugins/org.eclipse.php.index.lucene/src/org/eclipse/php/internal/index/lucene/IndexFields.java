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
 * Constants for Lucene document fields.
 * 
 * @author Michal Niewrzal, Bartlomiej Laczkowski
 */
public final class IndexFields {

	private IndexFields() {
		// Constants only
	}

	// Common set of fields
	public static final String F_PATH = "path"; //$NON-NLS-1$
	public static final String F_ELEMENT_NAME_LC = "elementNameLC"; //$NON-NLS-1$
	public static final String F_CC_NAME = "ccName"; //$NON-NLS-1$
	public static final String F_QUALIFIER = "qualifier"; //$NON-NLS-1$
	public static final String F_PARENT = "parent"; //$NON-NLS-1$
	// Numeric doc values
	public static final String NDV_TIMESTAMP = "timestampNDV"; //$NON-NLS-1$
	public static final String NDV_OFFSET = "offsetNDV"; //$NON-NLS-1$
	public static final String NDV_LENGTH = "lengthNDV"; //$NON-NLS-1$
	public static final String NDV_FLAGS = "flagsNDV"; //$NON-NLS-1$
	public static final String NDV_NAME_OFFSET = "nameOffsetNDV"; //$NON-NLS-1$
	public static final String NDV_NAME_LENGTH = "nameLengthNDV"; //$NON-NLS-1$
	// Binary doc values
	public static final String BDV_PATH = "pathBDV"; //$NON-NLS-1$
	public static final String BDV_ELEMENT_NAME = "elementNameBDV"; //$NON-NLS-1$
	public static final String BDV_QUALIFIER = "qualifierBDV"; //$NON-NLS-1$
	public static final String BDV_PARENT = "parentBDV"; //$NON-NLS-1$
	public static final String BDV_METADATA = "metadataBDV"; //$NON-NLS-1$
	public static final String BDV_DOC = "docBDV"; //$NON-NLS-1$

}
