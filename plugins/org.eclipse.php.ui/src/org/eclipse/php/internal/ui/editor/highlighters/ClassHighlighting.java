/*******************************************************************************
 * Copyright (c) 2006, 2016 Zend Corporation and IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   William Candillon {wcandillon@gmail.com} - Initial implementation
 *   Dawid Pakuła [469267]
 *******************************************************************************/
package org.eclipse.php.internal.ui.editor.highlighters;

import java.util.List;

import org.eclipse.php.internal.core.ast.nodes.*;
import org.eclipse.php.internal.core.typeinference.PHPSimpleTypes;
import org.eclipse.php.internal.ui.editor.highlighter.AbstractSemanticApply;
import org.eclipse.php.internal.ui.editor.highlighter.AbstractSemanticHighlighting;

public class ClassHighlighting extends AbstractSemanticHighlighting {

	protected class ClassApply extends AbstractSemanticApply {

		@Override
		public boolean visit(ClassDeclaration clazz) {
			highlight(clazz.getName());
			Expression superClass = clazz.getSuperClass();
			if (superClass instanceof NamespaceName) {
				highlightNamespaceType((NamespaceName) superClass);
			} else if (superClass != null) {
				highlight(superClass);
			}
			return true;
		}

		@Override
		public boolean visit(TraitDeclaration trait) {
			highlight(trait.getName());
			Expression superClass = trait.getSuperClass();
			if (superClass instanceof NamespaceName) {
				highlightNamespaceType((NamespaceName) superClass);
			} else if (superClass != null) {
				highlight(superClass);
			}
			return true;
		}

		@Override
		public boolean visit(ClassInstanceCreation clazz) {
			Expression name = clazz.getClassName().getName();
			if (name instanceof Identifier) {
				highlight(name);
			} else if (name instanceof NamespaceName) {
				highlightNamespaceType((NamespaceName) name);
			}
			return true;
		}

		public boolean visit(InstanceOfExpression instanceOfExpression) {
			Expression name = instanceOfExpression.getClassName().getName();
			if (name instanceof Identifier) {
				highlight(name);
			} else if (name instanceof NamespaceName) {
				highlightNamespaceType((NamespaceName) name);
			}
			return true;
		}

		@Override
		public boolean visit(FormalParameter param) {
			Expression type = param.getParameterType();
			if (type instanceof NamespaceName) {
				highlightNamespaceType((NamespaceName) type);
			} else if (type instanceof Identifier) {
				if (!PHPSimpleTypes.isHintable(((Identifier) type).getName(), param.getAST().apiLevel())) {
					highlight(type);
				}
			}
			return true;
		}

		@Override
		public boolean visit(FunctionDeclaration functionDeclaration) {
			if (functionDeclaration.getReturnType() == null) {
				return true;
			}
			Identifier type = functionDeclaration.getReturnType();
			if (type instanceof NamespaceName) {
				highlightNamespaceType((NamespaceName) type);
			} else if (type != null) {
				if (!PHPSimpleTypes.isHintable(type.getName(), functionDeclaration.getAST().apiLevel())) {
					highlight(type);
				}
			}
			return true;
		}

		public boolean visit(TraitUseStatement node) {
			List<NamespaceName> traitList = node.getTraitList();
			for (NamespaceName namespaceName : traitList) {
				highlightNamespaceType(namespaceName);
			}
			List<TraitStatement> tsList = node.getTsList();
			for (TraitStatement traitStatement : tsList) {
				if (traitStatement instanceof TraitAliasStatement) {
					TraitAliasStatement statement = (TraitAliasStatement) traitStatement;
					if (statement.getAlias().getTraitMethod() instanceof FullyQualifiedTraitMethodReference) {
						FullyQualifiedTraitMethodReference reference = (FullyQualifiedTraitMethodReference) statement
								.getAlias().getTraitMethod();
						highlightNamespaceType(reference.getClassName());
					}

				} else if (traitStatement instanceof TraitPrecedenceStatement) {
					TraitPrecedenceStatement statement = (TraitPrecedenceStatement) traitStatement;
					FullyQualifiedTraitMethodReference reference = statement.getPrecedence().getMethodReference();
					highlightNamespaceType(reference.getClassName());
					traitList = statement.getPrecedence().getTrList();
					for (NamespaceName namespaceName : traitList) {
						highlightNamespaceType(namespaceName);
					}
				}
			}
			return false;
		}

		private void highlightNamespaceType(NamespaceName name) {
			List<Identifier> segments = name.segments();
			Identifier segment = segments.get(segments.size() - 1);
			if (!(segments.size() == 1 && PHPSimpleTypes.isHintable(segment.getName(), name.getAST().apiLevel()))) {
				highlight(segment);
			}
		}
	}

	@Override
	public AbstractSemanticApply getSemanticApply() {
		return new ClassApply();
	}

	@Override
	protected void initDefaultPreferences() {
		getStyle().setEnabledByDefault(true).setDefaultTextColor(0, 80, 50);
	}

	public String getDisplayName() {
		return Messages.ClassHighlighting_0;
	}
}