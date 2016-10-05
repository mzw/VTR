package jp.mzw.vtr.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.mzw.vtr.CLI;

public class Project {
	protected static Logger LOGGER = LoggerFactory.getLogger(Project.class);

	/** Unique string representing subject project */
	protected String projectId;

	/** Path to directory containing subjects */
	protected String pathToSubjectsDir = "subjects";

	/** Path to output directory (default: output) */
	protected String pathToOutputDir = "output";

	/** Maven home (default: $M2_HOME) */
	protected String mavenHome = "/usr/local/apache-maven-3.3.9";

	/** Path to subject project */
	protected String pathToProject;

	/**
	 * Constructor
	 * 
	 * @param projectId
	 */
	public Project(String projectId) {
		this.projectId = projectId;
	}

	/**
	 * Get project ID
	 * 
	 * @return Project ID
	 */
	public String getProjectId() {
		return this.projectId;
	}

	/**
	 * Get project directory
	 * 
	 * @return Project directory
	 */
	public File getProjectDir() {
		return new File(this.pathToSubjectsDir, this.projectId);
	}

	/**
	 * Get directory where VTR outputs files
	 * 
	 * @return Directory
	 */
	public File getOutputDir() {
		return new File(this.pathToOutputDir);
	}

	/**
	 * Get directory where VTR finds subject clones
	 * 
	 * @return Directory
	 */
	public File getSubjectsDir() {
		return new File(this.pathToSubjectsDir);
	}

	/**
	 * Get Maven home
	 * 
	 * @return
	 */
	public File getMavenHome() {
		return new File(this.mavenHome);
	}

	/**
	 * Set configuration according to user environment
	 * 
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	public Project setConfig(String filename) throws IOException {
		// load
		Properties config = new Properties();
		InputStream is = CLI.class.getClassLoader().getResourceAsStream(filename);
		if (is != null) {
			config.load(is);
		}
		// read
		this.pathToOutputDir = config.getProperty("path_to_output_dir") != null ? config.getProperty("path_to_output_dir") : "output";
		this.pathToSubjectsDir = config.getProperty("path_to_subjects_dir") != null ? config.getProperty("path_to_subjects_dir") : "subjects";
		this.mavenHome = config.getProperty("maven_home") != null ? config.getProperty("maven_home") : "/usr/local/apache-maven-3.3.9";
		// return
		return this;
	}

}
