package com.ivanovsky.passnotes.data.repository.file;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface FileProvider {

	InputStream createInputStream() throws IOException;
	OutputStream createOutputStream() throws IOException;
}
