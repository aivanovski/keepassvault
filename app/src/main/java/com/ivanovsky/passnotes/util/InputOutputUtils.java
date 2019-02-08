package com.ivanovsky.passnotes.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

public class InputOutputUtils {

	public static String toString(InputStream is) {
		String result = null;

		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(is, "utf-8"));
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

		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}

		if (close) {
			in.close();
			out.close();
		}
	}

	public static void copy(InputStream in, OutputStream out, boolean close, AtomicBoolean cancellation) throws IOException {
		byte[] buf = new byte[1024 * 4];
		int len;

		while ((len = in.read(buf)) > 0 && !cancellation.get()) {
			out.write(buf, 0, len);
		}

		if (close) {
			in.close();
			out.close();
		}
	}

	private InputOutputUtils() {
	}
}
