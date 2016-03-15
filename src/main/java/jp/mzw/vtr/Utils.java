package jp.mzw.vtr;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {
	static Logger log = LoggerFactory.getLogger(Utils.class);

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

	public static List<String> exec(File dir, List<String> cmds, boolean stdio) throws IOException, InterruptedException {
		if(stdio) return exec(dir, cmds);
		else return exec_stderr(dir, cmds);
	}
	
	public static List<String> exec(File dir, List<String> cmds) throws IOException, InterruptedException {
		Thread timeout = new TimeoutThread(Thread.currentThread());
		timeout.start();
		
		List<String> ret = null;
		Process proc = null;
		try {
			proc = Runtime.getRuntime().exec(cmds.toArray(new String[0]), null, dir);
			try {
				proc.waitFor();
				timeout.interrupt();
			} catch (InterruptedException e) {
				// NOP
			}
		} finally {
			if(proc != null) {
				ret = read(proc, proc.getInputStream());
				List<String> err = read(proc, proc.getErrorStream());
				
				proc.getErrorStream().close();
				proc.getInputStream().close();
				proc.getOutputStream().close();
				proc.destroy();

				if(!err.isEmpty()) {
//					for(String e : err) {
//						System.err.println(e);
//					}
					return null;
				}
				return ret;
			}
		}
		return null;
	}

	public static List<String> exec_stderr(File dir, List<String> cmds) throws IOException, InterruptedException {
		Thread timeout = new TimeoutThread(Thread.currentThread());
		timeout.start();
		
		List<String> ret = null;
		Process proc = null;
		try {
			proc = Runtime.getRuntime().exec(cmds.toArray(new String[0]), null, dir);
			try {
				proc.waitFor();
				timeout.interrupt();
			} catch (InterruptedException e) {
				// NOP
			}
		} finally {
			if(proc != null) {
				List<String> stdio = read(proc, proc.getInputStream());
				ret = read(proc, proc.getErrorStream());
				
				proc.getErrorStream().close();
				proc.getInputStream().close();
				proc.getOutputStream().close();
				proc.destroy();

				if(!stdio.isEmpty()) {
//					for(String line : stdio) {
//						System.err.println(line);
//					}
					return null;
				}
				return ret;
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
	
	public static List<File> getFiles(File dir) {
		ArrayList<File> ret = new ArrayList<File>();
		if(!dir.exists()) return ret;
		for(File file : dir.listFiles()) {
			if(file.isDirectory()) {
				ret.addAll(getFiles(file));
			} else if(file.isFile()) {
				ret.add(file);
			}
		}
		return ret;
	}

	//----------------------------------------------------------------------------------------------------
	public static Properties getConfig(String filename) {
		InputStream is = jp.mzw.vtr.Utils.class.getClassLoader().getResourceAsStream(filename);
		Properties config = new Properties();
		try {
			config.load(is);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return config;
	}
	
	public static String getPathToGit(Properties config) {
		return config.getProperty("path_to_git") != null ? config.getProperty("path_to_git") : "/usr/local/bin/git";
	}

	public static String getPathToMaven(Properties config) {
		return config.getProperty("path_to_maven") != null ? config.getProperty("path_to_maven") : "/usr/local/bin/mvn";
	}
	
	public static String getRefToCompare(Properties config) {
		return config.getProperty("ref_to_compare") != null ? config.getProperty("ref_to_compare") : "refs/heads/master";
	}

	public static String getPathToLogDir(Properties config) {
		return config.getProperty("getPathToLogDir") != null ? config.getProperty("getPathToLogDir") : "log";
	}

	public static File getLogBaseDir(Project project, Properties config) {
		File projectLogDir = new File(getPathToLogDir(config), project.getProjectName());
		if(!projectLogDir.exists() && !projectLogDir.mkdirs()) {
			log.error("Cannot create directory: " + projectLogDir.getAbsolutePath());
		}
		return projectLogDir;
	}
	
}
