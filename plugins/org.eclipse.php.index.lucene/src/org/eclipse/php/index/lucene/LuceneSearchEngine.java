package org.eclipse.php.index.lucene;

import static org.eclipse.php.index.lucene.IndexFields.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.BooleanFilter;
import org.apache.lucene.queries.TermFilter;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocValuesDocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.PrefixFilter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.WildcardQuery;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.dltk.core.ScriptModelUtil;
import org.eclipse.php.index.lucene.LuceneManager.ContainerIndexType;
import org.eclipse.dltk.core.index2.search.ISearchEngine;
import org.eclipse.dltk.core.index2.search.ISearchEngineExtension;
import org.eclipse.dltk.core.index2.search.ISearchRequestor;
import org.eclipse.dltk.core.search.IDLTKSearchScope;
import org.eclipse.dltk.internal.core.search.DLTKSearchScope;

@SuppressWarnings("restriction")
public class LuceneSearchEngine implements ISearchEngine, ISearchEngineExtension {

	private static final class SearchScope {

		static List<String> getContainers(IDLTKSearchScope scope) {
			List<String> containers = new ArrayList<String>();
			for (IPath path : scope.enclosingProjectsAndZips()) {
				containers.add(path.toString());
			}
			return containers;
		}

		static List<String> getScripts(IDLTKSearchScope scope) {
			List<String> scripts = new ArrayList<String>();
			if (scope instanceof DLTKSearchScope) {
				String[] relativePaths = ((DLTKSearchScope) scope).getRelativePaths();
				String[] fileExtensions = ScriptModelUtil.getFileExtensions(scope.getLanguageToolkit());
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

		private static final String[] NUMERIC_FIELDS = new String[] { NDV_OFFSET, NDV_LENGTH, NDV_FLAGS,
				NDV_NAME_OFFSET, NDV_NAME_LENGTH };
		private static final String[] BINARY_FIELDS = new String[] { BDV_PATH, BDV_ELEMENT_NAME, BDV_QUALIFIER,
				BDV_PARENT, BDV_METADATA, BDV_DOC };
		private Map<String, NumericDocValues> fDocNumericValues;
		private Map<String, BinaryDocValues> fDocBinaryValues;
		private String fContainer;
		private int fElementType;
		private List<SearchMatch> fResult;

		public ResultsCollector(String container, int elementType, List<SearchMatch> result) {
			this.fContainer = container;
			this.fElementType = elementType;
			this.fResult = result;
		}

		public LeafCollector getLeafCollector(final LeafReaderContext context) throws IOException {
			final LeafReader reader = context.reader();
			fDocNumericValues = new HashMap<String, NumericDocValues>();
			for (String field : NUMERIC_FIELDS) {
				NumericDocValues docValues = reader.getNumericDocValues(field);
				if (docValues != null) {
					fDocNumericValues.put(field, docValues);
				}
			}
			fDocBinaryValues = new HashMap<String, BinaryDocValues>();
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

		private void addResult(int docId) throws IOException {
			fResult.add(new SearchMatch(fContainer, fElementType, getNumericValue(NDV_OFFSET, docId),
					getNumericValue(NDV_LENGTH, docId), getNumericValue(NDV_NAME_OFFSET, docId),
					getNumericValue(NDV_NAME_LENGTH, docId), getNumericValue(NDV_FLAGS, docId),
					getStringValue(BDV_ELEMENT_NAME, docId), getStringValue(BDV_PATH, docId),
					getStringValue(BDV_PARENT, docId), getStringValue(BDV_QUALIFIER, docId),
					getStringValue(BDV_DOC, docId), getStringValue(BDV_METADATA, docId)));
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

	private static final class BitFlagsFilter extends Filter {

		private String fField;
		private int fTrueFlags;
		private int fFalseFlags;

		public BitFlagsFilter(String field, int trueFlags, int falseFlags) {
			fField = field;
			fTrueFlags = trueFlags;
			fFalseFlags = falseFlags;
		}

		@Override
		public DocIdSet getDocIdSet(LeafReaderContext context, Bits acceptDocs) throws IOException {
			final NumericDocValues values = DocValues.getNumeric(context.reader(), fField);
			return new DocValuesDocIdSet(context.reader().maxDoc(), acceptDocs) {
				@Override
				protected boolean matchDoc(int doc) {
					long flags = values.get(doc);
					if (fTrueFlags != 0) {
						if ((fTrueFlags & flags) == 0) {
							return false;
						}
					}
					if (fFalseFlags != 0) {
						if ((fFalseFlags & flags) != 0) {
							return false;
						}
					}
					return true;
				}
			};
		}

		@Override
		public int hashCode() {
			return fField.hashCode() * fTrueFlags * fFalseFlags;
		}

	}

	@Override
	public void search(int elementType, String qualifier, String elementName, int trueFlags, int falseFlags, int limit,
			SearchFor searchFor, MatchRule matchRule, IDLTKSearchScope scope, ISearchRequestor requestor,
			IProgressMonitor monitor) {
		search(elementType, qualifier, elementName, null, trueFlags, falseFlags, limit, searchFor, matchRule, scope,
				requestor, monitor);
	}

	@Override
	public void search(int elementType, String qualifier, String elementName, String parent, int trueFlags,
			int falseFlags, int limit, SearchFor searchFor, MatchRule matchRule, IDLTKSearchScope scope,
			ISearchRequestor requestor, IProgressMonitor monitor) {
		boolean searchForDecls = searchFor == SearchFor.DECLARATIONS || searchFor == SearchFor.ALL_OCCURRENCES;
		boolean searchForRefs = searchFor == SearchFor.REFERENCES || searchFor == SearchFor.ALL_OCCURRENCES;
		if (searchForRefs) {
			doSearch(elementType, qualifier, elementName, parent, trueFlags, falseFlags, limit, true, matchRule, scope,
					requestor, monitor);
		}
		if (searchForDecls) {
			doSearch(elementType, qualifier, elementName, parent, trueFlags, falseFlags, limit, false, matchRule, scope,
					requestor, monitor);
		}
	}

	private Filter createFilter(final int elementType, String qualifier, String elementName, String parent,
			final int trueFlags, final int falseFlags, final boolean searchForRefs, MatchRule matchRule,
			IDLTKSearchScope scope) {
		BooleanFilter filter = new BooleanFilter();
		if (elementName != null && !elementName.isEmpty()) {
			String elementNameLC = elementName.toLowerCase();
			Filter nameFilter = null;
			Term nameCaseInsensitiveTerm = new Term(F_ELEMENT_NAME_LC, elementNameLC);
			if (matchRule == MatchRule.PREFIX) {
				nameFilter = new PrefixFilter(nameCaseInsensitiveTerm);
			} else if (matchRule == MatchRule.EXACT) {
				nameFilter = new TermFilter(nameCaseInsensitiveTerm);
			} else if (matchRule == MatchRule.CAMEL_CASE) {
				Term term = new Term(F_CC_NAME, elementName);
				nameFilter = new PrefixFilter(term);
			} else if (matchRule == MatchRule.PATTERN) {
				nameFilter = new QueryWrapperFilter(new WildcardQuery(nameCaseInsensitiveTerm));
			} else {
				throw new UnsupportedOperationException();
			}
			if (nameFilter != null) {
				filter.add(nameFilter, Occur.MUST);
			}
		}
		if (qualifier != null && !qualifier.isEmpty()) {
			filter.add(new TermFilter(new Term(F_QUALIFIER, qualifier)), Occur.MUST);
		}
		if (parent != null && !parent.isEmpty()) {
			filter.add(new TermFilter(new Term(F_PARENT, parent)), Occur.MUST);
		}
		if (trueFlags != 0 || falseFlags != 0) {
			filter.add(new BitFlagsFilter(NDV_FLAGS, trueFlags, falseFlags), Occur.MUST);
		}
		List<String> scripts = SearchScope.getScripts(scope);
		if (!scripts.isEmpty()) {
			BooleanFilter scriptFilter = new BooleanFilter();
			for (String script : scripts) {
				scriptFilter.add(new TermFilter(new Term(F_PATH, script)), Occur.MUST);
			}
			filter.add(scriptFilter, Occur.MUST);
		}
		return filter.clauses().isEmpty() ? null : filter;
	}

	private void doSearch(final int elementType, String qualifier, String elementName, String parent,
			final int trueFlags, final int falseFlags, int limit, final boolean searchForRefs, MatchRule matchRule,
			IDLTKSearchScope scope, ISearchRequestor requestor, IProgressMonitor monitor) {
		Filter filter = createFilter(elementType, qualifier, elementName, parent, trueFlags, falseFlags, searchForRefs,
				matchRule, scope);
		IndexSearcher indexSearcher = null;
		Query query = new MatchAllDocsQuery();
		final SearchMatchHandler searchMatchHandler = new SearchMatchHandler(scope, requestor);
		List<SearchMatch> results = new ArrayList<SearchMatch>();
		for (String container : SearchScope.getContainers(scope)) {
			// Use index searcher for given container, data type and element type
			SearcherManager searcherManager = LuceneManager.INSTANCE.findIndexSearcher(container,
					searchForRefs ? ContainerIndexType.REFERENCES : ContainerIndexType.DECLARATIONS, elementType);
			try {
				indexSearcher = searcherManager.acquire();
				ResultsCollector collector = new ResultsCollector(container, elementType, results);
				if (filter != null) {
					indexSearcher.search(query, filter, collector);
				} else {
					indexSearcher.search(query, collector);
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
		if (results.size() > 1) {
			// Sort final results by element name
			Collections.sort(results, new Comparator<SearchMatch>() {
				@Override
				public int compare(SearchMatch e1, SearchMatch e2) {
					return e1.getElementName().compareToIgnoreCase(e2.getElementName());
				}
			});
		}
		// Pass results to entity handler
		for (SearchMatch result : results) {
			searchMatchHandler.handle(result, searchForRefs);
		}
	}

}
