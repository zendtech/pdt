/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Zend Technologies
 *******************************************************************************/
package org.eclipse.php.internal.server.ui;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.php.internal.server.PHPServerUIMessages;
import org.eclipse.php.internal.server.core.Server;

/**
 * Wizard for editing PHP server settings.
 * 
 * @author Wojciech Galanciak, 2014
 * 
 */
public class ServerEditWizard extends Wizard {

	private Server server;
	private ServerEditPage serverPage;

	public ServerEditWizard(Server server) {
		this.server = server;
		setWindowTitle(PHPServerUIMessages.getString("ServerEditWizard.Title")); //$NON-NLS-1$
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		super.addPages();
		serverPage = new ServerEditPage(server);
		addPage(serverPage);
	}

	@Override
	public boolean performFinish() {
		return serverPage.performFinish();
	}

	@Override
	public boolean performCancel() {
		serverPage.performCancel();
		return super.performCancel();
	}

}
