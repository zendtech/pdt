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
package org.eclipse.php.internal.ui.wizards;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.php.internal.core.documentModel.provisional.contenttype.ContentTypeIdForPHP;
import org.eclipse.php.internal.ui.IPHPHelpContextIds;
import org.eclipse.php.internal.ui.PHPUIMessages;
import org.eclipse.php.internal.ui.util.PHPPluginImages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;

/**
 * This class allows for the creation of a PHP file.
 */
public class PHPFileCreationWizardPage extends WizardPage {

	protected Text containerText;
	protected Text fileText;
	private ISelection selection;

	protected static final String UTF_8 = "UTF 8"; //$NON-NLS-1$
	protected static final String NO_TEMPLATE = "-- none -- "; //$NON-NLS-1$
	protected Label targetResourceLabel;

	/**
	 * Constructor for SampleNewWizardPage.
	 * 
	 * @param pageName
	 */
	public PHPFileCreationWizardPage(final ISelection selection) {
		super("wizardPage"); //$NON-NLS-1$
		setTitle(PHPUIMessages.PHPFileCreationWizardPage_3);
		setDescription(PHPUIMessages.PHPFileCreationWizardPage_4);
		setImageDescriptor(PHPPluginImages.DESC_WIZBAN_ADD_PHP_FILE);
		this.selection = selection;
	}

	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(final Composite parent) {
		final Composite container = new Composite(parent, SWT.NULL);
		final GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 3;
		layout.verticalSpacing = 9;
		Label label = new Label(container, SWT.NULL);
		label.setText(PHPUIMessages.PHPFileCreationWizardPage_5);

		containerText = new Text(container, SWT.BORDER | SWT.SINGLE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 400;
		containerText.setLayoutData(gd);
		containerText.addModifyListener(new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				dialogChanged();
			}
		});

