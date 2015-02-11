package org.eclipse.php.server.ui.types;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

public interface IServerType {

	public enum ImageType {

		ICON_16("icon16"),

		ICON_32("icon32"),

		WIZARD("wizardImage");

		private String attribute;

		public String getAttribute() {
			return attribute;
		}

		private ImageType(String attribute) {
			this.attribute = attribute;
		}

	}

	public static final String TYPE = "serverType"; //$NON-NLS-1$

	String getId();
	
	String getName();

	String getDescription();

	Image getImage(ImageType type);

	ImageDescriptor getImageDescriptor(ImageType type);

}
