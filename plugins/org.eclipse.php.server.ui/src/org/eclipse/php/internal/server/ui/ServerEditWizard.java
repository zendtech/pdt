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
import org.eclipse.php.internal.server.core.manager.ServersManager;

/**
 * Wizard for editing PHP server settings.
 * 
 * @author Wojciech Galanciak, 2014
 * 
 */
public class ServerEditWizard extends Wizard {

	private Server server;
	private ServerEditPage serverPage;
	private String tabID;

	public ServerEditWizard(Server server) {
		this.server = server.makeCopy();
		setWindowTitle(PHPServerUIMessages.getString("ServerEditWizard.Title")); //$NON-NLS-1$
		setNeedsProgressMonitor(true);
	}

	public ServerEditWizard(Server server, String tabID) {
		this(server);
		this.tabID = tabID;
	}

	@Override
	public void addPages() {
		super.addPages();
		if (tabID != null)
			serverPage = new ServerEditPage(server, tabID);
		else
			serverPage = new ServerEditPage(server);
		addPage(serverPage);
	}

	@Override
	public boolean performFinish() {
		if (serverPage.performFinish()) {
			// Save original server
			try {
				Server originalServer = ServersManager.findServer(server
						.getUniqueId());
				// Server exists, update it
				if (originalServer != null) {
					originalServer.update(server);
				}
			} catch (Throwable e) {
				Logger.logException("Error while saving the server settings", e); //$NON-NLS-1$
				return false;
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean performCancel() {
		serverPage.performCancel();
		return super.performCancel();
	}

}
