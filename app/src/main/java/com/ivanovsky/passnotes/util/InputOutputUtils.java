package com.ivanovsky.passnotes.util;

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

import javax.annotation.Nullable;

public class InputOutputUtils {

    public static FileInputStream newFileInputStreamOrNull(File file) {
        FileInputStream result = null;

        try {
            result = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            Logger.printStackTrace(e);
        }

        return result;
    }

    public static FileOutputStream newFileOutputStreamOrNull(File file) {
        FileOutputStream result = null;

        try {
            result = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            Logger.printStackTrace(e);
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
            Logger.printStackTrace(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Logger.printStackTrace(e);
                }
            }
        }

        return result;
    }

    public static void copy(InputStream in, OutputStream out, boolean close) throws IOException {
        byte[] buf = new byte[1024 * 4];
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
                    Logger.printStackTrace(e);
                }

                try {
                    out.close();
                } catch (IOException e) {
                    Logger.printStackTrace(e);
                }
            }
        }
    }

    public static void copy(InputStream in, OutputStream out, boolean close,
                            AtomicBoolean cancellation) throws IOException {
        byte[] buf = new byte[1024 * 4];
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
            Logger.printStackTrace(e);
        }
    }

    public static void close(@Nullable InputStream in) {
        if (in == null) {
            return;
        }

        try {
            in.close();
        } catch (IOException e) {
            Logger.printStackTrace(e);
        }
    }

    private InputOutputUtils() {
    }
}
