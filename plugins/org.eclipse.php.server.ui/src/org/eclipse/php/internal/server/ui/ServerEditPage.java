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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.php.internal.server.IHelpContextIds;
import org.eclipse.php.internal.server.PHPServerUIMessages;
import org.eclipse.php.internal.server.core.Server;
import org.eclipse.php.internal.server.core.manager.ServersManager;
import org.eclipse.php.internal.ui.wizards.CompositeFragment;
import org.eclipse.php.internal.ui.wizards.IControlHandler;
import org.eclipse.php.server.ui.types.IServerType;
import org.eclipse.php.server.ui.types.ServerTypesManager;
import org.eclipse.php.ui.wizards.ICompositeFragmentFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.HelpEvent;
import org.eclipse.swt.events.HelpListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.internal.help.WorkbenchHelpSystem;

/**
 * Wizard page for editing PHP server settings.
 * 
 * @author Wojciech Galanciak, 2014
 * 
 */
public class ServerEditPage extends WizardPage implements IControlHandler {

	protected static final String FRAGMENT_GROUP_ID = "org.eclipse.php.server.ui.serverWizardAndComposite"; //$NON-NLS-1$

	public interface IPostFinish {

		public void perform();

	}

	private Server server;
	private ArrayList<CompositeFragment> runtimeComposites;
	private TabFolder tabs;
	private String tabID;
	private List<IPostFinish> postFinish = new ArrayList<IPostFinish>();

	/**
	 * Instantiate a new server edit wizard page.
	 * 
	 * @param server
	 *            An assigned IServer
	 */
	public ServerEditPage(Server server) {
		super(PHPServerUIMessages.getString("ServerEditPage.Title")); //$NON-NLS-1$
		this.server = server;
		this.runtimeComposites = new ArrayList<CompositeFragment>();
	}

	/**
	 * Instantiate a new server edit wizard page.
	 * 
	 * @param server
	 *            An assigned IServer
	 * @param init
	 *            selected tabe ID.
	 */
	public ServerEditPage(Server server, String tabID) {
		this(server);
		this.tabID = tabID;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.php.internal.server.apache.ui.IControlHandler#setDescription
	 * (java.lang.String)
	 */
	public void setDescription(String desc) {
		super.setMessage(desc);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.php.internal.ui.wizards.IControlHandler#getKind()
	 */
	@Override
	public Kind getKind() {
		return Kind.EDITOR;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.php.internal.server.apache.ui.IControlHandler#getServer()
	 */
	public Server getServer() {
		return server;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.php.internal.server.apache.ui.IControlHandler#setServer(org
	 * .eclipse.wst.server.core.IServer)
	 */
	public void setServer(Server server) {
		this.server = server;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.php.internal.ui.wizards.IControlHandler#run(boolean,
	 * boolean, org.eclipse.jface.operation.IRunnableWithProgress)
	 */
	public void run(boolean fork, boolean cancelable,
			IRunnableWithProgress runnable) throws InvocationTargetException,
			InterruptedException {
		getContainer().run(fork, cancelable, runnable);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets
	 * .Composite)
	 */
	public void createControl(Composite parent) {
		// Create a tabbed container that will hold all the fragments
		tabs = new TabFolder(parent, SWT.NONE);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		tabs.setLayoutData(gd);
		ICompositeFragmentFactory[] factories = ServerTypesManager
				.getInstance().getSettingsFragmentFactories(
						server.getAttribute(IServerType.TYPE, null));
		for (ICompositeFragmentFactory element : factories) {
			if (!element.isSupported(server)) {
				continue;
			}
			TabItem tabItem = new TabItem(tabs, SWT.NONE);
			CompositeFragment fragment = element.createComposite(tabs, this);
			fragment.setData(server);
			tabItem.setText(fragment.getDisplayName());
			tabItem.setControl(fragment);
			tabItem.setData(fragment.getId());
			runtimeComposites.add(fragment);
		}

		getShell().setText(
				PHPServerUIMessages.getString("ServerEditDialog.editServer")); //$NON-NLS-1$
		getShell().setImage(
				ServersPluginImages.get(ServersPluginImages.IMG_SERVER));

		tabs.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				TabItem item = (TabItem) e.item;
				CompositeFragment fragment = (CompositeFragment) item
						.getControl();
				if (fragment instanceof ServerCompositeFragment) {
					IServerType type = ServerTypesManager.getInstance()
							.getType(server);
					if (type != null) {
						((ServerCompositeFragment) fragment)
								.setImageDescriptor(type.getWizardImage());
					}
				}
				setImageDescriptor(fragment.getImageDescriptor());
				setDescription(fragment.getDescription());
				setTitle(fragment.getTitle());
				fragment.validate();
			}
		});

		// set the init selection of tabitem.
		if (tabID != null) {
			setSelect(tabID);
		}

		int selectionIndex = tabs.getSelectionIndex() != -1 ? tabs
				.getSelectionIndex() : 0;
		CompositeFragment selectedFragment = runtimeComposites
				.get(selectionIndex);
		setTitle(selectedFragment.getTitle());
		setDescription(selectedFragment.getDescription());

		setControl(tabs);
		IServerType type = ServerTypesManager.getInstance().getType(server);
		if (type != null) {
			setImageDescriptor(type.getWizardImage());
		}

		parent.setData(WorkbenchHelpSystem.HELP_KEY,
				IHelpContextIds.EDIT_PHP_SERVER);
		parent.addHelpListener(new HelpListener() {
			public void helpRequested(HelpEvent event) {
				Program.launch(IHelpContextIds.EDIT_PHP_SERVER);
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.php.internal.ui.wizards.IControlHandler#update()
	 */
	public void update() {
		for (CompositeFragment composite : runtimeComposites) {
			if (!composite.isComplete()) {
				setPageComplete(false);
				return;
			}
		}
		setPageComplete(true);
	}

	public void performCancel() {
		for (CompositeFragment composite : runtimeComposites) {
			composite.performCancel();
		}
	}

	public boolean performFinish() {
		for (CompositeFragment composite : runtimeComposites) {
			if (!composite.performOk()) {
				return false;
			}
		}
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
		// Perform post finish actions
		performPostFinish();
		return true;
	}

	public void addPostFinish(IPostFinish operation) {
		postFinish.add(operation);
	}

	private void performPostFinish() {
		for (IPostFinish operation : postFinish) {
			operation.perform();
		}
	}

	private void setSelect(String id) {
		if (id == null) {
			return;
		}
		for (int i = 0; i < tabs.getItemCount(); i++) {
			if (id.equals(tabs.getItem(i).getData())) {
				tabs.setSelection(i);
				break;
			}
		}
	}

}
