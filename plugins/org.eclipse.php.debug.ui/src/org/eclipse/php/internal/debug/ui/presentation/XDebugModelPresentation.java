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
package org.eclipse.php.internal.debug.ui.presentation;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.php.internal.debug.core.xdebug.dbgp.model.DBGpMultiSessionTarget;
import org.eclipse.php.internal.debug.core.xdebug.dbgp.model.DBGpStackFrame;
import org.eclipse.php.internal.debug.core.xdebug.dbgp.model.DBGpTarget;
import org.eclipse.php.internal.debug.core.xdebug.dbgp.model.DBGpThread;
import org.eclipse.php.internal.debug.ui.Logger;
import org.eclipse.php.internal.debug.ui.PHPDebugUIMessages;

import com.ibm.icu.text.MessageFormat;

/**
 * Renders PHP debug elements
 */
@SuppressWarnings("restriction")
public class XDebugModelPresentation extends PHPModelPresentation implements IDebugModelPresentation {

	protected String getStackFrameText(IStackFrame frame) {
		if (frame instanceof DBGpStackFrame) {
			try {
				// Fix bug #160443 (Stack frames line numbers update).
				// Synchronize the top frame with the given values.
				DBGpThread thread = (DBGpThread) frame.getThread();
				DBGpStackFrame topFrame = (DBGpStackFrame) thread.getTopStackFrame();
				if (topFrame != null && topFrame.equals(frame)) {
					frame = topFrame;
				} // end fix

				StringBuffer buffer = new StringBuffer();
				String frameName = frame.getName();
				if (frameName != null && frameName.length() > 0) {
					buffer.append(frame.getName());
					buffer.append(": lineno " + frame.getLineNumber()); //$NON-NLS-1$
				} else {
					buffer.append(((DBGpStackFrame) frame).getSourceName());
					buffer.append(PHPDebugUIMessages.MPresentation_ATLine_1 + (frame.getLineNumber()));
				}
				return buffer.toString();

			} catch (DebugException e) {
				Logger.logException(e);
			}
		}
		return ""; //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.php.internal.debug.ui.presentation.PHPModelPresentation#
	 * getTargetText(org.eclipse.debug.core.model.IDebugTarget)
	 */
	@Override
	protected String getTargetText(IDebugTarget target) {
		String label = ""; //$NON-NLS-1$
		if (target.isTerminated()) {
			label = MessageFormat.format(PHPDebugUIMessages.MPresentation_Terminated_1, new Object[] {});
		}
		String name = PHPDebugUIMessages.MPresentation_PHP_APP_1;
		if (target instanceof DBGpTarget || target instanceof DBGpMultiSessionTarget) {
			name = PHPDebugUIMessages.PHPModelPresentation_PHP_Applications;
			try {
				if (!target.hasThreads() && !target.isTerminated())
					name += PHPDebugUIMessages.XDebugModelPresentation_Waiting;
			} catch (DebugException e) {
			}
		}
		return label + name;
	}

}
