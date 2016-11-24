package jp.mzw.vtr.repair;

import java.io.File;

import jp.mzw.vtr.git.Commit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Repair {
	protected static Logger LOGGER = LoggerFactory.getLogger(Repair.class);
	
	private Commit commit;
	private File patch;
	
	private String clazz;
	private String method;
	
	public Repair(Commit commit, File patch) {
		this.commit = commit;
		this.patch = patch;
	}
	
	public Commit getCommit() {
		return this.commit;
	}
	
	public File getPatch() {
		return this.patch;
	}
	
	public Repair setTestCaseNames(String clazz, String method) {
		this.clazz = clazz;
		this.method = method;
		return this;
	}
	
	public String getTestCaseClassName() {
		return this.clazz;
	}
	
	public String getTestCaseMethodName() {
		return this.method;
	}
	
	public File getFile(File projectDir) {
		File src = new File(projectDir, "src/test/java");
		File file = new File(src, this.clazz.replace(".", "/") + ".java");
		return file;
	}
	
}
