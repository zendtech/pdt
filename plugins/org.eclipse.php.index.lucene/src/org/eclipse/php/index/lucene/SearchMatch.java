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
package org.eclipse.php.index.lucene;

/**
 * Search match descriptor.
 * 
 * @author Bartlomiej Laczkowski
 */
public class SearchMatch {

	private String fPath;
	private String fContainer;
	private int fElementType;
	private String fElementName;
	private int fOffset;
	private int fLength;
	private String fMetadata;
	private String fQualifier;
	private String fDoc;
	private int fFlags;
	private int fNameOffset;
	private int fNameLength;
	private String fParent;

	/**
	 * Creates new search match.
	 * 
	 * @param container
	 * @param elementType
	 * @param offset
	 * @param length
	 * @param nameOffset
	 * @param nameLength
	 * @param flags
	 * @param elementName
	 * @param path
	 * @param parent
	 * @param qualifier
	 * @param doc
	 * @param metadata
	 */
	public SearchMatch(String container, int elementType, int offset, int length, int nameOffset, int nameLength,
			int flags, String elementName, String path, String parent, String qualifier, String doc, String metadata) {
		super();
		fContainer = container;
		fElementType = elementType;
		fOffset = offset;
		fLength = length;
		fNameOffset = nameOffset;
		fNameLength = nameLength;
		fFlags = flags;
		fElementName = elementName;
		fPath = path;
		fParent = parent;
		fQualifier = qualifier;
		fDoc = doc;
		fMetadata = metadata;
	}

	/**
	 * Returns element path
	 * 
	 * @return element path
	 */
	public String getPath() {
		return fPath;
	}

	/**
	 * Returns element container
	 * 
	 * @return element container
	 */
	public String getContainer() {
		return fContainer;
	}

	/**
	 * Returns element type
	 * 
	 * @return element type
	 */
	public int getElementType() {
		return fElementType;
	}

	/**
	 * Returns element name
	 * 
	 * @return element name
	 */
	public String getElementName() {
		return fElementName;
	}

	/**
	 * Returns element offset
	 * 
	 * @return element offset
	 */
	public int getOffset() {
		return fOffset;
	}

	/**
	 * Returns element length
	 * 
	 * @return element length
	 */
	public int getLength() {
		return fLength;
	}

	/**
	 * Returns element meta-data
	 * 
	 * @return element meta-data
	 */
	public String getMetadata() {
		return fMetadata;
	}

	/**
	 * Returns element qualifier
	 * 
	 * @return element qualifier
	 */
	public String getQualifier() {
		return fQualifier;
	}

	/**
	 * Returns element doc
	 * 
	 * @return element doc
	 */
	public String getDoc() {
		return fDoc;
	}

	/**
	 * Returns element flags
	 * 
	 * @return element flags
	 */
	public int getFlags() {
		return fFlags;
	}

	/**
	 * Returns element name offset
	 * 
	 * @return element name offset
	 */
	public int getNameOffset() {
		return fNameOffset;
	}

	/**
	 * Returns element name length
	 * 
	 * @return element name length
	 */
	public int getNameLength() {
		return fNameLength;
	}

	/**
	 * Returns element parent
	 * 
	 * @return element parent
	 */
	public String getParent() {
		return fParent;
	}

}
