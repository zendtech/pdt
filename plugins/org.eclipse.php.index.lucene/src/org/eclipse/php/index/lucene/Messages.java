package org.eclipse.php.index.lucene;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.php.index.lucene.messages"; //$NON-NLS-1$
	public static String BitFlagsQuery_BitFlagsQueryDescription;
	public static String BitFlagsQuery_MatchOnId;
	public static String BitFlagsQuery_NoMatchOnId;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
