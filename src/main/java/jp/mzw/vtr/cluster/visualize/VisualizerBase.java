package jp.mzw.vtr.cluster.visualize;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jp.mzw.vtr.CLI;
import jp.mzw.vtr.cluster.HCluster;
import jp.mzw.vtr.cluster.similarity.DistAnalyzer;
import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.dict.Dictionary;
import jp.mzw.vtr.dict.DictionaryMaker;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apporiented.algorithm.clustering.LinkageStrategy;

abstract public class VisualizerBase {
	static Logger LOGGER = LoggerFactory.getLogger(VisualizerBase.class);

	public static final String VISUAL_DIR = "visual";

	protected File outputDir;
	protected File visualDir;

	private List<Project> projects;
	private Map<String, Dictionary> dicts;
	private List<ClusterResult> results;

	public VisualizerBase(File outputDir) throws IOException, ParseException {
		this.outputDir = outputDir;
		this.visualDir = new File(this.outputDir, VISUAL_DIR);
		if (!this.visualDir.exists()) {
			this.visualDir.mkdirs();
		}
		parseDicts();
		this.results = parseClusteringResults();
	}

	abstract public String getContent(ClusterLeaf leaf);

	abstract public String getMethodName();
	
	public static VisualizerBase visualizerFactory(String method, File outputDir) throws IOException, ParseException {
		if ("html".equals(method)) {
			return new HTMLVisualizer(outputDir);
		}
		return null;
	}

	/**
	 * Get dictionary
	 * 
	 * @param projectId
	 * @return
	 */
	public Dictionary getDict(String projectId) {
		return this.dicts.get(projectId);
	}

	/**
	 * Get clustering results
	 * 
	 * @return
	 */
	public List<ClusterResult> getClusterResults() {
		return this.results;
	}

	/**
	 * Parse dictionaries
	 * 
	 * @throws IOException
	 * @throws ParseException
	 */
	private void parseDicts() throws IOException, ParseException {
		this.projects = new ArrayList<>();
		this.dicts = new HashMap<>();
		for (File projectFile : this.outputDir.listFiles()) {
			File commitsFile = new File(projectFile, DictionaryMaker.FILENAME_COMMITS_XML);
			File dictFile = new File(projectFile, DictionaryMaker.FILENAME_DICT_XML);
			if (commitsFile.exists() && dictFile.exists()) {
				String projectId = projectFile.getName();
				this.projects.add(new Project(projectId).setConfig(CLI.CONFIG_FILENAME));
				this.dicts.put(projectId, new Dictionary(this.outputDir, projectId).parse());
			}
		}
	}

	/**
	 * Parse clustering results TODO Need to refactoring due to complexity
	 * 
	 * @return
	 * @throws IOException
	 */
	private List<ClusterResult> parseClusteringResults() throws IOException {
		List<ClusterResult> results = new ArrayList<>();
		for (File simDir : this.outputDir.listFiles()) {
			if (simDir.isDirectory() && DistAnalyzer.SIMILARITY_DIR.equals(simDir.getName())) {
				for (File distAnalyzerDir : simDir.listFiles()) {
					DistAnalyzer distAnalyzer = DistAnalyzer.analyzerFactory(this.outputDir, distAnalyzerDir.getName());
					if (distAnalyzer != null) {
						File latestDir = new File(distAnalyzerDir, DistAnalyzer.LATEST_DIR);
						File file = new File(latestDir, DistAnalyzer.HASHCODE_FILENAME);
						List<ClusterLeaf> clusterLeaves = parseClusterLeaves(file);
						for (File strategyDir : latestDir.listFiles()) {
							LinkageStrategy linkageStrategy = HCluster.getStrategy(strategyDir.getName());
							if (linkageStrategy != null) {
								for (File thresholdDir : strategyDir.listFiles()) {
									Double threshold = Double.valueOf(thresholdDir.getName());
									if (threshold != null) {
										ClusterResult result = new ClusterResult(distAnalyzer, linkageStrategy, threshold);
										result.setClusterLeaves(clusterLeaves);
										for (File clusterFile : thresholdDir.listFiles()) {
											List<Integer> hashcodes = parseHashcodes(clusterFile);
											result.addCluster(hashcodes);
										}
										results.add(result);
									}
								}
							}
						}
					}
				}
			}
		}
		return results;
	}

	/**
	 * Parse each clustering result
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 */
	protected List<ClusterLeaf> parseClusterLeaves(File file) throws IOException {
		String content = FileUtils.readFileToString(file);
		CSVParser parser = CSVParser.parse(content, CSVFormat.DEFAULT);
		List<CSVRecord> records = parser.getRecords();
		int size = records.size();
		List<ClusterLeaf> ret = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			CSVRecord record = records.get(i);
			int hashcode = Integer.parseInt(record.get(0));
			String projectId = record.get(1);
			String commitId = record.get(2);
			String className = record.get(3);
			String methodName = record.get(4);
			ret.add(new ClusterLeaf(hashcode, projectId, commitId, className, methodName));
		}
		return ret;
	}

	/**
	 * Parse hashcodes corresponding to clustering results
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 */
	protected List<Integer> parseHashcodes(File file) throws IOException {
		List<String> lines = FileUtils.readLines(file);
		List<Integer> hashcodes = new ArrayList<>();
		for (String line : lines) {
			int hashcode = Integer.parseInt(line);
			hashcodes.add(hashcode);
		}
		return hashcodes;
	}

	/**
	 * Output visualized results
	 * 
	 * @throws IOException
	 */
	public void visualize() throws IOException {
		File visualDir = new File(this.outputDir, VISUAL_DIR);
		File methodDir = new File(visualDir, this.getMethodName());

		for (ClusterResult result : this.getClusterResults()) {
			// Directory
			File distDir = new File(methodDir, result.getDistAnalyzer().getMethodName());
			File strategyDir = new File(distDir, HCluster.getStrategyName(result.getLinkageStrategy()));
			File thresholdDir = new File(strategyDir, new Double(result.getThreshold()).toString());
			// Parse
			List<List<ClusterLeaf>> clusters = result.getClusters();
			int size = String.valueOf(clusters.size()).length();
			int num = 0;
			for (List<ClusterLeaf> leaves : clusters) {
				// Directory
				String clusterId = String.format("%0" + size + "d", num++);
				File dir = new File(thresholdDir, clusterId);
				if (!dir.exists()) {
					dir.mkdirs();
				}
				// Output
				for (ClusterLeaf leaf : leaves) {
					String filename = leaf.getProjectId() + ":" + leaf.getCommitId() + ":" + leaf.getClassName() + ":" + leaf.getMethodName() + "."
							+ this.getMethodName();
					File file = new File(dir, filename);
					String content = getContent(leaf);
					FileUtils.writeStringToFile(file, content);
				}
			}
		}
	}
}
