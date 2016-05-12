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
package org.eclipse.php.internal.index.lucene;

import java.util.StringTokenizer;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.php.index.lucene.LucenePlugin;
import org.osgi.framework.Bundle;

/**
 * Small convenience class to log messages to plugin's log file and also in the
 * console if desired.
 * 
 * @author Bartlomiej Laczkowski
 */
public class Logger {

	private static final String PLUGIN_ID = LucenePlugin.ID;
	private static final String TRACEFILTER_LOCATION = "/debug/tracefilter"; //$NON-NLS-1$

	public static final int OK = IStatus.OK; // 0
	public static final int INFO = IStatus.INFO; // 1
	public static final int WARNING = IStatus.WARNING; // 2
	public static final int ERROR = IStatus.ERROR; // 4
	public static final int OK_DEBUG = 200 + OK;
	public static final int INFO_DEBUG = 200 + INFO;
	public static final int WARNING_DEBUG = 200 + WARNING;
	public static final int ERROR_DEBUG = 200 + ERROR;

	/**
	 * Adds message to log.
	 * 
	 * @param level
	 *            severity level of the message (OK, INFO, WARNING, ERROR,
	 *            OK_DEBUG, INFO_DEBUG, WARNING_DEBUG, ERROR_DEBUG)
	 * @param message
	 *            text to add to the log
	 * @param exception
	 *            exception thrown
	 */
	protected static void _log(int level, String message, Throwable exception) {
		if (level == OK_DEBUG || level == INFO_DEBUG || level == WARNING_DEBUG
				|| level == ERROR_DEBUG) {
			if (!isDebugging())
				return;
		}
		int severity = IStatus.OK;
		switch (level) {
		case INFO_DEBUG:
		case INFO:
			severity = IStatus.INFO;
			break;
		case WARNING_DEBUG:
		case WARNING:
			severity = IStatus.WARNING;
			break;
		case ERROR_DEBUG:
		case ERROR:
			severity = IStatus.ERROR;
		}
		message = (message != null) ? message : "null"; //$NON-NLS-1$
		Status statusObj = new Status(severity, PLUGIN_ID, severity, message,
				exception);
		Bundle bundle = Platform.getBundle(PLUGIN_ID);
		if (bundle != null)
			Platform.getLog(bundle).log(statusObj);
	}

	/**
	 * Prints message to log if category matches /debug/tracefilter option.
	 * 
	 * @param message
	 *            text to print
	 * @param category
	 *            category of the message, to be compared with
	 *            /debug/tracefilter
	 */
	protected static void _trace(String category, String message,
			Throwable exception) {
		if (isTracing(category)) {
			message = (message != null) ? message : "null"; //$NON-NLS-1$
			Status statusObj = new Status(IStatus.OK, PLUGIN_ID, IStatus.OK,
					message, exception);
			Bundle bundle = Platform.getBundle(PLUGIN_ID);
			if (bundle != null)
				Platform.getLog(bundle).log(statusObj);
		}
	}

	/**
	 * Checks if the platform is debugging.
	 * 
	 * @return true if the platform is debugging
	 */
	public static boolean isDebugging() {
		return Platform.inDebugMode();
	}

	/**
	 * Determines if currently tracing a category
	 * 
	 * @param category
	 * @return true if tracing category, false otherwise
	 */
	public static boolean isTracing(String category) {
		if (!isDebugging())
			return false;

		String traceFilter = Platform
				.getDebugOption(PLUGIN_ID + TRACEFILTER_LOCATION);
		if (traceFilter != null) {
			StringTokenizer tokenizer = new StringTokenizer(traceFilter, ","); //$NON-NLS-1$
			while (tokenizer.hasMoreTokens()) {
				String cat = tokenizer.nextToken().trim();
				if (category.equals(cat)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Log message with given severity level.
	 * 
	 * @param level
	 * @param message
	 */
	public static void log(int level, String message) {
		_log(level, message, null);
	}

	/**
	 * Log message with given severity level and exception.
	 * 
	 * @param level
	 * @param message
	 * @param exception
	 */
	public static void log(int level, String message, Throwable exception) {
		_log(level, message, exception);
	}

	/**
	 * Log error message with given exception.
	 * 
	 * @param message
	 * @param exception
	 */
	public static void logException(String message, Throwable exception) {
		_log(ERROR, message, exception);
	}

	/**
	 * Log error with given exception.
	 * 
	 * @param exception
	 */
	public static void logException(Throwable exception) {
		_log(ERROR, exception.getMessage(), exception);
	}

	/**
	 * Trace exception.
	 * 
	 * @param category
	 * @param message
	 * @param exception
	 */
	public static void traceException(String category, String message,
			Throwable exception) {
		_trace(category, message, exception);
	}

	/**
	 * Trace exception.
	 * 
	 * @param category
	 * @param exception
	 */
	public static void traceException(String category, Throwable exception) {
		_trace(category, exception.getMessage(), exception);
	}

	/**
	 * Trace message.
	 * 
	 * @param category
	 * @param message
	 */
	public static void trace(String category, String message) {
		_trace(category, message, null);
	}

}
