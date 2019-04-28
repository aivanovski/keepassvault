package com.ivanovsky.passnotes.data.repository.file.dropbox;

import com.dropbox.core.v2.files.FileMetadata;
import com.ivanovsky.passnotes.data.entity.DropboxFile;
import com.ivanovsky.passnotes.data.repository.file.RemoteFileOutputStream;
import com.ivanovsky.passnotes.data.repository.file.dropbox.exception.DropboxException;
import com.ivanovsky.passnotes.data.repository.file.dropbox.exception.DropboxNetworkException;
import com.ivanovsky.passnotes.util.Logger;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

class DropboxFileOutputStream extends RemoteFileOutputStream {

	private static final String TAG = DropboxFileOutputStream.class.getSimpleName();

	private boolean failed;
	private final UUID processingUnitUid;
	private final File outFile;
	private final DropboxFileSystemProvider provider;
	private final DropboxClient client;
	private final DropboxFile file;

	// should be lazy initialized, because new FileOutputStream make file content empty
	private OutputStream out;

	DropboxFileOutputStream(DropboxFileSystemProvider provider,
							DropboxClient client,
							DropboxFile file,
							UUID processingUnitUid) throws FileNotFoundException {
		this.provider = provider;
		this.client = client;
		this.file = file;
		this.outFile = new File(file.getLocalPath());
		this.processingUnitUid = processingUnitUid;
	}

	@Override
	public void write(int b) throws IOException {
		if (failed) {
			return;
		}

		if (out == null) {
			out = new BufferedOutputStream(new FileOutputStream(outFile));
		}

		try {
			out.write(b);
		} catch (IOException e) {
			Logger.printStackTrace(e);

			failed = true;

			provider.onFileUploadFailed(file, processingUnitUid);

			throw new IOException(e);
		}
	}

	@Override
	public void flush() throws IOException {
		if (failed) {
			return;
		}
		if (out == null) {
			return;
		}

		try {
			out.flush();
		} catch (IOException e) {
			Logger.printStackTrace(e);

			failed = true;

			provider.onFileUploadFailed(file, processingUnitUid);

			throw new IOException(e);
		}
	}

	@Override
	public void close() throws IOException {
		if (failed) {
			return;
		}

		FileMetadata metadata;

		try {
			metadata = client.uploadFileOrThrow(file.getRemotePath(), file.getLocalPath());
		} catch (DropboxNetworkException e) {
			Logger.printStackTrace(e);

			provider.onOfflineWriteFinished(file, processingUnitUid);

			throw new IOException(e);

		} catch (DropboxException e) {
			Logger.printStackTrace(e);

			provider.onFileUploadFailed(file, processingUnitUid);

			throw new IOException(e);
		}

		provider.onFileUploadFinished(file, metadata, processingUnitUid);
	}

	@Override
	public File getOutputFile() {
		return outFile;
	}
}
