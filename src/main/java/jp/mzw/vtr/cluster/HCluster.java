package jp.mzw.vtr.cluster;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jp.mzw.vtr.cluster.similarity.DistAnalyzer;
import jp.mzw.vtr.cluster.similarity.DistMap;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;

import com.apporiented.algorithm.clustering.AverageLinkageStrategy;
import com.apporiented.algorithm.clustering.Cluster;
import com.apporiented.algorithm.clustering.ClusteringAlgorithm;
import com.apporiented.algorithm.clustering.CompleteLinkageStrategy;
import com.apporiented.algorithm.clustering.DefaultClusteringAlgorithm;
import com.apporiented.algorithm.clustering.LinkageStrategy;
import com.apporiented.algorithm.clustering.SingleLinkageStrategy;
import com.apporiented.algorithm.clustering.WeightedLinkageStrategy;

public class HCluster {

	private File outputDir;
	private String methodName;

	private int[] hashcodes;
	private DistMap map;

	private LinkageStrategy strategy = new CompleteLinkageStrategy();
	private double threshold = 0.5;
	private List<Cluster> clusters;

	public HCluster(File outputDir, String methodName) {
		this.outputDir = outputDir;
		this.methodName = methodName;
	}

	public HCluster parse() throws IOException {
		this.hashcodes = this.parseHashcodes();
		this.map = this.parseDist(this.hashcodes);
		return this;
	}

	protected int[] parseHashcodes() throws IOException {
		File simDir = new File(this.outputDir, DistAnalyzer.SIMILARITY_DIR);
		File methodDir = new File(simDir, this.methodName);
		File dir = new File(methodDir, DistAnalyzer.LATEST_DIR);
		File file = new File(dir, DistAnalyzer.HASHCODE_FILENAME);
		String content = FileUtils.readFileToString(file);
		CSVParser parser = CSVParser.parse(content, CSVFormat.DEFAULT);
		List<CSVRecord> records = parser.getRecords();
		int size = records.size();
		int[] hashcodes = new int[size];
		for (int i = 0; i < size; i++) {
			CSVRecord record = records.get(i);
			int hashcode = Integer.parseInt(record.get(0));
			hashcodes[i] = hashcode;
		}
		return hashcodes;
	}

	protected DistMap parseDist(int[] hashcodes) throws IOException {
		DistMap map = new DistMap(hashcodes);
		File simDir = new File(this.outputDir, DistAnalyzer.SIMILARITY_DIR);
		File methodDir = new File(simDir, this.methodName);
		File dir = new File(methodDir, DistAnalyzer.LATEST_DIR);
		File file = new File(dir, DistAnalyzer.DIST_FILENAME);
		String content = FileUtils.readFileToString(file);
		CSVParser parser = CSVParser.parse(content, CSVFormat.DEFAULT);
		List<CSVRecord> records = parser.getRecords();
		int size = records.size();
		for (int i = 0; i < size - 1; i++) { // do not parse first
			CSVRecord record = records.get(i + 1);
			for (int j = 0; j < size - 1; j++) { // do not parse first
				if (i == j) {
					map.add(0.0, i, j);
				} else if (i < j) {
					double value = Double.parseDouble(record.get(j + 1));
					map.add(value, i, j);
					map.add(value, j, i);
				}
			}
		}
		return map;
	}

	/**
	 * Instantiate strategy
	 * 
	 * @param method
	 * @return
	 */
	public static LinkageStrategy getStrategy(String method) {
		if (method == null) {
			return null;
		} else if ("average".equals(method)) {
			return new AverageLinkageStrategy();
		} else if ("complete".equals(method)) {
			return new CompleteLinkageStrategy();
		} else if ("single".equals(method)) {
			return new SingleLinkageStrategy();
		} else if ("weighted".equals(method)) {
			return new WeightedLinkageStrategy();
		}
		return null;
	}

	/**
	 * Get strategy name
	 * 
	 * @param strategy
	 * @return
	 */
	public static String getStrategyName(LinkageStrategy strategy) {
		if (strategy == null) {
			return null;
		} else if (strategy instanceof AverageLinkageStrategy) {
			return "average";
		} else if (strategy instanceof CompleteLinkageStrategy) {
			return "complete";
		} else if (strategy instanceof SingleLinkageStrategy) {
			return "single";
		} else if (strategy instanceof WeightedLinkageStrategy) {
			return "weighted";
		}
		return null;
	}

	/**
	 * 
	 * @param strategy
	 * @param threshold
	 * @return
	 */
	public List<Cluster> cluster(LinkageStrategy strategy, double threshold) {
		this.strategy = strategy;
		this.threshold = threshold;
		ClusteringAlgorithm alg = new DefaultClusteringAlgorithm();
		Cluster cluster = alg.performClustering(this.map.getMap(), this.map.getHashcodesAsNames(), strategy);
		double normalized = cluster.getTotalDistance() * threshold;
		this.clusters = getClusters(cluster, normalized);
		return this.clusters;
	}

	/**
	 * Get clusters whose total distances are lower than given threshold
	 * 
	 * @param cluster
	 * @param threshold
	 * @return
	 */
	private List<Cluster> getClusters(Cluster cluster, double threshold) {
		List<Cluster> ret = new ArrayList<>();
		if (cluster.getTotalDistance() < threshold) {
			ret.add(cluster);
		} else {
			for (Cluster child : cluster.getChildren()) {
				ret.addAll(getClusters(child, threshold));
			}
		}
		return ret;
	}

	private List<Cluster> getLeaves(Cluster cluster) {
		List<Cluster> ret = new ArrayList<>();
		if (cluster.isLeaf()) {
			ret.add(cluster);
		} else {
			for (Cluster child : cluster.getChildren()) {
				ret.addAll(getLeaves(child));
			}
		}
		return ret;
	}

	public void output() throws IOException {
		File dir = this.getOutputDir();
		for (Cluster cluster : this.clusters) {
			File file = this.getOutputFile(dir, cluster);
			String content = this.getOutputContent(cluster);
			FileUtils.writeStringToFile(file, content);
		}
	}

	protected File getOutputDir() {
		File simDir = new File(this.outputDir, DistAnalyzer.SIMILARITY_DIR);
		File methodDir = new File(simDir, this.methodName);
		File latestDir = new File(methodDir, DistAnalyzer.LATEST_DIR);
		File stratedyDir = new File(latestDir, getStrategyName(this.strategy));
		File dir = new File(stratedyDir, new Double(this.threshold).toString());
		if (!dir.exists()) {
			dir.mkdirs();
		}
		return dir;
	}

	protected File getOutputFile(File dir, Cluster cluster) {
		return new File(dir, cluster.getName() + ".csv");
	}

	protected String getOutputContent(Cluster cluster) {
		StringBuilder builder = new StringBuilder();
		List<Cluster> leaves = getLeaves(cluster);
		for (Cluster leaf : leaves) {
			builder.append(leaf.getName()).append("\n");
		}
		return builder.toString();
	}

}
