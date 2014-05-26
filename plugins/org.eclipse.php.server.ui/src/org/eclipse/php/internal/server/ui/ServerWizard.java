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
package org.eclipse.php.internal.server.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.php.internal.server.PHPServerUIMessages;
import org.eclipse.php.internal.server.ui.wizard.ServerTypeCompositeFragmentFactory;
import org.eclipse.php.internal.ui.wizards.FragmentedWizard;
import org.eclipse.php.internal.ui.wizards.WizardFragment;
import org.eclipse.php.internal.ui.wizards.WizardModel;
import org.eclipse.php.server.ui.types.IServerType;
import org.eclipse.php.server.ui.types.ServerTypesManager;
import org.eclipse.php.ui.wizards.ICompositeFragmentFactory;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

/**
 * A Server wizard.
 * 
 * @author shalom
 */

public class ServerWizard extends FragmentedWizard implements INewWizard {

	protected static final String FRAGMENT_GROUP_ID = "org.eclipse.php.server.ui.serverWizardAndComposite"; //$NON-NLS-1$

	private IServerType serverType;

	private ServerTypeWizardFragment serverTypeWizardFragment;

	public ServerWizard() {
		this(PHPServerUIMessages.getString("ServerWizard.serverCreation")); //$NON-NLS-1$
	}

	public ServerWizard(String title, WizardModel taskModel) {
		super(title, null, taskModel);
		setRootFragment(createRootFragment(null));
		ServerTypeCompositeFragmentFactory serverType = new ServerTypeCompositeFragmentFactory();
		serverTypeWizardFragment = (ServerTypeWizardFragment) serverType
				.createWizardFragment();
	}

	public ServerWizard(String title) {
		super(title, null);
		setRootFragment(createRootFragment(null));
		ServerTypeCompositeFragmentFactory serverType = new ServerTypeCompositeFragmentFactory();
		serverTypeWizardFragment = (ServerTypeWizardFragment) serverType
				.createWizardFragment();
	}

	private WizardFragment createRootFragment(final IServerType type) {
		WizardFragment fragment = new WizardFragment() {
			private WizardFragment[] children;

			protected void createChildFragments(List list) {
				if (children != null) {
					loadChildren(children, list);
					return;
				}
				ICompositeFragmentFactory[] factories = ServerTypesManager
						.getInstance().getWizardFragmentFactories(type);
				List<ICompositeFragmentFactory> filtered = new ArrayList<ICompositeFragmentFactory>();
				for (ICompositeFragmentFactory factory : factories) {
					if (factory.isSupported(serverType)) {
						filtered.add(factory);
					}
				}
				if (ServerTypesManager.getInstance().getAll().size() > 0) {
					children = new WizardFragment[filtered.size() + 1];
					children[0] = serverTypeWizardFragment;
					for (int i = 0; i < filtered.size(); i++) {
						children[i + 1] = filtered.get(i)
								.createWizardFragment();
					}
				} else {
					children = filtered.toArray(new WizardFragment[filtered
							.size()]);
				}
				loadChildren(children, list);
			}
		};
		return fragment;
	}

	private void loadChildren(WizardFragment[] children, List list) {
		for (int i = 0; i < children.length; i++) {
			list.add(children[i]);
		}
	}

	@Override
	public void addPages() {
		if (serverTypeWizardFragment != null) {
			IServerType newType = serverTypeWizardFragment.getType();
			if (newType != null && !newType.equals(serverType)) {
				serverType = newType;
				setRootFragment(createRootFragment((newType)));
			}
		}
		super.addPages();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench,
	 * org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// Do nothing
	}
}
