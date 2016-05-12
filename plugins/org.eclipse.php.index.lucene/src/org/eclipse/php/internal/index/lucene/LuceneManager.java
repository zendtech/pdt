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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.SearcherManager;
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
import org.eclipse.php.index.lucene.LucenePlugin;

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

	private final class SaveParticipant extends Job
			implements ISaveParticipant {

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
			IndexManager indexManager = ModelManager.getModelManager()
					.getIndexManager();
			// Wait for indexer before shutting down
			while (indexManager.awaitingJobsCount() > 0) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					Logger.logException(e);
				}
			}
			shutdown();
			return Status.OK_STATUS;
		}

	}

	private static final String INDEX_DIR = "index"; //$NON-NLS-1$
	private static final String PROPERTIES_FILE = ".properties"; //$NON-NLS-1$
	private static final String MAPPINGS_FILE = ".mappings"; //$NON-NLS-1$

	private final String fIndexRoot;
	private final Properties fIndexProperties;
	private final Properties fContainerMappings;
	private final Map<String, IndexContainer> fIndexContainers;

	private LuceneManager() {
		fIndexProperties = new Properties();
		fContainerMappings = new Properties();
		fIndexContainers = new HashMap<>();
		fIndexRoot = Platform
				.getStateLocation(LucenePlugin.getDefault().getBundle())
				.append(INDEX_DIR).toOSString();
		File indexRootDirectory = new File(fIndexRoot);
		if (!indexRootDirectory.exists()) {
			indexRootDirectory.mkdirs();
		}
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
	public final IndexWriter findIndexWriter(String container,
			IndexType dataType, int elementType) {
		return getIndexContainer(container).getIndexWriter(dataType,
				elementType);
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
	public final SearcherManager findIndexSearcher(String container,
			IndexType dataType, int elementType) {
		return getIndexContainer(container).getIndexSearcher(dataType,
				elementType);
	}

	/**
	 * Finds and returns time stamps index writer for given container.
	 * 
	 * @param container
	 * @return time stamps index writer
	 */
	public final IndexWriter findTimestampsWriter(String container) {
		return getIndexContainer(container).getTimestampsWriter();
	}

	/**
	 * Finds and returns time stamps index searcher for given container.
	 * 
	 * @param container
	 * @return time stamps index searcher
	 */
	public final SearcherManager findTimestampsSearcher(String container) {
		return getIndexContainer(container).getTimestampsSearcher();
	}

	/**
	 * Deletes related container index entry (container entry is removed
	 * completely).
	 * 
	 * @param container
	 */
	public final void delete(final String container) {
		deleteIndexContainer(container);
	}

	/**
	 * Deletes given container's source module index data.
	 * 
	 * @param container
	 * @param sourceModule
	 */
	public final void delete(String container, String sourceModule) {
		if (fContainerMappings.getProperty(container) != null) {
			getIndexContainer(container).delete(sourceModule);
		}
	}

	private synchronized void startup() {
		loadProperties();
		boolean purgeIndexRoot = false;
		boolean resetProperties = false;
		String modelVersion = fIndexProperties
				.getProperty(IndexProperties.KEY_MODEL_VERSION);
		String luceneVersion = fIndexProperties
				.getProperty(IndexProperties.KEY_LUCENE_VERSION);
		if (!IndexProperties.MODEL_VERSION.equals(modelVersion)
				|| !IndexProperties.LUCENE_VERSION.equals(luceneVersion)) {
			purgeIndexRoot = true;
			resetProperties = true;
		}
		if (purgeIndexRoot) {
			purge();
		}
		if (resetProperties) {
			resetProperties();
			saveProperties();
		}
		loadMappings();
		registerIndexContainers();
		try {
			ResourcesPlugin.getWorkspace().addSaveParticipant(LucenePlugin.ID,
					new SaveParticipant());
		} catch (CoreException e) {
			Logger.logException(e);
		}
	}

	private synchronized void shutdown() {
		// Close all searchers & writers in all container entries
		for (IndexContainer entry : fIndexContainers.values()) {
			entry.close();
		}
		cleanup();
	}

	private synchronized IndexContainer getIndexContainer(String container) {
		String containerId = fContainerMappings.getProperty(container);
		if (containerId == null) {
			do {
				// Just to be sure that ID does not already exist
				containerId = UUID.randomUUID().toString();
			} while (fContainerMappings.containsValue(containerId));
			fContainerMappings.put(container, containerId);
			fIndexContainers.put(containerId,
					new IndexContainer(fIndexRoot, containerId));
			// Persist mapping
			saveMappings();
		}
		return fIndexContainers.get(containerId);
	}

	private synchronized void deleteIndexContainer(String container) {
		String containerId = (String) fContainerMappings.remove(container);
		if (containerId != null) {
			IndexContainer containerEntry = fIndexContainers
					.remove(containerId);
			saveMappings();
			containerEntry.delete();
		}
	}

	private void registerIndexContainers() {
		for (String container : fContainerMappings.stringPropertyNames()) {
			String containerId = fContainerMappings.getProperty(container);
			fIndexContainers.put(containerId,
					new IndexContainer(fIndexRoot, containerId));
		}
	}

	private void loadProperties() {
		File file = Paths.get(fIndexRoot, PROPERTIES_FILE).toFile();
		if (!file.exists()) {
			return;
		}
		try (FileInputStream fis = new FileInputStream(file)) {
			fIndexProperties.load(fis);
		} catch (IOException e) {
			Logger.logException(e);
		}
	}

	private void loadMappings() {
		File file = Paths.get(fIndexRoot, MAPPINGS_FILE).toFile();
		if (!file.exists()) {
			return;
		}
		try (FileInputStream fis = new FileInputStream(file)) {
			fContainerMappings.load(fis);
		} catch (IOException e) {
			Logger.logException(e);
		}
	}

	private void saveProperties() {
		File file = Paths.get(fIndexRoot, PROPERTIES_FILE).toFile();
		try (FileOutputStream fos = new FileOutputStream(file)) {
			fIndexProperties.store(fos, ""); //$NON-NLS-1$
		} catch (IOException e) {
			Logger.logException(e);
		}
	}

	private void saveMappings() {
		File file = Paths.get(fIndexRoot, MAPPINGS_FILE).toFile();
		try (FileOutputStream fos = new FileOutputStream(file)) {
			fContainerMappings.store(fos, ""); //$NON-NLS-1$
		} catch (IOException e) {
			Logger.logException(e);
		}
	}

	private void resetProperties() {
		fIndexProperties.clear();
		fIndexProperties.put(IndexProperties.KEY_MODEL_VERSION,
				IndexProperties.MODEL_VERSION);
		fIndexProperties.put(IndexProperties.KEY_LUCENE_VERSION,
				IndexProperties.LUCENE_VERSION);
	}

	private void cleanup() {
		List<String> containers = new ArrayList<>();
		for (IDLTKLanguageToolkit toolkit : DLTKLanguageManager
				.getLanguageToolkits()) {
			DLTKWorkspaceScope scope = ModelManager.getModelManager()
					.getWorkspaceScope(toolkit);
			for (IPath path : scope.enclosingProjectsAndZips()) {
				containers.add(path.toString());
			}
		}
		Set<String> toRemove = new HashSet<>();
		for (String mappedContainer : fContainerMappings
				.stringPropertyNames()) {
			if (!containers.contains(mappedContainer)) {
				toRemove.add(mappedContainer);
			}
		}
		if (!toRemove.isEmpty()) {
			for (String container : toRemove) {
				deleteIndexContainer(container);
			}
			// Save cleaned up container mappings
			saveMappings();
		}
	}

	private void purge() {
		Path indexRoot = Paths.get(fIndexRoot);
		try {
			Files.walkFileTree(indexRoot, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file,
						BasicFileAttributes attrs) throws IOException {
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
		indexRoot.toFile().mkdir();
	}

}
