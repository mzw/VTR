package jp.mzw.vtr;

import java.util.Properties;

public class VtrBase {

	protected Project project;
	protected Properties config;
	public VtrBase(Project project, Properties config) {
		this.project = project;
		this.config = config;
	}
	
	
}
