package org.eclipse.php.internal.core.builder;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.dltk.ast.declarations.ModuleDeclaration;
import org.eclipse.dltk.ast.parser.ISourceParser;
import org.eclipse.dltk.compiler.env.IModuleSource;
import org.eclipse.dltk.compiler.problem.DefaultProblemIdentifier;
import org.eclipse.dltk.compiler.problem.IProblem;
import org.eclipse.dltk.compiler.problem.ProblemCollector;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.DLTKLanguageManager;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.ISourceModuleInfoCache.ISourceModuleInfo;
import org.eclipse.dltk.core.SourceParserUtil;
import org.eclipse.dltk.core.builder.IBuildContext;
import org.eclipse.dltk.internal.core.ModelManager;
import org.eclipse.php.core.libfolders.LibraryFolderManager;
import org.eclipse.php.internal.core.PHPToolkitUtil;
import org.eclipse.php.internal.core.compiler.ast.nodes.UseStatement;
import org.eclipse.php.internal.core.compiler.ast.parser.ASTUtils;
import org.eclipse.php.internal.core.compiler.ast.parser.PhpProblemIdentifier;
import org.eclipse.php.internal.core.project.PHPNature;
import org.eclipse.wst.validation.AbstractValidator;
import org.eclipse.wst.validation.ValidationResult;
import org.eclipse.wst.validation.ValidationState;
import org.eclipse.wst.validation.ValidatorMessage;

public class PhpSourceCodeValidator extends AbstractValidator {

	private ISourceParser parser;

	@Override
	public void validationStarting(IProject project, ValidationState state, IProgressMonitor monitor) {
		parser = DLTKLanguageManager.getSourceParser(project, PHPNature.ID);
	}

	@Override
	public ValidationResult validate(IResource resource, int kind, ValidationState state, IProgressMonitor monitor) {
		ValidationResult result = new ValidationResult();
		if (resource.getType() != IResource.FILE || !(PHPToolkitUtil.isPhpFile((IFile) resource))) {
			return result;
		}

		if (LibraryFolderManager.getInstance().isInLibraryFolder(resource)) {
			// skip syntax check for code inside library folders
			return result;
		}
		ISourceModule sourceModule = (ISourceModule) DLTKCore.create(resource);
		ModuleDeclaration moduleDeclaration = (ModuleDeclaration) state.get(IBuildContext.ATTR_MODULE_DECLARATION);
		if (moduleDeclaration != null) {
			// do nothing if already have AST - optimization for reconcile
			return result;
		}

		// get cache entry
		final ISourceModuleInfo cacheEntry = ModelManager.getModelManager().getSourceModuleInfoCache()
				.get(sourceModule);
				// if full build,do not use cache,or the error marker will not
				// be
				// refreshed
				// if (context.getBuildType() != IScriptBuilder.FULL_BUILD) {
				//
				// // check if there is cached AST
				// moduleDeclaration =
				// SourceParserUtil.getModuleFromCache(cacheEntry,
				// context.getProblemReporter());
				// if (moduleDeclaration != null) {
				// // use AST from cache
				// context.set(IBuildContext.ATTR_MODULE_DECLARATION,
				// moduleDeclaration);
				// return;
				// }
				// }

		// create problem collector
		final ProblemCollector problemCollector = new ProblemCollector();
		// parse
		moduleDeclaration = (ModuleDeclaration) parser.parse((IModuleSource) sourceModule, problemCollector);
		// put result to the cache
		SourceParserUtil.putModuleToCache(cacheEntry, moduleDeclaration, problemCollector);
		// report errors to the build context
		state.put(IBuildContext.ATTR_MODULE_DECLARATION, moduleDeclaration);

		try {
			resource.deleteMarkers(PhpProblemIdentifier.MARKER_TYPE_ID, false, IResource.DEPTH_INFINITE);
		} catch (CoreException e) {
		}

		// Run the validation visitor:
		try {
			String content = sourceModule.getSource();
			UseStatement[] statements = ASTUtils.getUseStatements((ModuleDeclaration) moduleDeclaration,
					content.length());

			moduleDeclaration.traverse(
					new OrganizeBuildParticipantFactory.ImportValidationVisitor(content, problemCollector, statements));
		} catch (Exception e) {
		}

		for (IProblem problem : problemCollector.getProblems()) {
			ValidatorMessage vm = ValidatorMessage.create(problem.getMessage(), resource);
			vm.setType(PhpProblemIdentifier.MARKER_TYPE_ID);
			vm.setAttribute("id", DefaultProblemIdentifier.encode(PhpProblemIdentifier.SYNTAX));
			vm.setAttribute(IMarker.LINE_NUMBER, problem.getSourceLineNumber() + 1);
			vm.setAttribute(IMarker.CHAR_START, problem.getSourceStart());
			vm.setAttribute(IMarker.CHAR_END, problem.getSourceEnd());
			vm.setAttribute(IMarker.USER_EDITABLE, false);
			vm.setAttribute(IMarker.SEVERITY, problem.getSeverity().value);
			result.add(vm);
		}
		return result;
	}

}
