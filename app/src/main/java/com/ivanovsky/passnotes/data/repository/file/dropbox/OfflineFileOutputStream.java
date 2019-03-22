package com.ivanovsky.passnotes.data.repository.file.dropbox;

import com.ivanovsky.passnotes.data.entity.DropboxFile;
import com.ivanovsky.passnotes.data.repository.file.RemoteFileOutputStream;
import com.ivanovsky.passnotes.util.Logger;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

class OfflineFileOutputStream extends RemoteFileOutputStream {

	private final DropboxFileSystemProvider provider;
	private final OutputStream out;
	private final DropboxFile file;
	private final File outFile;
	private boolean failed;

	OfflineFileOutputStream(DropboxFileSystemProvider provider,
							DropboxFile file) throws FileNotFoundException {
		this.provider = provider;
		this.file = file;
		this.outFile = new File(file.getLocalPath());
		this.out = new BufferedOutputStream(new FileOutputStream(outFile));
	}

	@Override
	public void write(int b) throws IOException {
		try {
			out.write(b);
		} catch (IOException e) {
			Logger.printStackTrace(e);

			failed = true;

			provider.onOfflineWriteFailed(file);

			throw new IOException(e);
		}
	}

	@Override
	public void flush() throws IOException {
		try {
			out.flush();
		} catch (IOException e) {
			Logger.printStackTrace(e);

			failed = true;

			provider.onOfflineWriteFailed(file);

			throw new IOException(e);
		}
	}

	@Override
	public void close() {
		if (failed) {
			return;
		}

		provider.onOfflineWriteFinished(file);
	}

	@Override
	public File getOutputFile() {
		return outFile;
	}
}
