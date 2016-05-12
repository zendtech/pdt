/*******************************************************************************
 * Copyright (c) 2016 Zend Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Zend Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.php.index.lucene;

import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.jobs.Job;
import org.osgi.framework.BundleContext;

/**
 * Lucene based DLTK indexer plug-in.
 * 
 * @author Bartlomiej Laczkowski
 */
public class LucenePlugin extends Plugin {

	/**
	 * Plug-in unique ID.
	 */
	public static final String ID = "org.eclipse.php.index.lucene"; //$NON-NLS-1$

	/**
	 * Lucene indexer plug-in job family indicator.
	 */
	public static final Object LUCENE_JOB_FAMILY = new Object();

	private static LucenePlugin plugin;

	public static LucenePlugin getDefault() {
		return plugin;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

	}

	@Override
	public void stop(BundleContext context) throws Exception {
		try {
			Job.getJobManager().join(LUCENE_JOB_FAMILY, null);
		} finally {
			plugin = null;
			super.stop(context);
		}
	}

}
