package com.ivanovsky.passnotes.data.repository.file.dropbox;

import com.dropbox.core.DbxException;
import com.dropbox.core.NetworkIOException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.WriteMode;
import com.ivanovsky.passnotes.data.entity.DropboxFileLink;
import com.ivanovsky.passnotes.data.repository.file.RemoteFileOutputStream;
import com.ivanovsky.passnotes.util.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static com.ivanovsky.passnotes.util.DateUtils.anyLastTimestamp;

public class DropboxFileOutputStream extends RemoteFileOutputStream {

	private static final String TAG = DropboxFileOutputStream.class.getSimpleName();

	private final DropboxFileSystemProvider provider;
	private final DbxClientV2 client;
	private final OutputStream out;
	private final DropboxFileLink link;
	private final File outFile;

	DropboxFileOutputStream(DropboxFileSystemProvider provider,
							DbxClientV2 client,
							DropboxFileLink link) throws IOException {
		this.provider = provider;
		this.client = client;
		this.link = link;
		this.outFile = new File(link.getLocalPath());
		this.out = new BufferedOutputStream(new FileOutputStream(outFile));
	}

	@Override
	public void write(int b) throws IOException {
		try {
			out.write(b);
		} catch (IOException e) {
			Logger.printStackTrace(e);

			link.incrementRetryCount();
			link.setLastRetryTimestamp(System.currentTimeMillis());

//			provider.onFileUploadFinished(link); // TODO: uncomment

			throw new IOException(e);
		}
	}

	@Override
	public void flush() throws IOException {
		try {
			out.flush();
		} catch (IOException e) {
			Logger.printStackTrace(e);

			link.incrementRetryCount();
			link.setLastRetryTimestamp(System.currentTimeMillis());

//			provider.onFileUploadFinished(link); // TODO: uncomment

			throw new IOException(e);
		}
	}

	@Override
	public void close() throws IOException {
		//TODO: add specific flag which will describe if error was occurred and skip uploading

		try {
			out.close();

			FileMetadata metadata = client.files().uploadBuilder(link.getRemotePath())
					.withMode(WriteMode.OVERWRITE)
					.uploadAndFinish(new BufferedInputStream(new FileInputStream(link.getLocalPath())));
			if (metadata != null) {
				link.setDownloaded(true);
				link.setUploaded(true);
				link.setLastModificationTimestamp(anyLastTimestamp(metadata.getServerModified(), metadata.getClientModified()));
				link.setLastDownloadTimestamp(System.currentTimeMillis());
				link.setRevision(metadata.getRev());
				link.setRemotePath(metadata.getPathLower());

				link.setRetryCount(0);
				link.setLastRetryTimestamp(null);

				Logger.d(TAG, "Dropbox file was uploaded successfully: " + link.getRemotePath());
			} else {
				link.incrementRetryCount();
				link.setLastRetryTimestamp(System.currentTimeMillis());
			}

		} catch (IOException | NetworkIOException e) {
			Logger.printStackTrace(e);

			link.incrementRetryCount();
			link.setLastRetryTimestamp(System.currentTimeMillis());

			throw new IOException(e);

		} catch (DbxException e) {
			Logger.printStackTrace(e);

			throw new IOException(e);

		} finally {
//			provider.onFileUploadFinished(link); // TODO: uncomment
		}
	}

	@Override
	public File getOutputFile() {
		return outFile;
	}
}
