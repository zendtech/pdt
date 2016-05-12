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

import static org.eclipse.php.internal.index.lucene.IndexFields.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.util.BytesRef;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.dltk.core.ScriptModelUtil;
import org.eclipse.dltk.core.index2.search.ISearchEngineExtension;
import org.eclipse.dltk.core.index2.search.ISearchRequestor;
import org.eclipse.dltk.core.search.IDLTKSearchScope;
import org.eclipse.dltk.internal.core.search.DLTKSearchScope;

/**
 * Lucene based implementation for DLTK search engine.
 * 
 * @author Michal Niewrzal, Bartlomiej Laczkowski
 */
@SuppressWarnings("restriction")
public class LuceneSearchEngine implements ISearchEngineExtension {

	private static final class SearchScope {

		static List<String> getContainers(IDLTKSearchScope scope) {
			List<String> containers = new ArrayList<>();
			for (IPath path : scope.enclosingProjectsAndZips()) {
				containers.add(path.toString());
			}
			return containers;
		}

		static List<String> getScripts(IDLTKSearchScope scope) {
			List<String> scripts = new ArrayList<>();
			if (scope instanceof DLTKSearchScope) {
				String[] relativePaths = ((DLTKSearchScope) scope)
						.getRelativePaths();
				String[] fileExtensions = ScriptModelUtil
						.getFileExtensions(scope.getLanguageToolkit());
				for (String relativePath : relativePaths) {
					if (relativePath.length() > 0) {
						if (fileExtensions != null) {
							boolean isScriptFile = false;
							for (String ext : fileExtensions) {
								if (relativePath.endsWith("." + ext)) { //$NON-NLS-1$
									isScriptFile = true;
									break;
								}
							}
							if (!isScriptFile) {
								break;
							}
						}
						scripts.add(relativePath);
					}
				}
			}
			return scripts;
		}

	}

	private static final class ResultsCollector implements Collector {

		private static final String[] NUMERIC_FIELDS = new String[] {
				NDV_OFFSET, NDV_LENGTH, NDV_FLAGS, NDV_NAME_OFFSET,
				NDV_NAME_LENGTH };
		private static final String[] BINARY_FIELDS = new String[] { BDV_PATH,
				BDV_ELEMENT_NAME, BDV_QUALIFIER, BDV_PARENT, BDV_METADATA,
				BDV_DOC };
		private Map<String, NumericDocValues> fDocNumericValues;
		private Map<String, BinaryDocValues> fDocBinaryValues;
		private String fContainer;
		private int fElementType;
		private List<SearchMatch> fResult;

		public ResultsCollector(String container, int elementType,
				List<SearchMatch> result) {
			this.fContainer = container;
			this.fElementType = elementType;
			this.fResult = result;
		}

		@Override
		public boolean needsScores() {
			return true;
		}

		@Override
		public LeafCollector getLeafCollector(final LeafReaderContext context)
				throws IOException {
			final LeafReader reader = context.reader();
			fDocNumericValues = new HashMap<>();
			for (String field : NUMERIC_FIELDS) {
				NumericDocValues docValues = reader.getNumericDocValues(field);
				if (docValues != null) {
					fDocNumericValues.put(field, docValues);
				}
			}
			fDocBinaryValues = new HashMap<>();
			for (String field : BINARY_FIELDS) {
				BinaryDocValues docValues = reader.getBinaryDocValues(field);
				if (docValues != null) {
					fDocBinaryValues.put(field, docValues);
				}
			}
			return new LeafCollector() {
				@Override
				public void setScorer(Scorer scorer) throws IOException {
					// ignore
				}

				@Override
				public void collect(int docId) throws IOException {
					addResult(docId);
				}
			};
		}

		private void addResult(int docId) {
			fResult.add(new SearchMatch(fContainer, fElementType,
					getNumericValue(NDV_OFFSET, docId),
					getNumericValue(NDV_LENGTH, docId),
					getNumericValue(NDV_NAME_OFFSET, docId),
					getNumericValue(NDV_NAME_LENGTH, docId),
					getNumericValue(NDV_FLAGS, docId),
					getStringValue(BDV_ELEMENT_NAME, docId),
					getStringValue(BDV_PATH, docId),
					getStringValue(BDV_PARENT, docId),
					getStringValue(BDV_QUALIFIER, docId),
					getStringValue(BDV_DOC, docId),
					getStringValue(BDV_METADATA, docId)));
		}

		private int getNumericValue(String field, int docId) {
			NumericDocValues docValues = fDocNumericValues.get(field);
			if (docValues != null) {
				return (int) docValues.get(docId);
			}
			return 0;
		}

		private String getStringValue(String field, int docId) {
			BinaryDocValues docValues = fDocBinaryValues.get(field);
			if (docValues != null) {
				BytesRef bytesRef = docValues.get(docId);
				if (bytesRef.length > 0)
					return bytesRef.utf8ToString();
			}
			return null;
		}
	}

