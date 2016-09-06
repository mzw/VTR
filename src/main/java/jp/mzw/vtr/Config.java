package jp.mzw.vtr;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class Config {

	Properties config;
	String pathToOutputDir;
	
	public Config(String filename) throws IOException {
		// load
		this.config = new Properties();
		this.config.load(CLI.class.getClassLoader().getResourceAsStream(filename));
		// read
		this.pathToOutputDir = config.getProperty("path_to_output_dir") != null ? config.getProperty("path_to_output_dir") : "output";
	}
	
	/**
	 * Get path to directory where VTR outputs files
	 * @return path to directory
	 */
	public String getPathToOutputDir() {
		return this.pathToOutputDir;
	}

	/**
	 * Get directory where VTR outputs files
	 * @return Directory
	 */
	public File getOutputDir() {
		return new File(this.pathToOutputDir);
	}
	
}
