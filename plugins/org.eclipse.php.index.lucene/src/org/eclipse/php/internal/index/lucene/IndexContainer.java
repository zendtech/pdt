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

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.store.Directory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.php.index.lucene.LucenePlugin;

/**
 * Class responsible for handling container index data.
 * 
 * @author Bartlomiej Laczkowski
 */
class IndexContainer {

	private final class IndexCleaner extends Job {

		public IndexCleaner() {
			super(""); //$NON-NLS-1$
			setUser(false);
			setSystem(true);
		}

		@Override
		public boolean belongsTo(Object family) {
			return family == LucenePlugin.LUCENE_JOB_FAMILY;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			close();
			Path containerPath = Paths.get(fIndexRoot, getId());
			try {
				Files.walkFileTree(containerPath,
						new SimpleFileVisitor<Path>() {
							@Override
							public FileVisitResult visitFile(Path file,
									BasicFileAttributes attrs)
									throws IOException {
								Files.delete(file);
								return FileVisitResult.CONTINUE;
							}

							@Override
							public FileVisitResult postVisitDirectory(Path dir,
									IOException exc) throws IOException {
								Files.delete(dir);
								return FileVisitResult.CONTINUE;
							}
						});
			} catch (IOException e) {
				Logger.logException(e);
			}
			return Status.OK_STATUS;
		}

	}

	private static final String TIMESTAMPS_DIR = "timestamps"; //$NON-NLS-1$

	private final String fIndexRoot;
	private final String fContainerId;
	private IndexWriter fTimestampsWriter;
	private SearcherManager fTimestampsSearcher;
	private Map<IndexType, Map<Integer, IndexWriter>> fIndexWriters;
	private Map<IndexType, Map<Integer, SearcherManager>> fIndexSearchers;

	public IndexContainer(String indexRoot, String containerId) {
		fIndexRoot = indexRoot;
		fContainerId = containerId;
		initialize();
	}

	private void initialize() {
		fIndexWriters = new HashMap<>();
		fIndexWriters.put(IndexType.DECLARATIONS,
				new HashMap<Integer, IndexWriter>());
		fIndexWriters.put(IndexType.REFERENCES,
				new HashMap<Integer, IndexWriter>());
		fIndexSearchers = new HashMap<>();
		fIndexSearchers.put(IndexType.DECLARATIONS,
				new HashMap<Integer, SearcherManager>());
		fIndexSearchers.put(IndexType.REFERENCES,
				new HashMap<Integer, SearcherManager>());
	}

	public final String getId() {
		return fContainerId;
	}

	public synchronized IndexWriter getTimestampsWriter() {
		if (fTimestampsWriter == null) {
			try {
				Directory indexDir = new IndexDirectory(
						Paths.get(fIndexRoot, fContainerId, TIMESTAMPS_DIR));
				IndexWriterConfig config = new IndexWriterConfig(
						new SimpleAnalyzer());
				config.setOpenMode(OpenMode.CREATE_OR_APPEND);
				fTimestampsWriter = new IndexWriter(indexDir, config);
			} catch (IOException e) {
				Logger.logException(e);
			}
		}
		return fTimestampsWriter;
	}

	public synchronized SearcherManager getTimestampsSearcher() {
		try {
			if (fTimestampsSearcher == null) {
				fTimestampsSearcher = new SearcherManager(getTimestampsWriter(),
						true, new SearcherFactory());
			}
			// Try to achieve the up-to-date index state
			fTimestampsSearcher.maybeRefresh();
		} catch (IOException e) {
			Logger.logException(e);
		}
		return fTimestampsSearcher;
	}

	public synchronized IndexWriter getIndexWriter(IndexType dataType,
			int elementType) {
		IndexWriter writer = fIndexWriters.get(dataType).get(elementType);
		if (writer == null) {
			try {
				Directory indexDir = new IndexDirectory(Paths.get(fIndexRoot,
						fContainerId, dataType.getDirectory(),
						String.valueOf(elementType)));
				IndexWriterConfig config = new IndexWriterConfig(
						new SimpleAnalyzer());
				config.setOpenMode(OpenMode.CREATE_OR_APPEND);
				writer = new IndexWriter(indexDir, config);
				fIndexWriters.get(dataType).put(elementType, writer);
			} catch (IOException e) {
				Logger.logException(e);
			}
		}
		return writer;
	}

	public synchronized SearcherManager getIndexSearcher(IndexType dataType,
			int elementType) {
		SearcherManager searcher = fIndexSearchers.get(dataType)
				.get(elementType);
		try {
			if (searcher == null) {
				searcher = new SearcherManager(
						getIndexWriter(dataType, elementType), true,
						new SearcherFactory());
				fIndexSearchers.get(dataType).put(elementType, searcher);
			}
			// Try to achieve the up-to-date index state
			searcher.maybeRefresh();
		} catch (IOException e) {
			Logger.logException(e);
		}
		return searcher;
	}

	public synchronized void delete(String sourceModule) {
		Term term = new Term(IndexFields.F_PATH, sourceModule);
		try {
			// Cleanup related time stamp
			getTimestampsWriter().deleteDocuments(term);
			// Cleanup all related documents in data writers
			for (Map<Integer, IndexWriter> dataWriters : fIndexWriters
					.values()) {
				for (IndexWriter writer : dataWriters.values()) {
					writer.deleteDocuments(term);
				}
			}
		} catch (IOException e) {
			Logger.logException(e);
		}
	}

	public synchronized void delete() {
		// Delete container entry entirely
		(new IndexCleaner()).schedule();
	}

	public synchronized void close() {
		try {
			// Close time stamps searcher & writer
			if (fTimestampsSearcher != null)
				fTimestampsSearcher.close();
			if (fTimestampsWriter != null)
				fTimestampsWriter.close();
			// Close all data searchers
			for (Map<Integer, SearcherManager> dataSearchers : fIndexSearchers
					.values()) {
				for (SearcherManager searcher : dataSearchers.values()) {
					if (searcher != null)
						searcher.close();
				}
			}
			// Close all data writers
			for (Map<Integer, IndexWriter> dataWriters : fIndexWriters
					.values()) {
				for (IndexWriter writer : dataWriters.values()) {
					if (writer != null)
						writer.close();
				}
			}
		} catch (IOException e) {
			Logger.logException(e);
		}
	}

}
