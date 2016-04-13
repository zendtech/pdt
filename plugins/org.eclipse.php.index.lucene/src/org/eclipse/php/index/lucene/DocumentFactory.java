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

import static org.eclipse.php.index.lucene.IndexFields.*;

import org.apache.lucene.document.BinaryDocValuesField;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.util.BytesRef;
import org.eclipse.dltk.core.index2.IIndexingRequestor.DeclarationInfo;
import org.eclipse.dltk.core.index2.IIndexingRequestor.ReferenceInfo;

/**
 * <p>
 * Factory for creating different types of Lucene documents.
 * </p>
 * <p>
 * To boost the performance of documents search and related data retrieval,
 * numeric and binary document values are being used in pair with non-stored
 * fields. It basically means that non-stored fields are used for document
 * search purposes while numeric and binary document values are used to retrieve
 * the related data for particular search matches.
 * </p>
 * 
 * @author Bartlomiej Laczkowski
 */
public enum DocumentFactory {

	INSTANCE;

	/**
	 * Creates and returns a document for provided reference info.
	 * 
	 * @param source
	 * @param info
	 * @return a document for provided reference info
	 */
	public Document createForReference(String source, ReferenceInfo info) {
		Document doc = new Document();
		// Fields for search (no store, doc values will be used instead)
		addTextEntry(doc, F_PATH, source, false);
		addTextEntry(doc, F_QUALIFIER, info.qualifier, false);
		addTextLCEntry(doc, F_ELEMENT_NAME_LC, info.elementName, false);
		// Add numeric doc values
		addLongEntry(doc, NDV_OFFSET, info.offset);
		addLongEntry(doc, NDV_LENGTH, info.length);
		// Add text as binary doc values
		addBinaryEntry(doc, BDV_PATH, source);
		addBinaryEntry(doc, BDV_ELEMENT_NAME, info.elementName);
		addBinaryEntry(doc, BDV_QUALIFIER, info.qualifier);
		addBinaryEntry(doc, BDV_METADATA, info.metadata);
		return doc;
	}

	/**
	 * Creates and returns a document for provided declaration info.
	 * 
	 * @param source
	 * @param info
	 * @return a document for provided declaration info
	 */
	public Document createForDeclaration(String source, DeclarationInfo info) {
		Document doc = new Document();
		// Fields for search (no store, doc values will be used instead)
		addTextEntry(doc, F_PATH, source, false);
		addTextEntry(doc, F_PARENT, info.parent, false);
		addTextEntry(doc, F_QUALIFIER, info.qualifier, false);
		addTextLCEntry(doc, F_ELEMENT_NAME_LC, info.elementName, false);
		addCCNameEntry(doc, info.elementName);
		// Add numeric doc values
		addLongEntry(doc, NDV_OFFSET, info.offset);
		addLongEntry(doc, NDV_LENGTH, info.length);
		addLongEntry(doc, NDV_NAME_OFFSET, info.nameOffset);
		addLongEntry(doc, NDV_NAME_LENGTH, info.nameLength);
		addLongEntry(doc, NDV_FLAGS, info.flags);
		// Add text as binary doc values
		addBinaryEntry(doc, BDV_PATH, source);
		addBinaryEntry(doc, BDV_ELEMENT_NAME, info.elementName);
		addBinaryEntry(doc, BDV_PARENT, info.parent);
		addBinaryEntry(doc, BDV_QUALIFIER, info.qualifier);
		addBinaryEntry(doc, BDV_METADATA, info.metadata);
		addBinaryEntry(doc, BDV_DOC, info.doc);
		return doc;
	}

	/**
	 * Creates and returns a document for source file time stamp.
	 * 
	 * @param source
	 * @param timestamp
	 * @return a document for source file time stamp
	 */
	public Document createForTimestamp(String source, long timestamp) {
		Document doc = new Document();
		addTextEntry(doc, F_PATH, source, true);
		addLongEntry(doc, NDV_TIMESTAMP, timestamp);
		return doc;
	}

	private void addLongEntry(Document doc, String category, long value) {
		doc.add(new NumericDocValuesField(category, value));
	}

	private void addTextEntry(Document doc, String category, String value, boolean store) {
		if (value == null) {
			return;
		}
		doc.add(new TextField(category, value, store ? Field.Store.YES : Field.Store.NO));
	}

	private void addTextLCEntry(Document doc, String category, String value, boolean store) {
		addTextEntry(doc, category, value.toLowerCase(), store);
	}

	private void addCCNameEntry(Document doc, String name) {
		String camelCaseName = null;
		StringBuilder camelCaseNameBuf = new StringBuilder();
		for (int i = 0; i < name.length(); ++i) {
			char ch = name.charAt(i);
			if (Character.isUpperCase(ch)) {
				camelCaseNameBuf.append(ch);
			} else if (i == 0) {
				// Not applicable for camel case search
				break;
			}
		}
		camelCaseName = camelCaseNameBuf.length() > 0 ? camelCaseNameBuf.toString() : null;
		addTextEntry(doc, F_CC_NAME, camelCaseName, false);
	}

	private void addBinaryEntry(Document doc, String category, String value) {
		if (value == null) {
			return;
		}
		doc.add(new BinaryDocValuesField(category, new BytesRef(value)));
	}

}
