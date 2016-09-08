package jp.mzw.vtr.core;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import jp.mzw.vtr.CLI;

public class Config {

	Properties config;
	String pathToOutputDir;
	String mavenHome;
	
	public Config(String filename) throws IOException {
		// load
		this.config = new Properties();
		this.config.load(CLI.class.getClassLoader().getResourceAsStream(filename));
		// read
		this.pathToOutputDir = config.getProperty("path_to_output_dir") != null ? config.getProperty("path_to_output_dir") : "output";
		this.mavenHome = config.getProperty("maven_home") != null ? config.getProperty("maven_home") : System.getenv("M2_HOME");
	}
	
	public Config(String pathToOutputDir, String mavenHome) {
		this.pathToOutputDir = pathToOutputDir;
		this.mavenHome = mavenHome;
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
	
	/**
	 * Get Maven home
	 * @return
	 */
	public File getMavenHome() {
		return new File(this.mavenHome);
	}
	
}
