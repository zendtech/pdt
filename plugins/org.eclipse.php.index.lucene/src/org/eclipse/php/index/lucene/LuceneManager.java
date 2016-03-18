package org.eclipse.php.index.lucene;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
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
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queries.BooleanFilter;
import org.apache.lucene.queries.TermFilter;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
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
import org.eclipse.dltk.core.search.indexing.IndexManager;
import org.eclipse.dltk.internal.core.ModelManager;

@SuppressWarnings("restriction")
public enum LuceneManager {

	INSTANCE;

	public enum ContainerIndexType {

		DECLARATIONS("1"), //$NON-NLS-1$
		REFERENCES("2"); //$NON-NLS-1$

		private final String fId;

		private ContainerIndexType(String id) {
			this.fId = id;
		}

		public String getId() {
			return fId;
		}

	}

	private final class ContainerIndex {

		private static final String TIMESTAMPS_ID = "0"; //$NON-NLS-1$

		private final String fContainerId;

		private IndexWriter fTimestampsWriter;
		private SearcherManager fTimestampsSearcher;

		private Map<ContainerIndexType, Map<Integer, IndexWriter>> fIndexWriters;
		private Map<ContainerIndexType, Map<Integer, SearcherManager>> fIndexSearchers;

		public ContainerIndex(String containerId) {
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
							fBundlePath.append(INDEX_DIR).append(fContainerId).append(TIMESTAMPS_ID).toOSString()));
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
							.append(dataType.getId()).append(String.valueOf(elementType)).toOSString()));
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
			BooleanFilter filter = new BooleanFilter();
			filter.add(new TermFilter(new Term(IndexFields.F_PATH, sourceModule)), Occur.MUST);
			Query query = new ConstantScoreQuery(filter);
			try {
				// Cleanup related time stamp
				getTimestampsWriter().deleteDocuments(query);
				// Cleanup all related documents in data writers
				for (Map<Integer, IndexWriter> dataWriters : fIndexWriters.values()) {
					for (IndexWriter writer : dataWriters.values()) {
						writer.deleteDocuments(query);
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

		private ContainerIndex fContainerEntry;

		public ContainerCleaner(ContainerIndex containerEntry) {
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
	
	private static final class PropertyKeys {
		
		public static final String VERSION = "version"; //$NON-NLS-1$
		
	}

	private static final String INDEX_DIR = "index"; //$NON-NLS-1$
	private static final String PROPERTIES_FILE = ".properties"; //$NON-NLS-1$
	private static final String MAPPINGS_FILE = ".mappings"; //$NON-NLS-1$
	private static final String VERSION = String.valueOf(1);

	private final IPath fBundlePath;
	private final Properties fProperties;
	private final Map<String, String> fContainerMappings;
	private final Map<String, ContainerIndex> fContainerIndexes;

	private LuceneManager() {
		fProperties = new Properties();
		fContainerMappings = new HashMap<String, String>();
		fContainerIndexes = new HashMap<String, ContainerIndex>();
		fBundlePath = Platform.getStateLocation(LucenePlugin.getDefault().getBundle());
		startup();
	}

	public final IndexWriter findIndexWriter(String container, ContainerIndexType dataType, int elementType) {
		return getContainerIndex(container).getIndexWriter(dataType, elementType);
	}

	public final SearcherManager findIndexSearcher(String container, ContainerIndexType dataType, int elementType) {
		return getContainerIndex(container).getIndexSearcher(dataType, elementType);
	}

	public final IndexWriter findTimestampsWriter(String container) {
		return getContainerIndex(container).getTimestampsWriter();
	}

	public final SearcherManager findTimestampsSearcher(String container) {
		return getContainerIndex(container).getTimestampsSearcher();
	}

	public final void cleanup(final String container) {
		removeContainerIndex(container);
	}

	public final void cleanup(String container, String sourceModule) {
		getContainerIndex(container).cleanup(sourceModule);
	}

	private synchronized void startup() {
		loadProperties();
		loadContainerIds();
		try {
			ResourcesPlugin.getWorkspace().addSaveParticipant(LucenePlugin.ID, new SaveParticipant());
		} catch (CoreException e) {
			Logger.logException(e);
		}
	}

	private synchronized void shutdown() {
		// Close all searchers & writers in all container entries
		for (ContainerIndex entry : fContainerIndexes.values()) {
			entry.close();
		}
		// Save version if there is a need
		String version = fProperties.getProperty(PropertyKeys.VERSION);
		if (!VERSION.equals(version)) {
			fProperties.put(PropertyKeys.VERSION, VERSION);
			saveProperties();
		}
		// TODO - cleanup possibly non-existing container mappings
	}

	private synchronized ContainerIndex getContainerIndex(String container) {
		String containerId = fContainerMappings.get(container);
		if (containerId == null) {
			do {
				// Just to be sure that ID does not already exist
				containerId = UUID.randomUUID().toString();
			} while (fContainerMappings.containsValue(containerId));
			fContainerMappings.put(container, containerId);
			fContainerIndexes.put(containerId, new ContainerIndex(containerId));
			// Persist mapping
			saveContainerIds();
		}
		return fContainerIndexes.get(containerId);
	}

	private synchronized void removeContainerIndex(String container) {
		String containerId = fContainerMappings.remove(container);
		if (containerId != null) {
			ContainerIndex containerIndex = fContainerIndexes.remove(containerId);
			saveContainerIds();
			containerIndex.delete();
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
		File indexDir = Paths.get(fBundlePath.append(INDEX_DIR).toOSString()).toFile();
		if (!indexDir.exists()) {
			indexDir.mkdirs();
		}
		File file = Paths.get(fBundlePath.append(INDEX_DIR).append(MAPPINGS_FILE).toOSString()).toFile();
		ObjectInputStream ois = null;
		try {
			if (!file.exists()) {
				return;
			}
			FileInputStream fis = new FileInputStream(file);
			ois = new ObjectInputStream(fis);
			fContainerMappings.putAll((Map<String, String>) ois.readObject());
			for (String container : fContainerMappings.keySet()) {
				String containerId = fContainerMappings.get(container);
				fContainerIndexes.put(containerId, new ContainerIndex(containerId));
			}
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

}
