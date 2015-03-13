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
package org.eclipse.php.internal.server.ui.launching;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.php.debug.core.debugger.parameters.IDebugParametersKeys;
import org.eclipse.php.debug.ui.IDebugServerConnectionTest;
import org.eclipse.php.internal.debug.core.IPHPDebugConstants;
import org.eclipse.php.internal.debug.core.PHPDebugPlugin;
import org.eclipse.php.internal.debug.core.preferences.PHPDebugCorePreferenceNames;
import org.eclipse.php.internal.debug.core.preferences.PHPDebuggersRegistry;
import org.eclipse.php.internal.debug.core.xdebug.communication.XDebugCommunicationDaemon;
import org.eclipse.php.internal.debug.ui.wizards.DebuggerCompositeFragment;
import org.eclipse.php.internal.server.PHPServerUIMessages;
import org.eclipse.php.internal.server.core.Server;
import org.eclipse.php.internal.server.core.manager.ServersManager;
import org.eclipse.php.internal.server.ui.ServerEditWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

/**
 * A PHPServerAdvancedTab for selecting advanced debug options, such as 'Debug
 * all Pages', 'Start Debug from' etc.
 * 
 * @author Shalom Gibly
 */
public class PHPServerAdvancedTab extends AbstractLaunchConfigurationTab {

	// flag to be used to decide whether to enable combo in launch config dialog
	// after the user requests a launch, they cannot change it
	private static final String READ_ONLY = "read-only"; //$NON-NLS-1$

	private Button debugFirstPageBt;
	private Button debugAllPagesBt;
	private Button debugStartFromBt;
	private Button debugContinueBt;
	private Button resetBt;
	private Text debugFromTxt;
	protected Button openBrowser;
	protected WidgetListener listener;
	protected ILaunchConfiguration launchConfiguration;
	private Composite sessionGroup;
	protected boolean isOpenInBrowser;
	// private Combo fDebuggersCombo;
	private String debuggerId = PHPDebuggersRegistry.getDefaultDebuggerId();
	private Label debuggerName;
	private Button validateDebuggerBtn;
	private Button configureDebugger;
	private Button breakOnFirstLine;
	public boolean isTextModificationChange;

	private IDebugServerConnectionTest[] debugTesters = new IDebugServerConnectionTest[0];

