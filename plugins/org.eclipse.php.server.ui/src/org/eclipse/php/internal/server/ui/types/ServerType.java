package org.eclipse.php.internal.server.ui.types;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.php.internal.server.ui.Activator;
import org.eclipse.php.server.ui.types.IServerType;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.Bundle;

public class ServerType implements IServerType {

	private IConfigurationElement element;

	private String name;
	private String description;
	private String id;

	public static IServerType create(IConfigurationElement element) {
		ServerType type = new ServerType(element);
		return type.construct();
	}

	private IServerType construct() {
		this.id = element.getAttribute("id");
		this.name = element.getAttribute("name");
		this.description = element.getAttribute("description");
		return this;
	}

	private ServerType(IConfigurationElement element) {
		this.element = element;

	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public Image getImage(ImageType type) {
		return getImage(type.getAttribute());
	}

	@Override
	public ImageDescriptor getImageDescriptor(ImageType type) {
		return getImageDescriptor(type.getAttribute());
	}

	private Image getImage(String name) {
		ImageRegistry regitry = Activator.getDefault().getImageRegistry();
		String id = getId() + name;
		Image image = regitry.get(id);
		if (image == null) {
			ImageDescriptor descriptor = getImageDescriptor(name);
			regitry.put(
					id,
					descriptor != null ? descriptor : ImageDescriptor
							.getMissingImageDescriptor());
			image = regitry.get(id);
		}
		return image;
	}

	/**
	 * Returns an image descriptor for the image referenced by the given
	 * attribute and configuration element, or <code>null</code> if none.
	 * 
	 * @param element
	 *            the configuration element
	 * @param attribute
	 *            the name of the attribute
	 * @return image descriptor or <code>null</code>
	 */
	private ImageDescriptor getImageDescriptor(String attribute) {
		Bundle bundle = Platform.getBundle(element.getContributor().getName());
		String iconPath = element.getAttribute(attribute);
		if (iconPath != null) {
			URL iconURL = FileLocator.find(bundle, new Path(iconPath), null);
			if (iconURL != null) {
				return ImageDescriptor.createFromURL(iconURL);
			} else { // try to search as a URL in case it is absolute path
				try {
					iconURL = FileLocator.find(new URL(iconPath));
					if (iconURL != null) {
						return ImageDescriptor.createFromURL(iconURL);
					}
				} catch (MalformedURLException e) {
					// return null
				}
			}
		}
		return null;
	}

}