	@Override
	public void search(int elementType, String qualifier, String elementName,
			int trueFlags, int falseFlags, int limit, SearchFor searchFor,
			MatchRule matchRule, IDLTKSearchScope scope,
			ISearchRequestor requestor, IProgressMonitor monitor) {
		search(elementType, qualifier, elementName, null, trueFlags, falseFlags,
				limit, searchFor, matchRule, scope, requestor, monitor);
	}

	@Override
	public void search(int elementType, String qualifier, String elementName,
			String parent, int trueFlags, int falseFlags, int limit,
			SearchFor searchFor, MatchRule matchRule, IDLTKSearchScope scope,
			ISearchRequestor requestor, IProgressMonitor monitor) {
		boolean searchForDecls = searchFor == SearchFor.DECLARATIONS
				|| searchFor == SearchFor.ALL_OCCURRENCES;
		boolean searchForRefs = searchFor == SearchFor.REFERENCES
				|| searchFor == SearchFor.ALL_OCCURRENCES;
		if (searchForRefs) {
			doSearch(elementType, qualifier, elementName, parent, trueFlags,
					falseFlags, limit, true, matchRule, scope, requestor,
					monitor);
		}
		if (searchForDecls) {
			doSearch(elementType, qualifier, elementName, parent, trueFlags,
					falseFlags, limit, false, matchRule, scope, requestor,
					monitor);
		}
	}

	private Query createQuery(final String elementName, final String qualifier,
			final String parent, final int trueFlags, final int falseFlags,
			final boolean searchForRefs, MatchRule matchRule,
			IDLTKSearchScope scope) {
		BooleanQuery query = new BooleanQuery();
		List<String> scripts = SearchScope.getScripts(scope);
		if (!scripts.isEmpty()) {
			BooleanQuery scriptQuery = new BooleanQuery();
			for (String script : scripts) {
				scriptQuery.add(new TermQuery(new Term(F_PATH, script)),
						Occur.FILTER);
			}
			query.add(scriptQuery, Occur.FILTER);
		}
		if (elementName != null && !elementName.isEmpty()) {
			String elementNameLC = elementName.toLowerCase();
			Query nameQuery = null;
			Term nameCaseInsensitiveTerm = new Term(F_ELEMENT_NAME_LC,
					elementNameLC);
			if (matchRule == MatchRule.PREFIX) {
				nameQuery = new PrefixQuery(nameCaseInsensitiveTerm);
			} else if (matchRule == MatchRule.EXACT) {
				nameQuery = new TermQuery(nameCaseInsensitiveTerm);
			} else if (matchRule == MatchRule.CAMEL_CASE) {
				nameQuery = new PrefixQuery(new Term(F_CC_NAME, elementName));
			} else if (matchRule == MatchRule.PATTERN) {
				nameQuery = new WildcardQuery(nameCaseInsensitiveTerm);
			} else {
				throw new UnsupportedOperationException();
			}
			if (nameQuery != null) {
				query.add(nameQuery, Occur.FILTER);
			}
		}
		if (qualifier != null && !qualifier.isEmpty()) {
			query.add(new TermQuery(new Term(F_QUALIFIER, qualifier)),
					Occur.FILTER);
		}
		if (parent != null && !parent.isEmpty()) {
			query.add(new TermQuery(new Term(F_PARENT, parent)), Occur.FILTER);
		}
		if (trueFlags != 0 || falseFlags != 0) {
			query.add(new BitFlagsQuery(trueFlags, falseFlags), Occur.FILTER);
		}
		return query.clauses().isEmpty() ? null : query;
	}

	private void doSearch(final int elementType, String qualifier,
			String elementName, String parent, final int trueFlags,
			final int falseFlags, int limit, final boolean searchForRefs,
			MatchRule matchRule, IDLTKSearchScope scope,
			ISearchRequestor requestor, IProgressMonitor monitor) {
		Query query = createQuery(elementName, qualifier, parent, trueFlags,
				falseFlags, searchForRefs, matchRule, scope);
		IndexSearcher indexSearcher = null;
		final SearchMatchHandler searchMatchHandler = new SearchMatchHandler(
				scope, requestor);
		List<SearchMatch> results = new ArrayList<>();
		for (String container : SearchScope.getContainers(scope)) {
			SearcherManager searcherManager = LuceneManager.INSTANCE
					.findIndexSearcher(container, searchForRefs
							? IndexType.REFERENCES : IndexType.DECLARATIONS,
							elementType);
			try {
				indexSearcher = searcherManager.acquire();
				ResultsCollector collector = new ResultsCollector(container,
						elementType, results);
				if (query != null) {
					indexSearcher.search(query, collector);
				} else {
					indexSearcher.search(new MatchAllDocsQuery(), collector);
				}
			} catch (IOException e) {
				Logger.logException(e);
			} finally {
				if (indexSearcher != null) {
					try {
						searcherManager.release(indexSearcher);
					} catch (IOException e) {
						Logger.logException(e);
					}
				}
			}
		}
		// Pass results to entity handler
		for (SearchMatch result : results) {
			searchMatchHandler.handle(result, searchForRefs);
		}
	}

}
