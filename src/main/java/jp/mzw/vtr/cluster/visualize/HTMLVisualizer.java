package jp.mzw.vtr.cluster.visualize;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HTMLVisualizer extends VisualizerBase {
	static Logger LOGGER = LoggerFactory.getLogger(HTMLVisualizer.class);
	
	public static final String HTML_VISUAL_DIR = "html";
	
	protected File htmlVisualDir;
	
	public HTMLVisualizer(File outputDir) {
		super(outputDir);
		this.htmlVisualDir = new File(this.visualDir, HTML_VISUAL_DIR);
		if (!this.htmlVisualDir.exists()) {
			this.htmlVisualDir.mkdirs();
		}
	}
	
	
}
