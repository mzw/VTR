package jp.mzw.vtr.cluster;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.rosuda.JRI.Rengine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.mzw.vtr.core.VTRUtils;

public class LCSAnalyzer extends ClusterBase {
	protected static Logger log = LoggerFactory.getLogger(LCSAnalyzer.class);
	
	
	
	public static void main(String[] args) throws NoSuchAlgorithmException, IOException {
		InputStream is = LCSAnalyzer.class.getClassLoader().getResourceAsStream("vtr.properties");
		Properties config = new Properties();
		config.load(is);
		new LCSAnalyzer(config).analyze().saveClusteringResults();
	}
	
	public LCSAnalyzer(Properties config) throws IOException {
		super(config);
	}
	
	public LCSAnalyzer analyze() throws IOException, NoSuchAlgorithmException {
		log.info("Start to analyze LCS-based similarity");
		
		ArrayList<File> files = new ArrayList<>();
		for(File file : VTRUtils.getFiles(new File(log_dir))) {
			if(isHtmlReportFile(file)) {
				List<String> prev_lines = read(file, ".prev");
				if(prev_lines.size() == 0) {
					log.warn("Not found test modification but addition" + file.getAbsolutePath());
					continue;
				}
				files.add(file);
			}
		}
		
		StringBuilder builder = new StringBuilder();
		String delim = "";
		HashMap<String, File> hashIdFile = new HashMap<>();
		for(File file : files) {
	        String id = genShortId(file);			
			builder.append(delim).append(id);
			delim = ",";
			hashIdFile.put(id, file);
		}
		builder.append("\n");
		saveTestIdRelationships(hashIdFile);
		
		for(int i = 0; i < files.size(); i++) {
			File file = files.get(i);
			delim = "";
			for(int j = 0; j <= i; j++) {
				File _file = files.get(j);

				log.info("Measuring LCS between: " + file.getName() + " and " + _file.getName());
				LCSResult result = measureLcs(file, _file);
				
				if(result.getCurrSimilarity() == -1) {
					log.warn("Not found test modification: [" + file.getAbsolutePath() + "] and [" + _file.getAbsolutePath() + "]");
					continue;
				}
				if(result.getPrevSimilarity() == -1) {
					log.warn("Not found test modification but addition: [" + file.getAbsolutePath() + "] and [" + _file.getAbsolutePath() + "]");
					continue;
				}

				double dist = 1 - (result.getCurrSimilarity() + result.getPrevSimilarity()) / 2;
						
				builder.append(delim).append(dist);
				delim = ",";
			}
			for(int j = i+1; j < files.size(); j++) {
				builder.append(",");
			}
			builder.append("\n");
		}
		
		File output = getDistFile();
		FileUtils.write(output, builder);
		log.info("Save LCS-based similarity-based distance: " + output.getAbsolutePath());
		
		return this;
	}
	
	private void saveTestIdRelationships(HashMap<String, File> hashIdFile) throws IOException {
		StringBuilder builder = new StringBuilder();
		for(String id : hashIdFile.keySet()) {
			File file = hashIdFile.get(id);
			builder.append(id).append(",").append(file.getAbsolutePath()).append("\n");
		}
		File output = getTestIdRelateFile();
		FileUtils.write(output, builder);
		log.info("Save test-id relationships: " + output.getAbsolutePath());
	}

	public void saveClusteringResults() {
		log.info("Analyzing hierarchical clustering: (method) " + getClusteringMethod());
		
		File input_file = getDistFile();
		File output_dir = getClusterResultDir();
		
		Rengine engine = new Rengine(new String[]{"--no-save"}, false, null);
		engine.assign("input.file", input_file.getAbsolutePath());
		engine.assign("output.dir", output_dir.getAbsolutePath()+"/");
		engine.assign("clustering.method", getClusteringMethod());
		engine.eval("data <- read.csv(input.file)");
		engine.eval("clust <- hclust(as.dist(data), method=clustering.method)");
		
		int length = engine.eval("length(data)").asInt();
		for(int i = 1; i <= length; i++) {
			engine.assign("i", new int[]{i});
			engine.eval("cu.result <- cutree(clust,i)");
			engine.eval("filename <- paste(output.dir,i,sep=\"\")");
			engine.eval("write.table(cu.result, file=filename, quote=F, col.names=F)");
		}
		engine.end();
		
		log.info("Enter for R cluster-dendrogram: " + "plot(hclust(as.dist(read.csv(\"" + input_file.getAbsolutePath() + "\")), method=\"" + getClusteringMethod() + "\"))");
	}
	
	private LCSResult measureLcs(File src, File dst) throws IOException {
		
		List<String> src_curr_lines = read(src, ".curr");
		List<String> src_prev_lines = read(src, ".prev");
		
		List<String> dst_curr_lines = read(dst, ".curr");
		List<String> dst_prev_lines = read(dst, ".prev");
		
		List<String> curr_lcs = lcs(src_curr_lines, dst_curr_lines);
		List<String> prev_lcs = lcs(src_prev_lines, dst_prev_lines);
		
		return new LCSResult(src, dst).setCurrData(src_curr_lines, dst_curr_lines, curr_lcs).setPrevData(src_prev_lines, dst_prev_lines, prev_lcs);
	}
	
	private List<String> lcs(List<String> src, List<String> dst) {
		int M = src.size();
		int N = dst.size();
		int[][] opt = new int[M+1][N+1];
		
		for(int i = M-1; i >= 0; i--) {
			for(int j = N-1; j >= 0; j--) {
				if(src.get(i).equals(dst.get(j))) {
					opt[i][j] = opt[i+1][j+1] + 1;
				} else {
					opt[i][j] = Math.max(opt[i+1][j], opt[i][j+1]);
				}
			}
		}
		
		ArrayList<String> ret = new ArrayList<>();
		
		int i = 0, j = 0;
		while(i < M && j < N) {
			if(src.get(i).equals(dst.get(j))) {
				ret.add(src.get(i));
				i++;
				j++;
			} else if(opt[i+1][j] >= opt[i][j+1]) {
				i++;
			} else {
				j++;
			}
		}
		
		return ret;
	}
	
}
