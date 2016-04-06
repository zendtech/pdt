package org.eclipse.php.index.lucene;

import org.apache.lucene.document.BinaryDocValuesField;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.util.BytesRef;
import org.eclipse.dltk.core.index2.IIndexingRequestor.DeclarationInfo;
import org.eclipse.dltk.core.index2.IIndexingRequestor.ReferenceInfo;

import static org.eclipse.php.index.lucene.IndexFields.*;

public enum DocumentFactory {

	INSTANCE;

	public Document createForReference(String source, ReferenceInfo info) {
		Document doc = new Document();
		// Add text fields for search (no store, doc values will be used instead)
		addTextEntry(doc, F_PATH, source, false);
		addTextEntry(doc, F_QUALIFIER, info.qualifier, false);
		addTextLCEntry(doc, F_ELEMENT_NAME_LC, info.elementName, false);
		// Add numeric doc values
		addLongEntry(doc, NDV_OFFSET, info.offset);
		addLongEntry(doc, NDV_LENGTH, info.length);
		// Add text as binary doc values (will be used to retrieve data after search)
		addBinaryEntry(doc, BDV_PATH, source);
		addBinaryEntry(doc, BDV_ELEMENT_NAME, info.elementName);
		addBinaryEntry(doc, BDV_QUALIFIER, info.qualifier);
		addBinaryEntry(doc, BDV_METADATA, info.metadata);
		return doc;
	}

	public Document createForDeclaration(String source, DeclarationInfo info) {
		Document doc = new Document();
		// Add text fields for search (no store, doc values will be used instead)
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
		// Add text as binary doc values (will be used to retrieve data after search)
		addBinaryEntry(doc, BDV_PATH, source);
		addBinaryEntry(doc, BDV_ELEMENT_NAME, info.elementName);
		addBinaryEntry(doc, BDV_PARENT, info.parent);
		addBinaryEntry(doc, BDV_QUALIFIER, info.qualifier);
		addBinaryEntry(doc, BDV_METADATA, info.metadata);
		addBinaryEntry(doc, BDV_DOC, info.doc);
		return doc;
	}

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
		if (value == null) {
			return;
		}
		doc.add(new TextField(category, value.toLowerCase(), store ? Field.Store.YES : Field.Store.NO));
	}
	
	private void addCCNameEntry(Document doc, String name) {
		String camelCaseName = null;
		StringBuilder camelCaseNameBuf = new StringBuilder();
		for (int i = 0; i < name.length(); ++i) {
			char ch = name.charAt(i);
			if (Character.isUpperCase(ch)) {
				camelCaseNameBuf.append(ch);
			} else if (i == 0) {
				// not applicable for camel case search
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
