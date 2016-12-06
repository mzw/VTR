package jp.mzw.vtr.cluster.similarity;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import jp.mzw.vtr.detect.Detector;
import jp.mzw.vtr.detect.TestCaseModification;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract public class DistAnalyzer {
	protected static Logger LOGGER = LoggerFactory.getLogger(DistAnalyzer.class);

	public static final String SIMILARITY_DIR = "similarity";
	public static final String LATEST_DIR = "latest";
	public static final String DIST_FILENAME = "dist.csv";
	public static final String HASHCODE_FILENAME = "hashcode.csv";

	protected File outputDir;
	private List<String> skipProjectIdList;

	public DistAnalyzer(File outputDir) {
		this.outputDir = outputDir;
		this.skipProjectIdList = new ArrayList<>();
	}

	public void setSkipProjectId(List<String> projectIdList) {
		this.skipProjectIdList.addAll(projectIdList);
	}

	public void setSkipProjectId(String projectId) {
		this.skipProjectIdList.add(projectId);
	}

	public List<String> getSkipProjectIdList() {
		return this.skipProjectIdList;
	}

	public static DistAnalyzer analyzerFactory(File outputDir, String methodName) {
		if ("lcs".equals(methodName)) {
			return new LcsAnalyzer(outputDir);
		}
		return null;
	}

	/**
	 * Parse test-case modifications detected
	 * 
	 * @return
	 * @throws IOException
	 */
	public List<TestCaseModification> parseTestCaseModifications() throws IOException {
		List<TestCaseModification> ret = new ArrayList<>();
		for (File outputProjectDir : this.outputDir.listFiles()) {
			if (!outputProjectDir.isDirectory()) {
				continue;
			}
			String projectId = outputProjectDir.getName();
			if (this.getSkipProjectIdList().contains(projectId)) {
				continue;
			}
			File outputDetectDir = new File(outputProjectDir, Detector.DETECT_DIR);
			if (outputDetectDir.exists()) {
				for (File outputCommitDir : outputDetectDir.listFiles()) {
					if (!outputCommitDir.isDirectory()) {
						continue;
					}
					String commitId = outputCommitDir.getName();
					for (File file : outputCommitDir.listFiles()) {
						if (!file.isFile()) {
							continue;
						}
						String fullname = file.getName().replace(".xml", "");
						String[] split = fullname.split("#");
						String clazz = split[0];
						String method = split[1];
						// new and add
						TestCaseModification tcm = new TestCaseModification(file, projectId, commitId, clazz, method);
						ret.add(tcm);
					}
				}
			}
		}
		return ret;
	}

	/**
	 * Analyze similarity distance among test-case modifications
	 * 
	 * @param tcmList
	 * @return
	 * @throws IOException
	 * @throws ParseException
	 * @throws GitAPIException 
	 * @throws NoHeadException 
	 */
	public DistMap analyze(List<TestCaseModification> tcmList) throws IOException, ParseException, NoHeadException, GitAPIException {
		// TODO change methods on demand
		return analyzeByCommitMessages(tcmList);
	}

	/**
	 * Analyze similarity distance among test-case modifications
	 * by using commit messages
	 * 
	 * @param tcmList
	 * @return
	 * @throws IOException
	 * @throws ParseException
	 * @throws GitAPIException 
	 * @throws NoHeadException 
	 */
	public DistMap analyzeByCommitMessages(List<TestCaseModification> tcmList) throws IOException, ParseException, NoHeadException, GitAPIException {
		DistMap map = new DistMap(tcmList);
		for (TestCaseModification tcm : tcmList) {
			tcm.parseCommitMessage();
		}
		for (int i = 0; i < tcmList.size() - 1; i++) {
			TestCaseModification result1 = tcmList.get(i);
			System.out.println(result1.getCommitMessage());
			for (int j = i + 1; j < tcmList.size(); j++) {
				TestCaseModification result2 = tcmList.get(j);
				double sim = sim(result1.getCommitMessage(), result2.getCommitMessage());
				double dist = 1.0 - sim;
				map.add(dist, i, j);
			}
		}
		return map;
	}

	/**
	 * Analyze similarity distance among test-case modifications
	 * by using syntax elements
	 * 
	 * @param tcmList
	 * @return
	 * @throws IOException
	 * @throws ParseException
	 */
	public DistMap analyzeBySyntaxElements(List<TestCaseModification> tcmList) throws IOException, ParseException {
		DistMap map = new DistMap(tcmList);
		for (int i = 0; i < tcmList.size() - 1; i++) {
			TestCaseModification result1 = tcmList.get(i);
			for (int j = i + 1; j < tcmList.size(); j++) {
				TestCaseModification result2 = tcmList.get(j);
				double sim1 = sim(result1.getRevisedNodeClasses(), result2.getRevisedNodeClasses());
				double sim2 = sim(result1.getOriginalNodeClasses(), result2.getOriginalNodeClasses());
				double dist = 1.0 - (sim1 + sim2) / 2.0;
				map.add(dist, i, j);
			}
		}
		return map;
	}

	/**
	 * Output
	 * 
	 * @param map
	 * @return
	 * @throws IOException
	 */
	public String output(DistMap map) throws IOException {
		// root
		File simDir = new File(this.outputDir, SIMILARITY_DIR);
		File methodDir = new File(simDir, getMethodName());
		// with time-stamp
		Calendar c = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		String timestamp = sdf.format(c.getTime());
		File tsDir = new File(methodDir, timestamp);
		if (!tsDir.exists()) {
			tsDir.mkdirs();
		}
		// latest
		File latestDir = new File(methodDir, LATEST_DIR);
		if (latestDir.exists()) {
			latestDir.delete();
		}
		latestDir.mkdirs();
		// output
		// with time-stamp
		FileUtils.writeStringToFile(new File(tsDir, DIST_FILENAME), map.getCsv());
		FileUtils.writeStringToFile(new File(tsDir, HASHCODE_FILENAME), map.getHashcodeCsv());
		// latest
		FileUtils.writeStringToFile(new File(latestDir, DIST_FILENAME), map.getCsv());
		FileUtils.writeStringToFile(new File(latestDir, HASHCODE_FILENAME), map.getHashcodeCsv());
		FileUtils.writeStringToFile(new File(latestDir, "timestamp"), timestamp);
		// return
		return timestamp;
	}

	abstract public String getMethodName();

	abstract protected double sim(List<String> src, List<String> dst);

}