		final Button button = new Button(container, SWT.PUSH);
		button.setText(PHPUIMessages.PHPFileCreationWizardPage_6);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				handleBrowse();
			}
		});

		targetResourceLabel = new Label(container, SWT.NULL);
		targetResourceLabel.setText(PHPUIMessages.PHPFileCreationWizardPage_7);

		fileText = new Text(container, SWT.BORDER | SWT.SINGLE);
		fileText.setFocus();
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		// gd.widthHint = 300;
		fileText.setLayoutData(gd);
		fileText.addModifyListener(new ModifyListener() {
			public void modifyText(final ModifyEvent e) {
				dialogChanged();
			}
		});

		// label = new Label(container, SWT.NULL);
		// label.setText("Templates :");
		//
		// templatesCombo = new Combo(container, SWT.READ_ONLY);
		// templatesCombo.setItems(new String[] { NO_TEMPLATE });
		// templatesCombo.setText(NO_TEMPLATE);
		// gd = new GridData();
		// gd.horizontalSpan = 2;
		// gd.horizontalAlignment = GridData.HORIZONTAL_ALIGN_BEGINNING;
		// templatesCombo.setLayoutData(gd);
		// templatesCombo.addModifyListener(new ModifyListener() {
		// public void modifyText(ModifyEvent e) {
		// dialogChanged();
		// }
		// });

		// label = new Label(container, SWT.NULL);
		// label.setText("Encoding :");

		// encodingSettings = new PhpEncodingSettings(container, "Encoding");
		// gd = new GridData(GridData.FILL_HORIZONTAL);
		// gd.horizontalSpan = 3;
		// encodingSettings.setLayoutData(gd);
		// encodingSettings.setEncoding();

		// encodingCombo = new Combo(container, SWT.READ_ONLY);
		// encodingCombo.setItems(new String[]{UTF_8});
		// encodingCombo.setText(UTF_8);
		// gd = new GridData();
		// gd.horizontalSpan = 2;
		// gd.horizontalAlignment = GridData.HORIZONTAL_ALIGN_BEGINNING;
		// templatesCombo.setLayoutData(gd);
		// templatesCombo.addModifyListener(new ModifyListener() {
		// public void modifyText(ModifyEvent e) {
		// dialogChanged();
		// }
		// });

		initialize();
		dialogChanged();
		setControl(container);
		PlatformUI
				.getWorkbench()
				.getHelpSystem()
				.setHelp(parent,
						IPHPHelpContextIds.CREATING_A_PHP_FILE_WITHIN_A_PROJECT);
	}

	/**
	 * Tests if the current workbench selection is a suitable container to use.
	 */
	private void initialize() {
		if (selection != null && selection.isEmpty() == false
				&& selection instanceof IStructuredSelection) {
			final IStructuredSelection ssel = (IStructuredSelection) selection;
			if (ssel.size() > 1) {
				return;
			}

			Object obj = ssel.getFirstElement();
			if (obj instanceof IAdaptable) {
				obj = ((IAdaptable) obj).getAdapter(IResource.class);
			}

			IContainer container = null;
			if (obj instanceof IResource) {
				if (obj instanceof IContainer) {
					container = (IContainer) obj;
				} else {
					container = ((IResource) obj).getParent();
				}
			}

			if (container != null) {
				containerText.setText(container.getFullPath().toString());
			}
		}
		setInitialFileName(PHPUIMessages.PHPFileCreationWizardPage_8);
	}

	protected void setInitialFileName(final String fileName) {
		fileText.setText(fileName);
		// fixed bug 157145 - highlight the newfile word in the file name input
		fileText.setSelection(0, 7);
	}

	/**
	 * Uses the standard container selection dialog to choose the new value for
	 * the container field.
	 */

	private void handleBrowse() {
		final ContainerSelectionDialog dialog = new ContainerSelectionDialog(
				getShell(), ResourcesPlugin.getWorkspace().getRoot(), false,
				PHPUIMessages.PHPFileCreationWizardPage_9);
		dialog.showClosedProjects(false);
		if (dialog.open() == Window.OK) {
			final Object[] result = dialog.getResult();
			if (result.length == 1)
				containerText.setText(((Path) result[0]).toOSString());
		}
	}

	/**
	 * Ensures that both text fields are set.
	 */
	protected void dialogChanged() {
		final String container = getContainerName();
		final String fileName = getFileName();

		if (container.length() == 0) {
			updateStatus(PHPUIMessages.PHPFileCreationWizardPage_10);
			return;
		}
		final IContainer containerFolder = getContainer(container);
		if (containerFolder == null || !containerFolder.exists()) {
			updateStatus(PHPUIMessages.PHPFileCreationWizardPage_11);
			return;
		}
		if (!containerFolder.getProject().isOpen()) {
			updateStatus(PHPUIMessages.PHPFileCreationWizardPage_12);
			return;
		}
		if (fileName == null) {
			updateStatus(PHPUIMessages.PHPFileCreationWizardPage_15);
			return;
		}
		if (!fileName.equals("") && containerFolder.getFile(new Path(fileName)).exists()) { //$NON-NLS-1$
			updateStatus(PHPUIMessages.PHPFileCreationWizardPage_14);
			return;
		}

		int dotIndex = fileName.lastIndexOf('.');
		if (fileName.length() == 0 || dotIndex == 0) {
			updateStatus(PHPUIMessages.PHPFileCreationWizardPage_15);
			return;
		}

		if (dotIndex != -1) {
			String fileNameWithoutExtention = fileName.substring(0, dotIndex);
			for (int i = 0; i < fileNameWithoutExtention.length(); i++) {
				char ch = fileNameWithoutExtention.charAt(i);
				if (!(Character.isJavaIdentifierPart(ch) || ch == '.' || ch == '-')) {
					updateStatus(PHPUIMessages.PHPFileCreationWizardPage_16);
					return;
				}
			}
		}

		final IContentType contentType = Platform.getContentTypeManager()
				.getContentType(ContentTypeIdForPHP.ContentTypeID_PHP);
		if (!contentType.isAssociatedWith(fileName)) {
			// fixed bug 195274
			// get the extensions from content type
			final String[] fileExtensions = contentType
					.getFileSpecs(IContentType.FILE_EXTENSION_SPEC);
			StringBuffer buffer = new StringBuffer(
					PHPUIMessages.PHPFileCreationWizardPage_17);
			buffer.append(fileExtensions[0]);
			for (String extension : fileExtensions) {
				buffer.append(", ").append(extension); //$NON-NLS-1$
			}
			buffer.append("]"); //$NON-NLS-1$
			updateStatus(buffer.toString());
			return;
		}

		updateStatus(null);
	}

	protected IContainer getContainer(final String text) {
		final Path path = new Path(text);

		final IResource resource = ResourcesPlugin.getWorkspace().getRoot()
				.findMember(path);
		return resource instanceof IContainer ? (IContainer) resource : null;

	}

	protected void updateStatus(final String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}

	public String getContainerName() {
		return containerText.getText();
	}

	public void setContainerName(String containerPath) {
		containerText.setText(containerPath);
	}

	public String getFileName() {
		return fileText.getText();
	}

	public IProject getProject() {
		String projectName = getContainerName();
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IResource resource = root.findMember(new Path(projectName));
		IProject project = null;
		if (resource instanceof IProject) {
			project = (IProject) resource;
		} else if (resource != null) {
			project = resource.getProject();
		}
		return project;
	}
}