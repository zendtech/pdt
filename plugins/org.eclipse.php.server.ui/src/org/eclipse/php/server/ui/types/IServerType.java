package org.eclipse.php.server.ui.types;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

public interface IServerType {

	public static final String TYPE = "serverType"; //$NON-NLS-1$

	String getName();

	String getDescription();

	Image getViewIcon();

	Image getTypeIcon();

	ImageDescriptor getWizardImage();

	String getId();

	String getHelp();

}
