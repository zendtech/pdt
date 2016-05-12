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

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.concurrent.Future;

import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.OutputStreamIndexOutput;
import org.apache.lucene.store.RAFDirectory;

/**
 * <p>
 * Default directory implementation that store index files in the file system.
 * </p>
 * <p>
 * This extended implementation of {@link RAFDirectory} prevents from abnormal
 * index closing in case of interrupting indexing/searching threads via
 * {@link Thread#interrupt()} or {@link Future#cancel(boolean)}. As RAFDirectory
 * itself is using {@link FSDirectory.FSIndexOutput} that is vulnerable to
 * thread interruption, this implementation provides additional
 * {@link RAFIndexOutput} as a safe substitution.
 * </p>
 * 
 * @author Bartlomiej Laczkowski
 */
public class IndexDirectory extends RAFDirectory {

	final class RAFIndexOutput extends OutputStreamIndexOutput {

		private static final int CHUNK_SIZE = 8192;
		private static final String DESCRIPTION = "RAFIndexOutput(path=\"{0}\")"; //$NON-NLS-1$

		public RAFIndexOutput(String name) throws IOException {
			super(MessageFormat.format(DESCRIPTION, directory.resolve(name)),
					new RAFOutputStream(
							new FileOutputStream(new File(
									directory.resolve(name).toString())),
							CHUNK_SIZE),
					CHUNK_SIZE);
		}

	}

	final static class RAFOutputStream extends FilterOutputStream {

		final private int fChunkSize;

		public RAFOutputStream(OutputStream out, int chunkSize) {
			super(out);
			fChunkSize = chunkSize;
		}

		@Override
		public void write(byte[] b, int offset, int length) throws IOException {
			while (length > 0) {
				final int chunk = Math.min(length, fChunkSize);
				out.write(b, offset, chunk);
				length -= chunk;
				offset += chunk;
			}
		}

	}

	public IndexDirectory(Path path) throws IOException {
		super(path);
	}

	public IndexDirectory(Path path, LockFactory lockFactory)
			throws IOException {
		super(path, lockFactory);
	}

	@Override
	public IndexOutput createOutput(String name, IOContext context)
			throws IOException {
		ensureOpen();
		ensureCanWrite(name);
		return new RAFIndexOutput(name);
	}

}
