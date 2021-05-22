package com.ivanovsky.passnotes.data.repository.file.remote;

import com.ivanovsky.passnotes.data.entity.RemoteFile;
import com.ivanovsky.passnotes.data.entity.RemoteFileMetadata;
import com.ivanovsky.passnotes.data.repository.file.BaseRemoteFileOutputStream;
import com.ivanovsky.passnotes.data.repository.file.remote.exception.RemoteFSException;
import com.ivanovsky.passnotes.data.repository.file.remote.exception.RemoteFSNetworkException;
import com.ivanovsky.passnotes.util.Logger;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class RemoteFileOutputStream extends BaseRemoteFileOutputStream {

    private static final String TAG = RemoteFileOutputStream.class.getSimpleName();

    private boolean failed;
    private final UUID processingUnitUid;
    private final File outFile;
    private final RemoteFileSystemProvider provider;
    private final RemoteApiClient client;
    private final RemoteFile file;

    // should be lazy initialized, because new FileOutputStream make file content empty
    private OutputStream out;

    public RemoteFileOutputStream(RemoteFileSystemProvider provider,
                                  RemoteApiClient client,
                                  RemoteFile file,
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

        RemoteFileMetadata metadata;

        try {
            metadata = client.uploadFileOrThrow(file.getRemotePath(), file.getLocalPath());
        } catch (RemoteFSNetworkException e) {
            Logger.printStackTrace(e);

            provider.onOfflineWriteFinished(file, processingUnitUid);

            throw new IOException(e);

        } catch (RemoteFSException e) {
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
