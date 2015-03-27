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
/**
 *
 */
package org.eclipse.php.internal.debug.core.debugger;

import java.util.HashMap;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.php.debug.daemon.communication.ICommunicationDaemon;
import org.eclipse.php.internal.debug.core.PHPDebugPlugin;
import org.eclipse.php.internal.debug.core.PHPExeUtil;
import org.eclipse.php.internal.debug.core.preferences.PHPexeItem;

/**
 * An abstract implementation of the IDebuggerConfiguration.
 * 
 * @author Shalom Gibly
 * @since PDT 1.0
 */
public abstract class AbstractDebuggerConfiguration implements
		IDebuggerConfiguration {

	private static final String EMPTY_STRING = ""; //$NON-NLS-1$
	private static final String DEBUGGER_SCRIPT = "resources/debugger/validation.php"; //$NON-NLS-1$

	protected Preferences preferences;
	private HashMap<String, String> attributes;
	private ICommunicationDaemon communicationDaemon;

	/**
	 * AbstractDebuggerConfiguration constructor.
	 */
	public AbstractDebuggerConfiguration() {
		preferences = PHPDebugPlugin.getDefault().getPluginPreferences();
		attributes = new HashMap<String, String>();
	}

	/**
	 * Sets an attribute for this debugger.
	 * 
	 * @param id
	 *            The ID.
	 * @param value
	 *            The value.
	 * @see #save()
	 * @see #getAttribute(String)
	 */
	public void setAttribute(String id, String value) {
		if (EMPTY_STRING.equals(preferences.getDefaultString(id))) {
			attributes.put(id, value);
		} else {
			preferences.setValue(id, value);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.php.internal.debug.core.debugger.IDebuggerConfiguration#
	 * getAttribute(java.lang.String)
	 */
	public String getAttribute(String id) {
		String attribute = attributes.get(id);
		if (attribute == null) {
			attribute = preferences.getString(id);
		}
		return attribute;
	}

	/**
	 * Sets the debugger's id.
	 * 
	 * @param id
	 */
	public void setDebuggerId(String id) {
		attributes.put(DEBUGGER_ID, id);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.eclipse.php.internal.debug.core.debugger.IDebuggerConfiguration#
	 * getDebuggerId()
	 */
	public String getDebuggerId() {
		return getAttribute(DEBUGGER_ID);
	}

	/**
	 * Sets the debugger's name.
	 * 
	 * @param name
	 */
	public void setName(String name) {
		attributes.put(DEBUGGER_NAME, name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.php.internal.debug.core.debugger.IDebuggerConfiguration#getName
	 * ()
	 */
	public String getName() {
		return getAttribute(DEBUGGER_NAME);
	}

	/**
	 * Sets the debugger's port.
	 * 
	 * @param port
	 */
	public abstract void setPort(int port);

	/**
	 * Returns the debugger's port number.
	 * 
	 * @return The debugger's port number. -1, if none is defined.
	 */
	public abstract int getPort();

	/**
	 * Returns corresponding PHP debug module id.
	 * 
	 * @return corresponding PHP debug module id
	 */
	public abstract String getModuleId();

	/**
	 * Returns the {@link ICommunicationDaemon} that is related to this debugger
	 * configuration.
	 * 
	 * @return the communicationDaemon (can be null)
	 */
	public ICommunicationDaemon getCommunicationDaemon() {
		return communicationDaemon;
	}

	/**
	 * Sets the {@link ICommunicationDaemon} that is related to this debugger
	 * configuration.
	 * 
	 * @param communicationDaemon
	 *            the communicationDaemon to set
	 */
	public void setCommunicationDaemon(ICommunicationDaemon communicationDaemon) {
		this.communicationDaemon = communicationDaemon;
	}

	/**
	 * Save any plug-in preferences that needs to be saved.
	 */
	public void save() {
		PHPDebugPlugin.getDefault().savePluginPreferences();
	}

	/**
	 * Apply the default values for this debugger configuration and save them.
	 * Note that the changes affecting the PDT immediately.
	 */
	public abstract void applyDefaults();

	/**
	 * Validate debugger configuration for specified {@link PHPexeItem}
	 * instance.
	 * 
	 * @return validation status
	 */
	public abstract IStatus validate(PHPexeItem item);

	protected boolean isInstalled(PHPexeItem exeItem, String extensionId) {
		return PHPExeUtil.hasModule(exeItem, extensionId);
	}

}
