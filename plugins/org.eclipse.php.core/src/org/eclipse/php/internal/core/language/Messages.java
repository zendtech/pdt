package org.eclipse.php.internal.core.language;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.php.internal.core.language.messages"; //$NON-NLS-1$
	public static String DefaultLanguageModelProvider_1;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
