package jp.mzw.vtr.cluster.visualize;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jp.mzw.vtr.dict.Dictionary;
import jp.mzw.vtr.dict.DictionaryMaker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VisualizerBase {
	static Logger LOGGER = LoggerFactory.getLogger(VisualizerBase.class);
	
	public static final String VISUAL_DIR = "visual";

	protected File outputDir;
	protected File visualDir;

	private List<String> projectIdList;
	private Map<String, Dictionary> dicts;
	
	public VisualizerBase(File outputDir) {
		this.outputDir = outputDir;
		this.visualDir = new File(this.outputDir, VISUAL_DIR);
		if (!this.visualDir.exists()) {
			this.visualDir.mkdirs();
		}
		parseDicts();
	}
	
	private void parseDicts() {
		this.projectIdList = new ArrayList<>();
		this.dicts = new HashMap<>();
		for (File projectFile : this.outputDir.listFiles()) {
			File commitsFile = new File(projectFile, DictionaryMaker.FILENAME_COMMITS_XML);
			File dictFile = new File(projectFile, DictionaryMaker.FILENAME_DICT_XML);
			if (commitsFile.exists() && dictFile.exists()) {
				String projectId = projectFile.getName();
				this.projectIdList.add(projectId);
				this.dicts.put(projectId, new Dictionary(this.outputDir, projectId));
			}
		}
	}
	
	public Dictionary getDict(String projectId) {
		return this.dicts.get(projectId);
	}
	
}
