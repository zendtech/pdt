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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.IDLTKLanguageToolkit;
import org.eclipse.dltk.core.IDLTKLanguageToolkitExtension;
import org.eclipse.dltk.core.IProjectFragment;
import org.eclipse.dltk.core.IScriptFolder;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.index2.search.ISearchRequestor;
import org.eclipse.dltk.core.search.IDLTKSearchScope;
import org.eclipse.dltk.internal.core.ArchiveFolder;
import org.eclipse.dltk.internal.core.BuiltinScriptFolder;
import org.eclipse.dltk.internal.core.ExternalScriptFolder;
import org.eclipse.dltk.internal.core.ModelManager;
import org.eclipse.dltk.internal.core.ProjectFragment;
import org.eclipse.dltk.internal.core.search.DLTKSearchScope;

/**
 * Class responsible for handling search match.
 * 
 * @author Michal Niewrzal
 */
@SuppressWarnings("restriction")
public class SearchMatchHandler {

	private static class FilePathHandler {

		private IPath fFolderPath = Path.EMPTY;
		private String fFileName;

		public FilePathHandler(String filePath) {
			this.fFileName = filePath;
			int i = filePath.lastIndexOf('/');
			if (i == -1) {
				i = filePath.lastIndexOf('\\');
			}
			if (i != -1) {
				this.fFolderPath = new Path(filePath.substring(0, i));
				this.fFileName = filePath.substring(i + 1);
			}
		}

		public IPath getFolderPath() {
			return fFolderPath;
		}

		public String getFileName() {
			return fFileName;
		}
	}

	private Map<String, IProjectFragment> fProjectFragmentCache = new HashMap<>();
	private Map<String, ISourceModule> fSourceModuleCache = new HashMap<>();
	private ISearchRequestor fSearchRequestor;
	private IDLTKSearchScope fScope;

	/**
	 * Creates new search match handler.
	 * 
	 * @param scope
	 * @param searchRequestor
	 */
	public SearchMatchHandler(IDLTKSearchScope scope,
			ISearchRequestor searchRequestor) {
		this.fScope = scope;
		this.fSearchRequestor = searchRequestor;
	}

	/**
	 * Handle search match.
	 * 
	 * @param match
	 * @param isReference
	 */
	public void handle(SearchMatch match, boolean isReference) {
		String containerPath = match.container;
		IDLTKLanguageToolkit toolkit = ((DLTKSearchScope) fScope)
				.getLanguageToolkit();
		if (toolkit instanceof IDLTKLanguageToolkitExtension
				&& ((IDLTKLanguageToolkitExtension) toolkit)
						.isArchiveFileName(containerPath)) {
			containerPath = containerPath
					+ IDLTKSearchScope.FILE_ENTRY_SEPARATOR;
		}
		if (containerPath.length() != 0 && containerPath
				.charAt(containerPath.length() - 1) != IPath.SEPARATOR) {
			containerPath = containerPath + IPath.SEPARATOR;
		}
		String filePath = match.path;
		final String resourcePath = containerPath + filePath;
		IProjectFragment projectFragment = fProjectFragmentCache
				.get(containerPath);
		if (projectFragment == null) {
			projectFragment = ((DLTKSearchScope) fScope)
					.projectFragment(resourcePath);
			if (projectFragment == null) {
				projectFragment = ((DLTKSearchScope) fScope)
						.projectFragment(containerPath);
			}
			fProjectFragmentCache.put(containerPath, projectFragment);
		}
		if (projectFragment == null) {
			return;
		}
		if (!fScope.encloses(resourcePath)) {
			return;
		}
		ISourceModule sourceModule = fSourceModuleCache.get(resourcePath);
		if (sourceModule == null) {
			if (projectFragment.isArchive()) {
				FilePathHandler filePathHandler = new FilePathHandler(filePath);
				IScriptFolder scriptFolder = new ArchiveFolder(
						(ProjectFragment) projectFragment,
						filePathHandler.getFolderPath());
				sourceModule = scriptFolder
						.getSourceModule(filePathHandler.getFileName());
			} else if (projectFragment.isExternal()) {
				FilePathHandler filePathHandler = new FilePathHandler(filePath);
				IScriptFolder scriptFolder = new ExternalScriptFolder(
						(ProjectFragment) projectFragment,
						filePathHandler.getFolderPath());
				sourceModule = scriptFolder
						.getSourceModule(filePathHandler.getFileName());
			} else if (projectFragment.isBuiltin()) {
				FilePathHandler filePathHandler = new FilePathHandler(filePath);
				IScriptFolder scriptFolder = new BuiltinScriptFolder(
						(ProjectFragment) projectFragment,
						filePathHandler.getFolderPath());
				sourceModule = scriptFolder
						.getSourceModule(filePathHandler.getFileName());
			} else {
				IProject project = projectFragment.getScriptProject()
						.getProject();
				sourceModule = DLTKCore
						.createSourceModuleFrom(project.getFile(filePath));
			}
			fSourceModuleCache.put(resourcePath, sourceModule);
		}
		String name = match.elementName;
		if (name == null) {
			return;
		}
		ModelManager modelManager = ModelManager.getModelManager();
		name = modelManager.intern(name);
		// Pass to requestor
		fSearchRequestor.match(match.elementType, match.flags, match.offset,
				match.length, match.nameOffset, match.nameLength, name,
				match.metadata, match.doc, match.qualifier, match.parent,
				sourceModule, isReference);

	}

}