package jp.mzw.vtr.cluster.visualize;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VisualizerBase {
	static Logger LOGGER = LoggerFactory.getLogger(VisualizerBase.class);
	
	public static final String VISUAL_DIR = "visual";

	protected File outputDir;
	protected File visualDir;
	
	public VisualizerBase(File outputDir) {
		this.outputDir = outputDir;
		this.visualDir = new File(this.outputDir, VISUAL_DIR);
		if (!this.visualDir.exists()) {
			this.visualDir.mkdirs();
		}
	}
	
}