	/**
	 * Constructor
	 */
	public PHPServerAdvancedTab() {
		listener = new WidgetListener();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse
	 * .swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));

		createDebuggerSelectionControl(composite);
		createBreakControl(composite);
		createAdvanceControl(composite);
		createExtensionControls(composite);

		Dialog.applyDialogFont(composite);
		setControl(composite);
	}

	/**
	 * Create the advanced control.
	 * 
	 * @param composite
	 */
	protected void createAdvanceControl(Composite composite) {
		// == Groups ==
		Group browserGroup = new Group(composite, SWT.NONE);
		browserGroup.setLayout(new GridLayout(1, false));
		browserGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		browserGroup.setText(PHPServerUIMessages
				.getString("PHPServerAdvancedTab.2")); //$NON-NLS-1$

		// Add the Browser group controls
		openBrowser = new Button(browserGroup, SWT.CHECK);
		openBrowser.setText(PHPServerUIMessages
				.getString("PHPServerAdvancedTab.9")); //$NON-NLS-1$
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 3;
		openBrowser.setLayoutData(data);

		sessionGroup = new Composite(browserGroup, SWT.NONE);
		sessionGroup.setLayout(new GridLayout(3, false));
		sessionGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		openBrowser.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent se) {
				Button b = (Button) se.getSource();
				isOpenInBrowser = b.getSelection();
				if (!isOpenInBrowser) {
					debugFirstPageBt.setSelection(true);
					debugAllPagesBt.setSelection(false);
				} else {
					debugFirstPageBt.setSelection(false);
					debugAllPagesBt.setSelection(true);
				}
				debugStartFromBt.setSelection(false);
				debugContinueBt.setSelection(false);
				enableSessionSettingButtons(isOpenInBrowser
						&& ILaunchManager.DEBUG_MODE
								.equals(getLaunchConfigurationDialog()
										.getMode()));
				updateLaunchConfigurationDialog();
			}
		});

		// Add the Session group controls
		debugAllPagesBt = createRadioButton(sessionGroup,
				PHPServerUIMessages.getString("PHPServerAdvancedTab.10")); //$NON-NLS-1$
		data = (GridData) debugAllPagesBt.getLayoutData();
		data.horizontalSpan = 3;
		data.horizontalIndent = 20;

		debugFirstPageBt = createRadioButton(sessionGroup,
				PHPServerUIMessages.getString("PHPServerAdvancedTab.11")); //$NON-NLS-1$
		data = (GridData) debugFirstPageBt.getLayoutData();
		data.horizontalSpan = 3;
		data.horizontalIndent = 20;

		debugStartFromBt = createRadioButton(sessionGroup,
				PHPServerUIMessages.getString("PHPServerAdvancedTab.12")); //$NON-NLS-1$
		data = (GridData) debugStartFromBt.getLayoutData();
		data.horizontalIndent = 20;

		debugFromTxt = new Text(sessionGroup, SWT.SINGLE | SWT.BORDER);
		debugFromTxt.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		resetBt = createPushButton(sessionGroup,
				PHPServerUIMessages.getString("PHPServerAdvancedTab.13"), null); //$NON-NLS-1$
		resetBt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (launchConfiguration != null) {
					try {
						debugFromTxt.setText(launchConfiguration.getAttribute(
								Server.BASE_URL, "")); //$NON-NLS-1$
					} catch (CoreException e1) {
					}
				}
			}
		});

		debugContinueBt = createCheckButton(sessionGroup,
				PHPServerUIMessages.getString("PHPServerAdvancedTab.15")); //$NON-NLS-1$
		data = (GridData) debugContinueBt.getLayoutData();
		data.horizontalSpan = 3;
		data.horizontalIndent = 40;

		// Add listeners
		debugStartFromBt.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateDebugFrom();
			}
		});

		updateDebugFrom();

		// Add widget listeners
		debugFirstPageBt.addSelectionListener(listener);
		debugAllPagesBt.addSelectionListener(listener);
		debugContinueBt.addSelectionListener(listener);
		debugStartFromBt.addSelectionListener(listener);
		debugFromTxt.addModifyListener(listener);
	}

	protected void createDebuggerSelectionControl(Composite parent) {
		Group group = new Group(parent, SWT.NONE);
		group.setText(PHPServerUIMessages.getString("PHPServerAdvancedTab.18")); //$NON-NLS-1$
		GridLayout ly = new GridLayout(1, false);
		ly.marginHeight = 0;
		ly.marginWidth = 0;
		group.setLayout(ly);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Composite debuggerServerComp = new Composite(group, SWT.NONE);
		GridLayout layout = new GridLayout(5, false);
		debuggerServerComp.setLayout(layout);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		debuggerServerComp.setLayoutData(data);
		Font font = parent.getFont();
		debuggerServerComp.setFont(font);

		// Add the debuggers combo
		Label label = new Label(debuggerServerComp, SWT.WRAP);
		data = new GridData(SWT.BEGINNING);
		// data.widthHint = 100;
		label.setLayoutData(data);
		label.setFont(font);
		label.setText(PHPServerUIMessages.getString("PHPServerAdvancedTab.19")); //$NON-NLS-1$

		debuggerName = new Label(debuggerServerComp, SWT.NONE);
		//		debuggerName.setFont(JFaceResources.getFontRegistry().getBold("")); //$NON-NLS-1$

		Label separator = new Label(debuggerServerComp, SWT.NONE);
		data = new GridData(SWT.BEGINNING);
		data.widthHint = 20;
		separator.setLayoutData(data);

		validateDebuggerBtn = createPushButton(debuggerServerComp,
				PHPServerUIMessages.getString("PHPServerAdvancedTab.22"), null); //$NON-NLS-1$
		validateDebuggerBtn.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				updateDebugServerTesters();
				String serverName = null;
				try {
					serverName = launchConfiguration.getAttribute(Server.NAME,
							(String) null);
				} catch (CoreException e) {
					// TODO handle
				}
				if (serverName != null) {
					Server server = ServersManager.getServer(serverName);
					for (IDebugServerConnectionTest debugServerTester : debugTesters) {
						debugServerTester.testConnection(server, getShell());
					}
				}
			}
		});

		configureDebugger = createPushButton(debuggerServerComp,
				PHPServerUIMessages.getString(PHPServerUIMessages
						.getString("PHPServerAdvancedTab.23")), null); //$NON-NLS-1$
		configureDebugger.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				handleConfigureDebuggerSelected();
			}
		});

		// initialize the debuggers list
		// fillDebuggers();
	}

	private Server getServer() {
		try {
			String serverName = launchConfiguration.getAttribute(Server.NAME,
					""); //$NON-NLS-1$
			Server server = ServersManager.getServer(serverName);
			return server;
		} catch (CoreException e) {
			// Should not happen
		}
		return null;
	}

	// In case this is a debug mode, display 'Break on first line' attribute
	// checkbox.
	protected void createBreakControl(Composite parent) {

		Group group = new Group(parent, SWT.NONE);
		group.setText(PHPServerUIMessages.getString("PHPServerAdvancedTab.24")); //$NON-NLS-1$
		group.setLayout(new GridLayout(1, false));
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		breakOnFirstLine = createCheckButton(group,
				PHPServerUIMessages.getString("PHPServerAdvancedTab.25")); //$NON-NLS-1$
		breakOnFirstLine.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				setDirty(true);
				updateLaunchConfigurationDialog();
			}
		});
		// Disables/Enables all the controls according the the debug mode.
		String mode = getLaunchConfigurationDialog().getMode();
		boolean isDebugMode = ILaunchManager.DEBUG_MODE.equals(mode);
		breakOnFirstLine.setEnabled(isDebugMode);
	}

	protected void handleDebuggerChanged() {
		boolean isXDebug = isXdebug();
		openBrowser.setEnabled(!isXDebug);
		sessionGroup.setVisible(!isXDebug);
		openBrowser.setSelection(isXDebug || debugFirstPageBt.getEnabled());
		if (isXDebug) {
			openBrowser.setText(PHPServerUIMessages
					.getString("PHPServerAdvancedTab.20")); //$NON-NLS-1$
		} else {
			openBrowser.setText(PHPServerUIMessages
					.getString("PHPServerAdvancedTab.21")); //$NON-NLS-1$
		}
		updateLaunchConfigurationDialog();
		updateDebugServerTesters();
	}

	protected void handleConfigureDebuggerSelected() {
		String serverName = null;
		try {
			serverName = launchConfiguration.getAttribute(Server.NAME,
					(String) null);
		} catch (CoreException e) {
			// TODO handle
		}
		if (serverName != null) {
			Server server = ServersManager.getServer(serverName);
			Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
					.getShell();
			NullProgressMonitor monitor = new NullProgressMonitor();
			ServerEditWizard wizard = new ServerEditWizard(server,
					DebuggerCompositeFragment.ID);
			WizardDialog dialog = new WizardDialog(shell, wizard);
			if (dialog.open() == Window.CANCEL) {
				monitor.setCanceled(true);
				return;
			}
			ServersManager.save();
			String previousDebuggerId = debuggerId;
			debuggerId = server.getDebuggerId();
			if (!debuggerId.equals(previousDebuggerId))
				setDebugger();
		}

	}

	/**
	 * Populates the debuggers with the debuggers defined in the workspace.
	 */
	protected void setDebugger() {
		Server server = getServer();
		if (server == null) {
			server = ServersManager.getDefaultServer(null);
		}
		debuggerId = server.getDebuggerId();
		debuggerName.setText(PHPDebuggersRegistry.getDebuggerName(debuggerId));
		handleDebuggerChanged();
	}

	/**
	 * Set multiple control enablement state.
	 * 
	 * @param enabled
	 * @param controls
	 */
	protected void setEnabled(boolean enabled, Control... controls) {
		for (Control c : controls) {
			c.setEnabled(enabled);
		}
	}

	private void enableSessionSettingButtons(boolean isOpenInBrowser) {
		// also check for debug mode.
		String mode = getLaunchConfigurationDialog().getMode();
		isOpenInBrowser = isOpenInBrowser
				&& ILaunchManager.DEBUG_MODE.equals(mode);
		debugFirstPageBt.setEnabled(isOpenInBrowser);
		debugAllPagesBt.setEnabled(isOpenInBrowser);
		debugStartFromBt.setEnabled(isOpenInBrowser);
		debugContinueBt.setEnabled(false);
		resetBt.setEnabled(false);
		debugFromTxt.setEnabled(false);
	}

	/**
	 * Override this method to add more widgets to this tab.
	 * 
	 * @param composite
	 */
	protected void createExtensionControls(Composite composite) {
		return;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
	 */
	public String getName() {
		return PHPServerUIMessages.getString("PHPServerAdvancedTab.45"); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse
	 * .debug.core.ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration configuration) {
		launchConfiguration = configuration;
		initializeDebuggerControl(configuration);
		boolean isXdebugger = isXdebug();
		try {
			// Zend debugger have the option not to use a browser to start a
			// session. Since XDebug seems to start only
			// with a browser instance, we check for it and enable it anyway.
			isOpenInBrowser = isXdebugger
					|| configuration.getAttribute(
							IPHPDebugConstants.OPEN_IN_BROWSER,
							PHPDebugPlugin.getOpenInBrowserOption());
			// isUsingExternalBrowser = internalWebBrowserAvailable
			// && configuration.getAttribute(
			// IPHPDebugConstants.USE_INTERNAL_BROWSER, false);
			openBrowser.setSelection(isOpenInBrowser);
			if (isXdebugger) {
				openBrowser.setEnabled(false);
			}

			sessionGroup.setVisible(!isXdebugger);

			String debugSetting = configuration.getAttribute(
					IPHPDebugConstants.DEBUGGING_PAGES,
					IPHPDebugConstants.DEBUGGING_ALL_PAGES);
			if (IPHPDebugConstants.DEBUGGING_ALL_PAGES.equals(debugSetting)) {
				debugFirstPageBt.setSelection(false);
				debugAllPagesBt.setSelection(true);
				debugStartFromBt.setSelection(false);
			} else if (IPHPDebugConstants.DEBUGGING_FIRST_PAGE
					.equals(debugSetting)) {
				debugFirstPageBt.setSelection(true);
				debugAllPagesBt.setSelection(false);
				debugStartFromBt.setSelection(false);
			} else if (IPHPDebugConstants.DEBUGGING_START_FROM
					.equals(debugSetting)) {
				debugFirstPageBt.setSelection(false);
				debugAllPagesBt.setSelection(false);
				debugStartFromBt.setSelection(true);
				boolean shouldContinue = configuration.getAttribute(
						IPHPDebugConstants.DEBUGGING_SHOULD_CONTINUE, false);
				debugContinueBt.setSelection(shouldContinue);
			}
			String startFromURL = configuration.getAttribute(
					IPHPDebugConstants.DEBUGGING_START_FROM_URL, ""); //$NON-NLS-1$
			debugFromTxt.setText(startFromURL);
			updateDebugFrom();
			// in case we are dealing with XDebug, enable the browser control
			// anyway and do not restrict to debug mode
			enableSessionSettingButtons(isXdebugger
					|| (isOpenInBrowser && ILaunchManager.DEBUG_MODE
							.equals(getLaunchConfigurationDialog().getMode())));
			if (breakOnFirstLine != null) {
				// init the breakpoint settings
				breakOnFirstLine.setSelection(configuration.getAttribute(
						IDebugParametersKeys.FIRST_LINE_BREAKPOINT,
						PHPDebugPlugin.getStopAtFirstLine()));
			}
		} catch (CoreException e) {
		}
		isValid(configuration);
	}

	protected void initializeDebuggerControl(ILaunchConfiguration configuration) {
		setDebugger();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.debug.ui.AbstractLaunchConfigurationTab#activated(org.eclipse
	 * .debug.core.ILaunchConfigurationWorkingCopy )
	 */
	public void activated(ILaunchConfigurationWorkingCopy workingCopy) {
		super.activated(workingCopy);
		// hide/show the session group in case the debugger type was modified in
		// the 'main' tab
		boolean isXDebug = isXdebug();
		sessionGroup.setVisible(!isXDebug);
		openBrowser.setEnabled(!isXDebug);
		if (isXDebug) {
			openBrowser.setText(PHPServerUIMessages
					.getString("PHPServerAdvancedTab.54")); //$NON-NLS-1$
		} else {
			openBrowser.setText(PHPServerUIMessages
					.getString("PHPServerAdvancedTab.55")); //$NON-NLS-1$
		}
	}

	/*
	 * Aptana addition - Check to see if this is a XDebug configuration. This
	 * value will be used to determine the options to display in this dialog.
	 */
	private boolean isXdebug() {
		return XDebugCommunicationDaemon.XDEBUG_DEBUGGER_ID.equals(debuggerId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse
	 * .debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		launchConfiguration = configuration;
		configuration.setAttribute(PHPDebugCorePreferenceNames.PHP_DEBUGGER_ID,
				debuggerId);
		configuration.setAttribute(IDebugParametersKeys.FIRST_LINE_BREAKPOINT,
				breakOnFirstLine.getSelection());
		configuration.setAttribute(IPHPDebugConstants.OPEN_IN_BROWSER,
				isOpenInBrowser);
		// configuration.setAttribute(IPHPDebugConstants.USE_INTERNAL_BROWSER,
		// internalBrowser.getSelection());
		if (isOpenInBrowser) {
			if (debugAllPagesBt.getSelection()) {
				configuration.setAttribute(IPHPDebugConstants.DEBUGGING_PAGES,
						IPHPDebugConstants.DEBUGGING_ALL_PAGES);
			} else if (debugFirstPageBt.getSelection()) {
				configuration.setAttribute(IPHPDebugConstants.DEBUGGING_PAGES,
						IPHPDebugConstants.DEBUGGING_FIRST_PAGE);
			} else {
				configuration.setAttribute(IPHPDebugConstants.DEBUGGING_PAGES,
						IPHPDebugConstants.DEBUGGING_START_FROM);
				configuration.setAttribute(
						IPHPDebugConstants.DEBUGGING_START_FROM_URL,
						debugFromTxt.getText());
				configuration.setAttribute(
						IPHPDebugConstants.DEBUGGING_SHOULD_CONTINUE,
						debugContinueBt.getSelection());
			}
		} else {
			// Allow only debug-first-page
			configuration.setAttribute(IPHPDebugConstants.DEBUGGING_PAGES,
					IPHPDebugConstants.DEBUGGING_FIRST_PAGE);
		}
		applyExtension(configuration);
		isTextModificationChange = false; // reset this flag here.
		updateDebugServerTesters();
	}

	/**
	 * Override this method to perform the apply in the extending classes.
	 * 
	 * @param configuration
	 */
	protected void applyExtension(ILaunchConfigurationWorkingCopy configuration) {
		return;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.
	 * debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		launchConfiguration = configuration;
		setErrorMessage(null);
		configuration.setAttribute(IPHPDebugConstants.DEBUGGING_PAGES,
				IPHPDebugConstants.DEBUGGING_ALL_PAGES);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.debug.ui.AbstractLaunchConfigurationTab#isValid(org.eclipse
	 * .debug.core.ILaunchConfiguration)
	 */
	public boolean isValid(ILaunchConfiguration launchConfig) {
		launchConfiguration = launchConfig;
		setMessage(null);
		setErrorMessage(null);
		if (debugStartFromBt.getSelection()) {
			if (debugFromTxt.getText().trim().equals("")) { //$NON-NLS-1$
				setErrorMessage(PHPServerUIMessages
						.getString("PHPServerAdvancedTab.61")); //$NON-NLS-1$
				return false;
			}
			try {
				new URL(debugFromTxt.getText());
			} catch (MalformedURLException mue) {
				setErrorMessage(PHPServerUIMessages
						.getString("PHPServerAdvancedTab.62")); //$NON-NLS-1$
				return false;
			}
		}
		return isValidExtension(launchConfig);
	}

	/**
	 * Override this method to perform the isValid in the extending classes.
	 * 
	 * @param launchConfig
	 * @return true, if the extention is in a valid state.
	 */
	protected boolean isValidExtension(ILaunchConfiguration launchConfig) {
		return true;
	}

	private void updateDebugServerTesters() {
		debugTesters = retrieveAllServerTestExtensions(PHPDebuggersRegistry
				.getDebuggerName(debuggerId));
		if (debugTesters.length == 0) {
			validateDebuggerBtn.setEnabled(false);
		} else {
			validateDebuggerBtn.setEnabled(true);
		}
	}

	private IDebugServerConnectionTest[] retrieveAllServerTestExtensions(
			final String currentDebuggerType) {
		String debugServerTestExtensionName = "org.eclipse.php.debug.ui.debugServerConnectionTest"; //$NON-NLS-1$
		Map<String, IDebugServerConnectionTest> filtersMap = new HashMap<String, IDebugServerConnectionTest>();
		IConfigurationElement[] elements = Platform.getExtensionRegistry()
				.getConfigurationElementsFor(debugServerTestExtensionName);
		for (int i = 0; i < elements.length; i++) {
			IConfigurationElement element = elements[i];
			if ("debugServerTest".equals(element.getName())) { //$NON-NLS-1$
				String debuggerTypeName = elements[i]
						.getAttribute("debuggerTypeName"); //$NON-NLS-1$
				String overridesIds = elements[i].getAttribute("overridesId"); //$NON-NLS-1$
				if (debuggerTypeName.equals(currentDebuggerType)) {// must be
																	// equal to
																	// the
																	// current
																	// selected
																	// type
					String id = element.getAttribute("id"); //$NON-NLS-1$
					if (!filtersMap.containsKey(id)) {
						if (overridesIds != null) {
							StringTokenizer st = new StringTokenizer(
									overridesIds, ", "); //$NON-NLS-1$
							while (st.hasMoreTokens()) {
								filtersMap.put(st.nextToken(), null);
							}
						}
						try {
							filtersMap
									.put(id,
											(IDebugServerConnectionTest) element
													.createExecutableExtension("class")); //$NON-NLS-1$
						} catch (CoreException e) {
							PHPDebugPlugin.log(e);
						}
					}

				}
			}
		}
		Collection<IDebugServerConnectionTest> l = filtersMap.values();
		while (l.remove(null))
			; // remove null elements
		debugTesters = l.toArray(new IDebugServerConnectionTest[l.size()]);
		return debugTesters;
	}

	// Update the 'debug from' related widgets
	private void updateDebugFrom() {
		if (launchConfiguration != null
				&& debugFromTxt.getText().trim().equals("")) { //$NON-NLS-1$
			try {
				debugFromTxt.setText(launchConfiguration.getAttribute(
						Server.BASE_URL, "")); //$NON-NLS-1$
			} catch (CoreException e) {
			}
		}
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				try {
					boolean debugFromSelected = debugStartFromBt.getSelection();
					debugFromTxt.setEnabled(debugFromSelected);
					debugContinueBt.setEnabled(debugFromSelected);
					resetBt.setEnabled(debugFromSelected);
				} catch (SWTException se) {
					// Just in case the widget was disposed (cases such as the
					// configuration deletion).
				}
			}
		});
	}

	protected class WidgetListener extends SelectionAdapter implements
			ModifyListener {
		public void modifyText(ModifyEvent e) {
			// mark that this was a text modification change, so that the apply
			// will not save to the secured storage.
			isTextModificationChange = true;
			updateLaunchConfigurationDialog();
		}

		public void widgetSelected(SelectionEvent e) {
			setDirty(true);
			updateLaunchConfigurationDialog();
		}
	}
}
