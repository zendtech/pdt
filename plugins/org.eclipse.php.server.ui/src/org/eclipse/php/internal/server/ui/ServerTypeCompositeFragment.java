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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.php.internal.server.PHPServerUIMessages;
import org.eclipse.php.internal.server.core.Server;
import org.eclipse.php.internal.ui.wizards.CompositeFragment;
import org.eclipse.php.internal.ui.wizards.IControlHandler;
import org.eclipse.php.server.ui.types.IServerType;
import org.eclipse.php.server.ui.types.ServerTypesManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

/**
 * Composite fragment for server type selection page.
 * 
 * @author Wojciech Galanciak, 2014
 * 
 */
public class ServerTypeCompositeFragment extends CompositeFragment {

	private List<Button> typeButtons;
	private IServerType currentType;

	public ServerTypeCompositeFragment(Composite parent,
			IControlHandler handler, boolean isForEditing) {
		super(parent, handler, isForEditing);
		this.typeButtons = new ArrayList<Button>();
		setTitle(PHPServerUIMessages
				.getString("ServerTypeCompositeFragment.Title")); //$NON-NLS-1$
		setDescription(PHPServerUIMessages
				.getString("ServerTypeCompositeFragment.Description")); //$NON-NLS-1$
		controlHandler.setTitle(PHPServerUIMessages
				.getString("ServerTypeCompositeFragment.Title")); //$NON-NLS-1$
		controlHandler.setDescription(getDescription());
		controlHandler.setImageDescriptor(ServersPluginImages.DESC_WIZ_SERVER);
		setDisplayName(PHPServerUIMessages
				.getString("ServerCompositeFragment.server")); //$NON-NLS-1$
		createControl();
	}

	@Override
	public boolean performOk() {
		Server server = getServer();
		server.setAttribute(IServerType.TYPE, currentType.getId());
		return true;
	}

	@Override
	public void validate() {
	}

	public void setData(Object server) throws IllegalArgumentException {
		if (server != null && !(server instanceof Server)) {
			throw new IllegalArgumentException(
					"The given object is not a Server"); //$NON-NLS-1$
		}
		super.setData(server);
		validate();
	}

	public Server getServer() {
		return (Server) getData();
	}

	@Override
	public boolean isComplete() {
		return getType() != null;
	}

	public IServerType getType() {
		return currentType;
	}

	protected void createControl() {
		GridLayout layout = new GridLayout(1, true);
		setLayout(layout);
		setLayoutData(new GridData(GridData.FILL_BOTH));

		Composite container = new Composite(this, SWT.NULL);
		container.setLayout(new GridLayout(1, false));

		Collection<IServerType> types = ServerTypesManager.getInstance()
				.getAll();
		for (IServerType type : types) {
			final Button button = new Button(container, SWT.RADIO);
			button.setText(type.getName());
			button.setData(type);
			button.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					currentType = (IServerType) ((Button) e.getSource())
							.getData();
					controlHandler.update();
				}
			});
			typeButtons.add(button);
		}
		validate();
		Dialog.applyDialogFont(this);
	}

}
