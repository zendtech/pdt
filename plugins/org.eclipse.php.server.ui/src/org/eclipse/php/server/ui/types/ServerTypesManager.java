package org.eclipse.php.server.ui.types;

import java.util.*;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.php.internal.server.core.Server;
import org.eclipse.php.internal.server.ui.Activator;
import org.eclipse.php.internal.server.ui.types.ServerType;
import org.eclipse.php.internal.ui.wizards.WizardFragmentsFactoryRegistry;
import org.eclipse.php.ui.wizards.ICompositeFragmentFactory;

public class ServerTypesManager {

	private class Fragment {

		private String id;
		private boolean wizard;
		private boolean settings;

		public Fragment(String id, boolean wizard, boolean settings) {
			super();
			this.id = id;
			this.wizard = wizard;
			this.settings = settings;
		}

		public String getId() {
			return id;
		}

		public boolean isSettings() {
			return settings;
		}

		public boolean isWizard() {
			return wizard;
		}

	}

	protected static final String FRAGMENT_GROUP_ID = "org.eclipse.php.server.ui.serverWizardAndComposite"; //$NON-NLS-1$

	private static final String GENERIC_PHP_SERVER_ID = Activator.PLUGIN_ID
			+ ".genericServerType";

	private static ServerTypesManager manager;

	private Map<String, IServerType> types;
	private Map<String, List<Fragment>> fragments;

	private ServerTypesManager() {
	}

	public static synchronized ServerTypesManager getInstance() {
		if (manager == null) {
			manager = new ServerTypesManager();
			manager.init();
		}
		return manager;
	}

	/**
	 * Get {@link IServerType} instance base on specified id. If id is
	 * <code>null</code> then return Generic PHP Server type.
	 * 
	 * @param id
	 * @return
	 */
	public IServerType getType(String id) {
		return id != null ? types.get(id) : types.get(GENERIC_PHP_SERVER_ID);
	}

	public IServerType getType(Server server) {
		return getType(server.getAttribute(IServerType.TYPE, null));
	}

	public ICompositeFragmentFactory[] getWizardFragmentFactories(
			IServerType type) {
		if (type == null) {
			return getWizardFragmentFactories(getType(GENERIC_PHP_SERVER_ID));
		}
		Map<String, ICompositeFragmentFactory> factories = WizardFragmentsFactoryRegistry
				.getFragmentsFactories(FRAGMENT_GROUP_ID);
		List<ICompositeFragmentFactory> result = new ArrayList<ICompositeFragmentFactory>();
		List<Fragment> typeFragments = fragments.get(type.getId());
		for (Fragment fragment : typeFragments) {
			if (fragment.isWizard()) {
				ICompositeFragmentFactory factory = factories.get(fragment
						.getId());
				if (factory != null) {
					result.add(factory);
				}
			}
		}
		return result.toArray(new ICompositeFragmentFactory[result.size()]);
	}

	public ICompositeFragmentFactory[] getSettingsFragmentFactories(
			IServerType type) {
		if (type == null) {
			return getWizardFragmentFactories(getType(GENERIC_PHP_SERVER_ID));
		}
		Map<String, ICompositeFragmentFactory> factories = WizardFragmentsFactoryRegistry
				.getFragmentsFactories(FRAGMENT_GROUP_ID);
		List<ICompositeFragmentFactory> result = new ArrayList<ICompositeFragmentFactory>();
		List<Fragment> typeFragments = fragments.get(type.getId());
		for (Fragment fragment : typeFragments) {
			if (fragment.isSettings()) {
				ICompositeFragmentFactory factory = factories.get(fragment
						.getId());
				if (factory != null) {
					result.add(factory);
				}
			}
		}
		return result.toArray(new ICompositeFragmentFactory[result.size()]);
	}

	public Collection<IServerType> getAll() {
		return types.values();
	}

	private void init() {
		IConfigurationElement[] elements = Platform.getExtensionRegistry()
				.getConfigurationElementsFor(
						"org.eclipse.php.server.ui.serverTypes"); //$NON-NLS-1$
		Map<String, List<Fragment>> fragments = new HashMap<String, List<Fragment>>();
		List<IServerType> result = new ArrayList<IServerType>();
		for (IConfigurationElement element : elements) {
			IServerType type = null;
			if ("type".equals(element.getName())) { //$NON-NLS-1$
				type = ServerType.create(element);
			}
			IConfigurationElement[] children = element.getChildren();
			List<Fragment> typeFragments = new ArrayList<Fragment>();
			for (IConfigurationElement child : children) {
				String name = child.getName();
				if ("fragment".equals(name)) { //$NON-NLS-1$
					String id = child.getAttribute("id");
					String wizardAttribute = child.getAttribute("wizard");
					String settingsAttribute = child.getAttribute("settings");
					boolean wizard = wizardAttribute != null ? Boolean
							.valueOf(wizardAttribute) : true;
					boolean settings = settingsAttribute != null ? Boolean
							.valueOf(settingsAttribute) : true;
					typeFragments.add(new Fragment(id, wizard, settings));
				}
			}
			if (type != null) {
				fragments.put(type.getId(), typeFragments);
				result.add(type);
			}
		}
		types = new LinkedHashMap<String, IServerType>();
		for (IServerType type : result) {
			types.put(type.getId(), type);
		}
		this.fragments = fragments;
	}

}