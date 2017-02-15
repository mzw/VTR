package jp.mzw.vtr.repair;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.mzw.vtr.core.Project;

abstract public class EvaluatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(EvaluatorBase.class);

	protected String projectId;
	protected File projectDir;
	protected File outputDir;
	protected File mavenHome;
	protected boolean mavenOutput;
	
	public EvaluatorBase(Project project) {
		projectId = project.getProjectId();
		projectDir = project.getProjectDir();
		outputDir = project.getOutputDir();
		mavenHome = project.getMavenHome();
		mavenOutput = project.getMavenOutput();
	}

	abstract public void evaluateBefore(Repair repair);
	abstract public void evaluateAfter(Repair repair);
	abstract public void compare(Repair repair);
	abstract public void output(List<Repair> repairs) throws IOException;
	
	// TODO Add evaluators from resources
	public static List<EvaluatorBase> getEvaluators(Project project) {
		final List<EvaluatorBase> ret = new ArrayList<>();
//		ret.add(new Readability(project));
		ret.add(new MutationAnalysis(project));
		return ret;
	}

	public static final String REPAIR_DIRNAME = "repair";
	public static final String REPAIR_FILENAME = "results.csv";
	
	public static File getRepairDir(File outputDir, String projectId) {
		File projectDir = new File(outputDir, projectId);
		File repairDir = new File(projectDir, REPAIR_DIRNAME);
		return repairDir;
	}

	public static String toString(List<String> lines) {
		StringBuilder builder = new StringBuilder();
		for (String line : lines) {
			builder.append(line).append("\n");
		}
		return builder.toString();
	}

	public static String toString(List<String> lines, int start, int end) {
		StringBuilder builder = new StringBuilder();
		for (int i = 1; i <= lines.size(); i++) {
			String line = lines.get(i - 1);
			if (start <= i && i <= end) {
				builder.append(line).append("\n");
			}
		}
		return builder.toString();
	}
	
	public static String getCommonCsvHeader() {
		StringBuilder builder = new StringBuilder();
		builder.append("Commit").append(",");
		builder.append("Validator").append(",");
		builder.append("Class").append(",");
		builder.append("Method").append(",");
		builder.append("Result");
		return builder.toString();
	}
}
