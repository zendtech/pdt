/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Zend Technologies
 *******************************************************************************/
package org.eclipse.php.internal.core.codeassist.strategies;

import org.eclipse.dltk.ast.Modifiers;
import org.eclipse.dltk.core.*;
import org.eclipse.dltk.internal.core.ModelElement;
import org.eclipse.dltk.internal.core.SourceRange;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.php.core.codeassist.ICompletionContext;
import org.eclipse.php.core.compiler.PHPFlags;
import org.eclipse.php.internal.core.PHPCorePlugin;
import org.eclipse.php.internal.core.codeassist.ICompletionReporter;
import org.eclipse.php.internal.core.codeassist.contexts.AbstractCompletionContext;
import org.eclipse.php.internal.core.typeinference.FakeMethod;

/**
 * This strategy completes global classes after 'new' statement
 * 
 * @author michael
 */
public class ClassInstantiationStrategy extends GlobalTypesStrategy {

	public ClassInstantiationStrategy(ICompletionContext context) {
		super(context, 0, Modifiers.AccInterface | Modifiers.AccNameSpace
				| Modifiers.AccAbstract);
	}

	public void apply(ICompletionReporter reporter) throws BadLocationException {

		ICompletionContext context = getContext();
		AbstractCompletionContext concreteContext = (AbstractCompletionContext) context;

		IType enclosingClass = null;
		try {
			IModelElement enclosingElement = concreteContext.getSourceModule()
					.getElementAt(concreteContext.getOffset());
			while (enclosingElement instanceof IField) {
				enclosingElement = enclosingElement.getParent();
			}
			if (enclosingElement instanceof IMethod) {
				IModelElement parent = ((IMethod) enclosingElement).getParent();
				if (parent instanceof IType) {
					enclosingClass = (IType) parent;
				}
			}
		} catch (ModelException e) {
			PHPCorePlugin.log(e);
		}

		SourceRange replaceRange = getReplacementRange(context);
		String suffix = getSuffix(concreteContext);

		IType[] types = getTypes(concreteContext);
		for (IType type : types) {

			IMethod ctor = null;
			try {
				for (IMethod method : type.getMethods()) {
					if (method.isConstructor()
							&& method.getParameters() != null
							&& method.getParameters().length > 0) {
						ctor = method;
						if (!PHPFlags.isPrivate(ctor.getFlags())
								|| type.equals(enclosingClass)) {
							ISourceRange sourceRange = type.getSourceRange();
							FakeMethod ctorMethod = new FakeConstructor(
									(ModelElement) type, type.getElementName(),
									sourceRange.getOffset(), sourceRange
											.getLength(), sourceRange
											.getOffset(), sourceRange
											.getLength(), ctor) {

							};
							ctorMethod.setParameters(ctor.getParameters());
							ctorMethod.setParameterInitializers(ctor
									.getParameterInitializers());
							reporter.reportMethod(ctorMethod, suffix,
									replaceRange);
							break;
						}
					}
				}
			} catch (ModelException e) {
				PHPCorePlugin.log(e);
			}
			if (ctor == null) {
				reporter.reportType(type, suffix, replaceRange);
			}
		}

		addSelf(concreteContext, reporter);
	}

	public String getSuffix(AbstractCompletionContext abstractContext) {
		String nextWord = null;
		try {
			nextWord = abstractContext.getNextWord();
		} catch (BadLocationException e) {
			PHPCorePlugin.log(e);
		}
		return "(".equals(nextWord) ? "" : "()"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	class FakeConstructor extends FakeMethod {
		private IMethod ctor;

		public FakeConstructor(ModelElement parent, String name, int offset,
				int length, int nameOffset, int nameLength, IMethod ctor) {
			super(parent, name, offset, length, nameOffset, nameLength);
			this.ctor = ctor;
		}

		public boolean isConstructor() throws ModelException {
			return true;
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof FakeConstructor) {
				FakeConstructor fakeConstructor = (FakeConstructor) o;
				return this.ctor == fakeConstructor.ctor;
			}
			return false;
		}
	}
}