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

	private static List<String> fse17Subjects;
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

	public static List<String> getFse17Subjects() {
		if (fse17Subjects != null) {
			return fse17Subjects;
		} else {
			fse17Subjects = new ArrayList<>();
			fse17Subjects.add("commons-bcel");
			fse17Subjects.add("commons-beanutils");
			fse17Subjects.add("commons-bsf");
			fse17Subjects.add("commons-chain");
			fse17Subjects.add("commons-cli");
			fse17Subjects.add("commons-codec");
			fse17Subjects.add("commons-collections");
			fse17Subjects.add("commons-compress");
			fse17Subjects.add("commons-configuration");
			fse17Subjects.add("commons-crypto");
			fse17Subjects.add("commons-csv");
			fse17Subjects.add("commons-dbcp");
			fse17Subjects.add("commons-dbutils");
			fse17Subjects.add("commons-digester");
			fse17Subjects.add("commons-discovery");
			fse17Subjects.add("commons-email");
			fse17Subjects.add("commons-exec");
			fse17Subjects.add("commons-fileupload");
			fse17Subjects.add("commons-functor");
			fse17Subjects.add("commons-imaging");
			fse17Subjects.add("commons-io");
			fse17Subjects.add("commons-jcs");
			fse17Subjects.add("commons-jexl");
			fse17Subjects.add("commons-jxpath");
			fse17Subjects.add("commons-lang");
			fse17Subjects.add("commons-logging");
			fse17Subjects.add("commons-math");
			fse17Subjects.add("commons-net");
			fse17Subjects.add("commons-pool");
			fse17Subjects.add("commons-proxy");
			fse17Subjects.add("commons-rng");
			fse17Subjects.add("commons-scxml");
			fse17Subjects.add("commons-validator");
		}
		return fse17Subjects;
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
