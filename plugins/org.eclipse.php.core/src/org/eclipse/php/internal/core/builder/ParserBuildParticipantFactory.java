/*******************************************************************************
 * Copyright (c) 2008, 2014 xored software, Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     xored software, Inc. - initial API and Implementation (Alex Panchenko)
 *     Zend Technologies - [424340] Library Folders (Kaloyan Raev)
 *******************************************************************************/
package org.eclipse.php.internal.core.builder;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.dltk.compiler.problem.ProblemCollector;
import org.eclipse.dltk.core.IScriptProject;
import org.eclipse.dltk.core.SourceParserUtil;
import org.eclipse.dltk.core.builder.AbstractBuildParticipantType;
import org.eclipse.dltk.core.builder.IBuildContext;
import org.eclipse.dltk.core.builder.IBuildParticipant;

public class ParserBuildParticipantFactory extends AbstractBuildParticipantType implements IExecutableExtension {

	@Override
	public IBuildParticipant createBuildParticipant(IScriptProject project) throws CoreException {
		if (natureId != null) {
			return new ParserBuildParticipant();
		}
		return null;
	}

	private String natureId = null;

	public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
			throws CoreException {
		natureId = config.getAttribute("nature"); //$NON-NLS-1$
	}

	private static class ParserBuildParticipant implements IBuildParticipant {

		public void build(IBuildContext context) throws CoreException {
			if (context.getBuildType() != IBuildContext.RECONCILE_BUILD) {
				return;
			}

			// create problem collector
			final ProblemCollector problemCollector = new ProblemCollector();
			// collect errors
			SourceParserUtil.getModuleDeclaration(context.getSourceModule(), problemCollector);
			problemCollector.copyTo(context.getProblemReporter());
		}
	}

}
