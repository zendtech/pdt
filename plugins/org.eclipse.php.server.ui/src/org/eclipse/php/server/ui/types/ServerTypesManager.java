package org.eclipse.php.server.ui.types;

import java.util.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.php.internal.server.core.Server;
import org.eclipse.php.internal.server.ui.Activator;
import org.eclipse.php.internal.ui.wizards.WizardFragmentsFactoryRegistry;
import org.eclipse.php.ui.wizards.ICompositeFragmentFactory;

public class ServerTypesManager {

	protected static final String FRAGMENT_GROUP_ID = "org.eclipse.php.server.ui.serverWizardAndComposite"; //$NON-NLS-1$

	private static ServerTypesManager manager;
	private Map<String, IServerType> types;
	private Map<String, List<String>> wizardFragments;
	private Map<String, List<String>> settingsFragments;

	private ServerTypesManager() {
	}

	public static synchronized ServerTypesManager getInstance() {
		if (manager == null) {
			manager = new ServerTypesManager();
			manager.init();
		}
		return manager;
	}

	public IServerType getType(String id) {
		return id != null ? types.get(id) : new BasicServerType();
	}

	public IServerType getType(Server server) {
		return getType(server.getAttribute(IServerType.TYPE, null));
	}

	public ICompositeFragmentFactory[] getWizardFragmentFactories(
			IServerType type) {
		ICompositeFragmentFactory[] factories = WizardFragmentsFactoryRegistry
				.getFragmentsFactories(FRAGMENT_GROUP_ID);
		if (type == null || wizardFragments.get(type.getId()) == null
				|| wizardFragments.get(type.getId()).isEmpty()) {
			return factories;
		}
		List<ICompositeFragmentFactory> result = new ArrayList<ICompositeFragmentFactory>();
		List<String> vaildFragments = wizardFragments.get(type.getId());
		for (ICompositeFragmentFactory factory : factories) {
			if (vaildFragments.contains(factory.getId())) {
				result.add(factory);
			}
		}
		return result.toArray(new ICompositeFragmentFactory[result.size()]);
	}

	public ICompositeFragmentFactory[] getSettingsFragmentFactories(
			String typeId) {
		ICompositeFragmentFactory[] factories = WizardFragmentsFactoryRegistry
				.getFragmentsFactories(FRAGMENT_GROUP_ID);
		if (typeId == null || settingsFragments.get(typeId) == null
				|| settingsFragments.get(typeId).isEmpty()) {
			return factories;
		}
		List<ICompositeFragmentFactory> result = new ArrayList<ICompositeFragmentFactory>();
		List<String> vaildFragments = settingsFragments.get(typeId);
		for (ICompositeFragmentFactory factory : factories) {
			if (vaildFragments.contains(factory.getId())) {
				result.add(factory);
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
		Map<String, List<String>> wizardFragments = new HashMap<String, List<String>>();
		Map<String, List<String>> settingsFragments = new HashMap<String, List<String>>();
		List<IServerType> result = new ArrayList<IServerType>();
		for (IConfigurationElement element : elements) {
			IServerType type = null;
			if ("type".equals(element.getName())) { //$NON-NLS-1$
				try {
					Object contribution = element
							.createExecutableExtension("class"); //$NON-NLS-1$
					if (contribution instanceof IServerType) {
						type = (IServerType) contribution;
					}
				} catch (CoreException e) {
					Activator.logError(e);
				}
			}
			IConfigurationElement[] children = element.getChildren();
			List<String> settingsParts = new ArrayList<String>();
			List<String> wizardParts = new ArrayList<String>();
			for (IConfigurationElement child : children) {
				String name = child.getName();
				if ("wizard".equals(name)) { //$NON-NLS-1$
					wizardParts.addAll(getFragments(child));
				} else if ("settings".equals(name)) { //$NON-NLS-1$
					settingsParts.addAll(getFragments(child));
				} else if ("wizardAndSettings".equals(name)) { //$NON-NLS-1$
					List<String> fragments = getFragments(child);
					wizardParts.addAll(fragments);
					settingsParts.addAll(fragments);
				}
			}
			if (type != null) {
				wizardFragments.put(type.getId(), wizardParts);
				settingsFragments.put(type.getId(), settingsParts);
				result.add(type);
			}
		}
		types = new LinkedHashMap<String, IServerType>();
		for (IServerType type : result) {
			types.put(type.getId(), type);
		}
		this.wizardFragments = wizardFragments;
		this.settingsFragments = settingsFragments;
		IServerType basicType = new BasicServerType();
		types.put(basicType.getId(), basicType);
		wizardFragments.put(basicType.getId(), new ArrayList<String>());
		settingsFragments.put(basicType.getId(), new ArrayList<String>());
	}

	private List<String> getFragments(IConfigurationElement element) {
		List<String> result = new ArrayList<String>();
		IConfigurationElement[] fragments = element.getChildren();
		for (IConfigurationElement fragment : fragments) {
			if ("fragment".equals(fragment.getName())) { //$NON-NLS-1$
				String id = fragment.getAttribute("id"); //$NON-NLS-1$
				result.add(id);
			}
		}
		return result;
	}

}