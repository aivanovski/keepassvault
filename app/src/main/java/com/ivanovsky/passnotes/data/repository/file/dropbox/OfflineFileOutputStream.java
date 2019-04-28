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
import java.util.UUID;

class OfflineFileOutputStream extends RemoteFileOutputStream {

	private boolean failed;
	private final UUID processingUnitUid;
	private final File outFile;
	private final DropboxFileSystemProvider provider;
	private final OutputStream out;
	private final DropboxFile file;

	OfflineFileOutputStream(DropboxFileSystemProvider provider,
							DropboxFile file,
							UUID processingUnitUid) throws FileNotFoundException {
		this.provider = provider;
		this.file = file;
		this.outFile = new File(file.getLocalPath());
		this.out = new BufferedOutputStream(new FileOutputStream(outFile));
		this.processingUnitUid = processingUnitUid;
	}

	@Override
	public void write(int b) throws IOException {
		try {
			out.write(b);
		} catch (IOException e) {
			Logger.printStackTrace(e);

			failed = true;

			provider.onOfflineWriteFailed(file, processingUnitUid);

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

			provider.onOfflineWriteFailed(file, processingUnitUid);

			throw new IOException(e);
		}
	}

	@Override
	public void close() {
		if (failed) {
			return;
		}

		provider.onOfflineWriteFinished(file, processingUnitUid);
	}

	@Override
	public File getOutputFile() {
		return outFile;
	}
}
