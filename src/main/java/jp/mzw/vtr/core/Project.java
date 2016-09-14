package jp.mzw.vtr.core;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.mzw.vtr.CLI;

public class Project {
	protected static Logger LOGGER = LoggerFactory.getLogger(Project.class);

	/** Unique string representing subject project */
	protected String projectId;

	/** Path to subject project */
	protected String pathToProject;

	/**
	 * Constructor
	 * 
	 * @param projectId
	 *            Unique string representing subject project
	 * @param pathToProject
	 *            Path to subject project
	 */
	public Project(String projectId, String pathToProject) {
		this.projectId = projectId;
		this.pathToOutputDir = pathToProject;
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
	 * Get path to project
	 * 
	 * @return Path to project
	 */
	public String getPathToProject() {
		return this.pathToProject;
	}

	/**
	 * Get project directory
	 * 
	 * @return Project directory
	 */
	public File getProjectDir() {
		return new File(this.pathToProject);
	}

	/** Containing local configuration (e.g., Maven Home) */
	protected Properties config;

	/** Path to output directory (default: output) */
	protected String pathToOutputDir = "output";

	/** Maven home (default: $M2_HOME) */
	protected String mavenHome = System.getenv("M2_HOME");

	public void setConfig(String filename) throws IOException {
		// load
		this.config = new Properties();
		this.config.load(CLI.class.getClassLoader().getResourceAsStream(filename));
		// read
		this.pathToOutputDir = config.getProperty("path_to_output_dir") != null ? config.getProperty("path_to_output_dir") : "output";
		this.mavenHome = config.getProperty("maven_home") != null ? config.getProperty("maven_home") : System.getenv("M2_HOME");
	}

	/**
	 * Get path to directory where VTR outputs files
	 * 
	 * @return path to directory
	 */
	public String getPathToOutputDir() {
		return this.pathToOutputDir;
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
	 * Get Maven home
	 * 
	 * @return
	 */
	public File getMavenHome() {
		return new File(this.mavenHome);
	}

	/** Reference of branch to be compared (default: refs/heads/master) */
	protected String refToCompare = "refs/heads/master";

	/**
	 * Set reference to compare
	 * 
	 * @param refToCompare
	 */
	public void setRefToCompare(String refToCompare) {
		this.refToCompare = refToCompare;
	}

	/**
	 * Get reference to compare
	 * 
	 * @return reference to compare
	 */
	public String getRefToCompare() {
		return this.refToCompare;
	}

}
