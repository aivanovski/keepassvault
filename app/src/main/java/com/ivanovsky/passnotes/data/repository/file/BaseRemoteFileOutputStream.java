package com.ivanovsky.passnotes.data.repository.file;

import java.io.File;
import java.io.OutputStream;

public abstract class BaseRemoteFileOutputStream extends OutputStream {

	public abstract File getOutputFile();
}
