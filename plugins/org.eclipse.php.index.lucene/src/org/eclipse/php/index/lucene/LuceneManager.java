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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.util.CharTokenizer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.eclipse.core.resources.ISaveContext;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.dltk.core.DLTKLanguageManager;
import org.eclipse.dltk.core.IDLTKLanguageToolkit;
import org.eclipse.dltk.core.search.indexing.IndexManager;
import org.eclipse.dltk.internal.core.ModelManager;
import org.eclipse.dltk.internal.core.search.DLTKWorkspaceScope;

/**
 * <p>
 * Apache Lucene indexes manager responsible for managing indexes model.
 * </p>
 * <p>
 * Indexes are stored in hierarchical directory structure as follows:
 * <code><pre>
 * index_root
 *   |_container_id
 *     |_declarations
 *       |_model_element_type_id (index data)
 *       ...
 *     |_references
 *       |_model_element_type_id (index data)
 *       ...
 *     |_timestamps (index data)
 * </pre></code>
 * </p>
 * 
 * @author Bartlomiej Laczkowski
 */
@SuppressWarnings("restriction")
public enum LuceneManager {

	/**
	 * Manager Instance.
	 */
	INSTANCE;

	/**
	 * Container index type (declarations or references).
	 */
	public enum ContainerIndexType {

		/**
		 * Index type for storing declarations data.
		 */
		DECLARATIONS("declarations"), //$NON-NLS-1$
		/**
		 * Index type for storing references data.
		 */
		REFERENCES("references"); //$NON-NLS-1$

		private final String fDirectory;

		private ContainerIndexType(String directory) {
			this.fDirectory = directory;
		}

		public String getDirectory() {
			return fDirectory;
		}

	}

	private final class ContainerEntry {

		private static final String TIMESTAMPS_DIR = "timestamps"; //$NON-NLS-1$

		private final String fContainerId;
		private IndexWriter fTimestampsWriter;
		private SearcherManager fTimestampsSearcher;
		private Map<ContainerIndexType, Map<Integer, IndexWriter>> fIndexWriters;
		private Map<ContainerIndexType, Map<Integer, SearcherManager>> fIndexSearchers;

		public ContainerEntry(String containerId) {
			fContainerId = containerId;
			initialize();
		}

		private void initialize() {
			fIndexWriters = new HashMap<ContainerIndexType, Map<Integer, IndexWriter>>();
			fIndexWriters.put(ContainerIndexType.DECLARATIONS, new HashMap<Integer, IndexWriter>());
			fIndexWriters.put(ContainerIndexType.REFERENCES, new HashMap<Integer, IndexWriter>());
			fIndexSearchers = new HashMap<ContainerIndexType, Map<Integer, SearcherManager>>();
			fIndexSearchers.put(ContainerIndexType.DECLARATIONS, new HashMap<Integer, SearcherManager>());
			fIndexSearchers.put(ContainerIndexType.REFERENCES, new HashMap<Integer, SearcherManager>());
		}

		public final String getId() {
			return fContainerId;
		}

		public synchronized IndexWriter getTimestampsWriter() {
			if (fTimestampsWriter == null) {
				try {
					Directory indexDir = FSDirectory.open(Paths.get(
							fBundlePath.append(INDEX_DIR).append(fContainerId).append(TIMESTAMPS_DIR).toOSString()));
					IndexWriterConfig config = new IndexWriterConfig(new DefaultAnalyzer());
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
					fTimestampsSearcher = new SearcherManager(getTimestampsWriter(), true, new SearcherFactory());
				}
				// Try to achieve the up-to-date index state
				fTimestampsSearcher.maybeRefresh();
			} catch (IOException e) {
				Logger.logException(e);
			}
			return fTimestampsSearcher;
		}

		public synchronized IndexWriter getIndexWriter(ContainerIndexType dataType, int elementType) {
			IndexWriter writer = fIndexWriters.get(dataType).get(elementType);
			if (writer == null) {
				try {
					Directory indexDir = FSDirectory.open(Paths.get(fBundlePath.append(INDEX_DIR).append(fContainerId)
							.append(dataType.getDirectory()).append(String.valueOf(elementType)).toOSString()));
					IndexWriterConfig config = new IndexWriterConfig(new DefaultAnalyzer());
					config.setOpenMode(OpenMode.CREATE_OR_APPEND);
					writer = new IndexWriter(indexDir, config);
					fIndexWriters.get(dataType).put(elementType, writer);
				} catch (IOException e) {
					Logger.logException(e);
				}
			}
			return writer;
		}

