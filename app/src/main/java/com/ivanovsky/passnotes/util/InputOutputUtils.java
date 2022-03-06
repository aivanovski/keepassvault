package com.ivanovsky.passnotes.util;

import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import timber.log.Timber;

public class InputOutputUtils {

    private static final int BUFFER_SIZE = 1024 * 8;

    public static FileInputStream newFileInputStreamOrNull(File file) {
        FileInputStream result = null;

        try {
            result = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            Timber.d(e);
        }

        return result;
    }

    public static FileOutputStream newFileOutputStreamOrNull(File file) {
        FileOutputStream result = null;

        try {
            result = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            Timber.d(e);
        }

        return result;
    }

    public static String toString(InputStream is) {
        String result = null;

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            result = sb.toString();
        } catch (IOException e) {
            Timber.d(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Timber.d(e);
                }
            }
        }

        return result;
    }

    public static void copy(InputStream in, OutputStream out, boolean close) throws IOException {
        byte[] buf = new byte[BUFFER_SIZE];
        int len;

        try {
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }

            out.flush();
        } finally {
            if (close) {
                try {
                    in.close();
                } catch (IOException e) {
                    Timber.d(e);
                }

                try {
                    out.close();
                } catch (IOException e) {
                    Timber.d(e);
                }
            }
        }
    }

    public static void copy(InputStream in, OutputStream out, boolean close,
                            AtomicBoolean cancellation) throws IOException {
        byte[] buf = new byte[BUFFER_SIZE];
        int len;

        while ((len = in.read(buf)) > 0 && !cancellation.get()) {
            out.write(buf, 0, len);
        }

        out.flush();

        if (close) {
            in.close();
            out.close();
        }
    }

    public static void close(@Nullable OutputStream out) {
        if (out == null) {
            return;
        }

        try {
            out.close();
        } catch (IOException e) {
            Timber.d(e);
        }
    }

    public static void close(@Nullable InputStream in) {
        if (in == null) {
            return;
        }

        try {
            in.close();
        } catch (IOException e) {
            Timber.d(e);
        }
    }

    private InputOutputUtils() {
    }
}
