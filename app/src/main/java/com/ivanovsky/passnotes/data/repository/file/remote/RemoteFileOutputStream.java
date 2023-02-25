package com.ivanovsky.passnotes.data.repository.file.remote;

import com.ivanovsky.passnotes.data.entity.RemoteFile;
import com.ivanovsky.passnotes.data.entity.RemoteFileMetadata;
import com.ivanovsky.passnotes.data.repository.file.BaseRemoteFileOutputStream;
import com.ivanovsky.passnotes.data.repository.file.remote.exception.RemoteFSException;
import com.ivanovsky.passnotes.data.repository.file.remote.exception.RemoteFSNetworkException;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;
import timber.log.Timber;

public class RemoteFileOutputStream extends BaseRemoteFileOutputStream {

    private static final String TAG = RemoteFileOutputStream.class.getSimpleName();

    private boolean failed;
    private boolean flushed;
    private boolean closed;
    private final UUID processingUnitUid;
    private final File outFile;
    private final RemoteFileSystemProvider provider;
    private final RemoteApiClient client;
    private final RemoteFile file;

    // should be lazy initialized, because new FileOutputStream make file content empty
    private OutputStream out;

    public RemoteFileOutputStream(
            RemoteFileSystemProvider provider,
            RemoteApiClient client,
            RemoteFile file,
            UUID processingUnitUid)
            throws FileNotFoundException {
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
            flushed = false;
        } catch (IOException e) {
            Timber.d(e);

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
            flushed = true;
        } catch (IOException e) {
            Timber.d(e);

            failed = true;

            provider.onFileUploadFailed(file, processingUnitUid);

            throw new IOException(e);
        }
    }

    @Override
    public void close() throws IOException {
        if (failed || closed) {
            return;
        }

        if (!flushed) {
            flush();
        }

        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                failed = true;

                provider.onFileUploadFailed(file, processingUnitUid);

                throw new IOException(e);
            }
        }

        RemoteFileMetadata metadata;

        try {
            metadata = client.uploadFileOrThrow(file.getRemotePath(), file.getLocalPath());
            closed = true;
        } catch (RemoteFSNetworkException e) {
            Timber.d(e);

            provider.onOfflineWriteFinished(file, processingUnitUid);

            throw new IOException(e);

        } catch (RemoteFSException e) {
            Timber.d(e);

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
