package jp.mzw.vtr.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

public class VtrUtils {

	/**
	 * Make Collection corresponding to given Iterable
	 * 
	 * @param iter
	 *            Given Iterable
	 * @return Corresponding Collection
	 */
	public static <E> Collection<E> makeCollection(Iterable<E> iter) {
		Collection<E> list = new ArrayList<>();
		for (E item : iter) {
			list.add(item);
		}
		return list;
	}

	/**
	 * Get files under given directory recursively
	 * 
	 * @param dir
	 *            Given Directory
	 * @return File found
	 */
	public static List<File> getFiles(File dir) {
		ArrayList<File> ret = new ArrayList<File>();
		if (!dir.exists()) {
			return ret;
		}
		for (File file : dir.listFiles()) {
			if (file.isDirectory()) {
				ret.addAll(getFiles(file));
			} else if (file.isFile()) {
				ret.add(file);
			}
		}
		return ret;
	}

	/**
	 * Get relative path from subject directory to target file
	 * 
	 * @param subject
	 *            Git root directory
	 * @param target
	 *            File
	 * @return relative path from Git root directory to target file
	 */
	public static String getFilePath(File subject, File target) {
		return subject.toURI().relativize(target.toURI()).toString();
	}
	
	/**
	 * 
	 * @param dir
	 * @param cmd
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static Pair<List<String>, List<String>> exec(File dir, List<String> cmd) throws IOException, InterruptedException {
		Thread timeout = new TimeoutThread(Thread.currentThread());
		timeout.start();
		Process proc = null;
		try {
			proc = Runtime.getRuntime().exec(cmd.toArray(new String[0]), null, dir);
			proc.waitFor();
			timeout.interrupt();
		} finally {
			if(proc != null) {
				List<String> stdio = read(proc, proc.getInputStream());
				List<String> err = read(proc, proc.getErrorStream());
				proc.getErrorStream().close();
				proc.getInputStream().close();
				proc.getOutputStream().close();
				proc.destroy();
				return Pair.of(stdio, err);
			}
		}
		return null;
	}
	
	public static List<String> read(Process process, InputStream is) throws IOException {
		ArrayList<String> ret = new ArrayList<String>();
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line = br.readLine();
		while(line != null) {
			ret.add(line);
			line = br.readLine();
		}
		br.close();
		return ret;
	}
	
	public static final long Timeout = 3000;
	
	public static class TimeoutThread extends Thread {
		private Thread mParent;
		public TimeoutThread(Thread parent) {
			mParent = parent;
		}
		public void run() {
			try {
				Thread.sleep(Timeout);
				mParent.interrupt();
			} catch (InterruptedException e) {
				// NOP
			}
		}
	}
}
