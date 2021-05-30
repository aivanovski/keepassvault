package com.ivanovsky.passnotes.data.repository.file.remote;

import com.ivanovsky.passnotes.data.entity.RemoteFile;
import com.ivanovsky.passnotes.data.repository.file.BaseRemoteFileOutputStream;
import com.ivanovsky.passnotes.util.Logger;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

public class OfflineFileOutputStream extends BaseRemoteFileOutputStream {

    private boolean failed;
    private boolean flushed;
    private boolean closed;
    private final UUID processingUnitUid;
    private final File outFile;
    private final RemoteFileSystemProvider provider;
    private final OutputStream out;
    private final RemoteFile file;

    public OfflineFileOutputStream(RemoteFileSystemProvider provider,
                                   RemoteFile file,
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
            flushed = false;
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
            flushed = true;
        } catch (IOException e) {
            Logger.printStackTrace(e);

            failed = true;

            provider.onOfflineWriteFailed(file, processingUnitUid);

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

        closed = true;
        provider.onOfflineWriteFinished(file, processingUnitUid);
    }

    @Override
    public File getOutputFile() {
        return outFile;
    }
}