		public synchronized SearcherManager getIndexSearcher(ContainerIndexType dataType, int elementType) {
			SearcherManager searcher = fIndexSearchers.get(dataType).get(elementType);
			try {
				if (searcher == null) {
					searcher = new SearcherManager(getIndexWriter(dataType, elementType), true, new SearcherFactory());
					fIndexSearchers.get(dataType).put(elementType, searcher);
				}
				// Try to achieve the up-to-date index state
				searcher.maybeRefresh();
			} catch (IOException e) {
				Logger.logException(e);
			}
			return searcher;
		}

		public synchronized void delete() {
			// Delete container entry entirely
			(new ContainerCleaner(this)).schedule();
		}

		public synchronized void cleanup(String sourceModule) {
			Term term = new Term(IndexFields.F_PATH, sourceModule);
			try {
				// Cleanup related time stamp
				getTimestampsWriter().deleteDocuments(term);
				// Cleanup all related documents in data writers
				for (Map<Integer, IndexWriter> dataWriters : fIndexWriters.values()) {
					for (IndexWriter writer : dataWriters.values()) {
						writer.deleteDocuments(term);
					}
				}
			} catch (IOException e) {
				Logger.logException(e);
			}
		}

		public synchronized void close() {
			try {
				// Close time stamps searcher & writer
				if (fTimestampsSearcher != null)
					fTimestampsSearcher.close();
				if (fTimestampsWriter != null)
					fTimestampsWriter.close();
				// Close all data searchers
				for (Map<Integer, SearcherManager> dataSearchers : fIndexSearchers.values()) {
					for (SearcherManager searcher : dataSearchers.values()) {
						if (searcher != null)
							searcher.close();
					}
				}
				// Close all data writers
				for (Map<Integer, IndexWriter> dataWriters : fIndexWriters.values()) {
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

	private final class ContainerCleaner extends Job {

		private ContainerEntry fContainerEntry;

		public ContainerCleaner(ContainerEntry containerEntry) {
			super(""); //$NON-NLS-1$
			setUser(false);
			setSystem(true);
			fContainerEntry = containerEntry;
		}

		@Override
		public boolean belongsTo(Object family) {
			return family == LucenePlugin.LUCENE_JOB_FAMILY;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			fContainerEntry.close();
			Path path = Paths.get(fBundlePath.append(INDEX_DIR).append(fContainerEntry.getId()).toOSString());
			delete(path.toFile());
			return Status.OK_STATUS;
		}

	}

	private final class DefaultAnalyzer extends Analyzer {

		@Override
		protected TokenStreamComponents createComponents(String fieldName) {
			final Tokenizer src = new CharTokenizer() {
				@Override
				protected boolean isTokenChar(int arg0) {
					return true;
				}
			};
			TokenStream tok = new StandardFilter(src);
			return new TokenStreamComponents(src, tok);
		}

	}

	private final class SaveParticipant extends Job implements ISaveParticipant {

		public SaveParticipant() {
			super(""); //$NON-NLS-1$
			setSystem(true);
			setUser(false);
		}

		@Override
		public void doneSaving(ISaveContext context) {
			// ignore
		}

		@Override
		public void prepareToSave(ISaveContext context) throws CoreException {
			// ignore
		}

		@Override
		public void rollback(ISaveContext context) {
			// ignore
		}

		@Override
		public void saving(ISaveContext context) throws CoreException {
			// Close all indexes on workspace save
			if (context.getKind() == ISaveContext.FULL_SAVE)
				schedule();
		}

		@Override
		public boolean belongsTo(Object family) {
			return family == LucenePlugin.LUCENE_JOB_FAMILY;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			CountDownLatch latch = new CountDownLatch(0);
			IndexManager indexManager = ModelManager.getModelManager().getIndexManager();
			// Discard all running requests
			indexManager.discardJobs(null);
			// Wait for indexer before shutting down
			while (indexManager.awaitingJobsCount() > 0) {
				try {
					latch.await(100, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
					Logger.logException(e);
				}
			}
			shutdown();
			return Status.OK_STATUS;
		}

	}

	private static final class IndexProperties {

		private static final String PREFIX = LucenePlugin.ID + ".property."; //$NON-NLS-1$
		
		public static final String KEY_MODEL_VERSION = PREFIX + "model.version"; //$NON-NLS-1$
		public static final String KEY_LUCENE_VERSION = PREFIX + "lucene.version"; //$NON-NLS-1$
		
		public static final String MODEL_VERSION = "1.0"; //$NON-NLS-1$
		public static final String LUCENE_VERSION = Version.LATEST.toString();

	}

	private static final String INDEX_DIR = "index"; //$NON-NLS-1$
	private static final String PROPERTIES_FILE = ".properties"; //$NON-NLS-1$
	private static final String MAPPINGS_FILE = ".mappings"; //$NON-NLS-1$
	
	private final IPath fBundlePath;
	private final Properties fProperties;
	private final Map<String, String> fContainerMappings;
	private final Map<String, ContainerEntry> fContainerEntries;

	private LuceneManager() {
		fProperties = new Properties();
		fContainerMappings = new HashMap<String, String>();
		fContainerEntries = new HashMap<String, ContainerEntry>();
		fBundlePath = Platform.getStateLocation(LucenePlugin.getDefault().getBundle());
		startup();
	}

	/**
	 * Finds and returns index writer for given container, data type and model
	 * element.
	 * 
	 * @param container
	 * @param dataType
	 * @param elementType
	 * @return index writer
	 */
	public final IndexWriter findIndexWriter(String container, ContainerIndexType dataType, int elementType) {
		return getContainerEntry(container).getIndexWriter(dataType, elementType);
	}

	/**
	 * Finds and returns index searcher for given container, data type and model
	 * element.
	 * 
	 * @param container
	 * @param dataType
	 * @param elementType
	 * @return index searcher
	 */
	public final SearcherManager findIndexSearcher(String container, ContainerIndexType dataType, int elementType) {
		return getContainerEntry(container).getIndexSearcher(dataType, elementType);
	}

	/**
	 * Finds and returns time stamps index writer for given container.
	 * 
	 * @param container
	 * @return time stamps index writer
	 */
	public final IndexWriter findTimestampsWriter(String container) {
		return getContainerEntry(container).getTimestampsWriter();
	}

	/**
	 * Finds and returns time stamps index searcher for given container.
	 * 
	 * @param container
	 * @return time stamps index searcher
	 */
	public final SearcherManager findTimestampsSearcher(String container) {
		return getContainerEntry(container).getTimestampsSearcher();
	}

	/**
	 * Cleans up related container index entry (container entry is removed
	 * completely).
	 * 
	 * @param container
	 */
	public final void cleanup(final String container) {
		removeContainerEntry(container);
	}

	/**
	 * Cleans up given container's source module index data.
	 * 
	 * @param container
	 * @param sourceModule
	 */
	public final void cleanup(String container, String sourceModule) {
		getContainerEntry(container).cleanup(sourceModule);
	}

	private synchronized void startup() {
		File indexDir = Paths.get(fBundlePath.append(INDEX_DIR).toOSString()).toFile();
		if (!indexDir.exists()) {
			indexDir.mkdirs();
		}
		loadProperties();
		boolean cleanupIndex = false;
		boolean updateProperties = false;
		String modelVersion = fProperties.getProperty(IndexProperties.KEY_MODEL_VERSION);
		String luceneVersion = fProperties.getProperty(IndexProperties.KEY_LUCENE_VERSION);
		if (!IndexProperties.MODEL_VERSION.equals(modelVersion) || !IndexProperties.LUCENE_VERSION.equals(luceneVersion)) {
			cleanupIndex = true;
			updateProperties = true;
		}
		if (cleanupIndex) {
			delete(indexDir);
			indexDir.mkdirs();
		}
		if (updateProperties) {
			registerProperties();
			saveProperties();
		}
		loadContainerIds();
		registerContainers();
		try {
			ResourcesPlugin.getWorkspace().addSaveParticipant(LucenePlugin.ID, new SaveParticipant());
		} catch (CoreException e) {
			Logger.logException(e);
		}
	}

	private synchronized void shutdown() {
		// Close all searchers & writers in all container entries
		for (ContainerEntry entry : fContainerEntries.values()) {
			entry.close();
		}
		cleanup();
	}

	private synchronized ContainerEntry getContainerEntry(String container) {
		String containerId = fContainerMappings.get(container);
		if (containerId == null) {
			do {
				// Just to be sure that ID does not already exist
				containerId = UUID.randomUUID().toString();
			} while (fContainerMappings.containsValue(containerId));
			fContainerMappings.put(container, containerId);
			fContainerEntries.put(containerId, new ContainerEntry(containerId));
			// Persist mapping
			saveContainerIds();
		}
		return fContainerEntries.get(containerId);
	}

	private synchronized void removeContainerEntry(String container) {
		String containerId = fContainerMappings.remove(container);
		if (containerId != null) {
			ContainerEntry containerEntry = fContainerEntries.remove(containerId);
			saveContainerIds();
			containerEntry.delete();
		}
	}

	private void loadProperties() {
		File indexDir = Paths.get(fBundlePath.append(INDEX_DIR).toOSString()).toFile();
		if (!indexDir.exists()) {
			indexDir.mkdirs();
		}
		File file = Paths.get(fBundlePath.append(INDEX_DIR).append(PROPERTIES_FILE).toOSString()).toFile();
		FileInputStream fis = null;
		try {
			if (!file.exists()) {
				return;
			}
			fis = new FileInputStream(file);
			fProperties.load(fis);
		} catch (Exception e) {
			Logger.logException(e);
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					Logger.logException(e);
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void loadContainerIds() {
		File file = Paths.get(fBundlePath.append(INDEX_DIR).append(MAPPINGS_FILE).toOSString()).toFile();
		ObjectInputStream ois = null;
		try {
			if (!file.exists()) {
				return;
			}
			FileInputStream fis = new FileInputStream(file);
			ois = new ObjectInputStream(fis);
			fContainerMappings.putAll((Map<String, String>) ois.readObject());
		} catch (Exception e) {
			Logger.logException(e);
		} finally {
			if (ois != null) {
				try {
					ois.close();
				} catch (IOException e) {
					Logger.logException(e);
				}
			}
		}
	}

	private void saveProperties() {
		File file = Paths.get(fBundlePath.append(INDEX_DIR).append(PROPERTIES_FILE).toOSString()).toFile();
		FileOutputStream fos = null;
		try {
			if (!file.exists()) {
				file.createNewFile();
			}
			fos = new FileOutputStream(file);
			fProperties.store(fos, ""); //$NON-NLS-1$
		} catch (IOException e) {
			Logger.logException(e);
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					Logger.logException(e);
				}
			}
		}
	}

	private void saveContainerIds() {
		File file = Paths.get(fBundlePath.append(INDEX_DIR).append(MAPPINGS_FILE).toOSString()).toFile();
		ObjectOutputStream oos = null;
		try {
			if (!file.exists()) {
				file.createNewFile();
			}
			FileOutputStream fos = new FileOutputStream(file);
			oos = new ObjectOutputStream(fos);
			oos.writeObject(fContainerMappings);
		} catch (IOException e) {
			Logger.logException(e);
		} finally {
			if (oos != null) {
				try {
					oos.close();
				} catch (IOException e) {
					Logger.logException(e);
				}
			}
		}
	}
	
	private void registerProperties() {
		fProperties.clear();
		fProperties.put(IndexProperties.KEY_MODEL_VERSION, IndexProperties.MODEL_VERSION);
		fProperties.put(IndexProperties.KEY_LUCENE_VERSION, IndexProperties.LUCENE_VERSION);
	}
	
	private void registerContainers() {
		for (String container : fContainerMappings.keySet()) {
			String containerId = fContainerMappings.get(container);
			fContainerEntries.put(containerId, new ContainerEntry(containerId));
		}
	}

	private void cleanup() {
		List<String> containers = new ArrayList<String>();
		for (IDLTKLanguageToolkit toolkit : DLTKLanguageManager.getLanguageToolkits()) {
			DLTKWorkspaceScope scope = ModelManager.getModelManager().getWorkspaceScope(toolkit);
			for (IPath path : scope.enclosingProjectsAndZips()) {
				containers.add(path.toString());
			}
		}
		Set<String> toRemove = new HashSet<String>();
		for (String mappedContainer : fContainerMappings.keySet()) {
			if (!containers.contains(mappedContainer)) {
				toRemove.add(mappedContainer);
			}
		}
		if (!toRemove.isEmpty()) {
			for (String container : toRemove) {
				removeContainerEntry(container);
			}
			// Save cleaned up container mappings
			saveContainerIds();
		}
	}
	
	private boolean delete(File path) {
		if (path.exists()) {
			File[] files = path.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					delete(files[i]);
				} else {
					files[i].delete();
				}
			}
		}
		return (path.delete());
	}

}