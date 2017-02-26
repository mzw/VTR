package jp.mzw.vtr.repair;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.validate.ValidatorBase;

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

	abstract public List<Class<? extends ValidatorBase>> includeValidators();

	abstract public String getName();

	public static enum Phase {
		Before, After
	};

	public static enum Type {
		Score, SplitNum, Time, Mem
	};

	public String getFileName(Phase phase, Type type) {
		StringBuilder builder = new StringBuilder();
		switch (phase) {
		case Before:
			builder.append("before_");
			break;
		case After:
			builder.append("after_");
			break;
		}
		switch (type) {
		case Score:
			builder.append("score");
			break;
		case SplitNum:
			builder.append("split_num");
			break;
		case Time:
			builder.append("time");
			break;
		case Mem:
			builder.append("mem");
			break;
		}
		builder.append(".txt");
		return builder.toString();
	}

	public File getRepairDir(Repair repair) {
		File rootDir = EvaluatorBase.getRepairDir(outputDir, projectId);
		File evaluateDir = new File(rootDir, getName());
		File commitDir = new File(evaluateDir, repair.getCommit().getId());
		File validateDir = new File(commitDir, repair.getValidatorName());
		File testDir = new File(validateDir, repair.getTestCaseFullName());
		return testDir;
	}

	public File getFile(Repair repair, Phase phase, Type type) {
		File dir = getRepairDir(repair);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		return new File(dir, getFileName(phase, type));
	}

	/**
	 * 
	 * 
	 * @param repair
	 * @return
	 * @throws IOException
	 */
	public File measure(Repair repair) throws IOException {
		File dstPatchFile = new File(getRepairDir(repair), repair.getPatchFile().getName());
		if (!dstPatchFile.exists()) {
			return dstPatchFile;
		}
		if (repair.isSameContent(dstPatchFile)) {
			LOGGER.info("Patch not changed and already measured: {} at {} by {}", repair.getTestCaseFullName(), repair.getCommit().getId(),
					getClass().getName());
			return null;
		} else {
			return dstPatchFile;
		}
	}

	// TODO Add evaluators from resources
	public static List<EvaluatorBase> getEvaluators(Project project) {
		final List<EvaluatorBase> ret = new ArrayList<>();
		//ret.add(new Readability(project));
		ret.add(new MutationAnalysis(project));
		//ret.add(new Performance(project));
		//ret.add(new RuntimeOutput(project));
		//ret.add(new JavadocOutput(project));
		//ret.add(new CompileOutput(project));
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
