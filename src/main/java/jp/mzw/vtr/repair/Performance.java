package jp.mzw.vtr.repair;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.shared.invoker.MavenInvocationException;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.core.VtrUtils;
import jp.mzw.vtr.maven.MavenUtils;

public class Performance extends EvaluatorBase {

	protected Map<Repair, Result> beforeResults;
	protected Map<Repair, Result> afterResults;

	public Performance(Project project) {
		super(project);
		beforeResults = new HashMap<>();
		afterResults = new HashMap<>();
	}

	@Override
	public void evaluateBefore(Repair repair) {
		try {
			int compile = MavenUtils.maven(this.projectDir, Arrays.asList("compile", "test-compile"), mavenHome, mavenOutput);
			if (compile != 0) {
				LOGGER.warn("Failed to compile before: {} at {}", repair.getTestCaseFullName(), repair.getCommit().getId());
				return;
			}
			System.gc();
			Runtime rt = Runtime.getRuntime();
			long startTime = System.currentTimeMillis();
			long startMem = (rt.totalMemory() - rt.freeMemory());
			runJunitTestSuite(projectDir, mavenHome, repair.getTestCaseClassName());
			long endMem = (rt.totalMemory() - rt.freeMemory());
			long endTime = System.currentTimeMillis();
			beforeResults.put(repair, new Result(endTime - startTime, endMem - startMem));
		} catch (MavenInvocationException | IOException | InterruptedException e) {
			LOGGER.warn("Failed to evaluate before: {} at {} with {}", repair.getTestCaseFullName(), repair.getCommit().getId(), this.getClass().getName());
		}
	}

	@Override
	public void evaluateAfter(Repair repair) {
		if (beforeResults.get(repair) == null) {
			return;
		}
		try {
			int compile = MavenUtils.maven(this.projectDir, Arrays.asList("test-compile"), mavenHome, mavenOutput);
			if (compile != 0) {
				LOGGER.warn("Failed to compile after: {} at {}", repair.getTestCaseFullName(), repair.getCommit().getId());
				return;
			}
			System.gc();
			Runtime rt = Runtime.getRuntime();
			long startTime = System.currentTimeMillis();
			long startMem = (rt.totalMemory() - rt.freeMemory());
			runJunitTestSuite(projectDir, mavenHome, repair.getTestCaseClassName());
			long endMem = (rt.totalMemory() - rt.freeMemory());
			long endTime = System.currentTimeMillis();
			afterResults.put(repair, new Result(endTime - startTime, endMem - startMem));
		} catch (MavenInvocationException | IOException | InterruptedException e) {
			LOGGER.warn("Failed to evaluate after: {} at {} with {}", repair.getTestCaseFullName(), repair.getCommit().getId(), this.getClass().getName());
		}
	}

	public static List<String> runJunitTestSuite(File projectDir, File mavenHome, String className)
			throws MavenInvocationException, IOException, InterruptedException {
		String classpath = MavenUtils.getBuildClassPath(projectDir, mavenHome);
		if (classpath == null) {
			LOGGER.warn("Failed to '$ mvn dependency:build-classpath'");
			return null;
		}
		List<String> cmd = Arrays.asList("java", "-cp", "target/classes:target/test-classes:" + classpath, className);
		Pair<List<String>, List<String>> results = VtrUtils.exec(projectDir, cmd);
		return results.getRight();
	}

	@Override
	public void compare(Repair repair) {
		Result before = beforeResults.get(repair);
		Result after = afterResults.get(repair);
		if (before == null || after == null) {
			repair.setStatus(this, Repair.Status.Broken);
		} else if (before.getElapsedTime() < 0 || after.getElapsedTime() < 0 || before.getUsedMemory() < 0 || after.getUsedMemory() < 0) {
			repair.setStatus(this, Repair.Status.Broken);
		} else if (before.getElapsedTime() > after.getElapsedTime() && before.getUsedMemory() > after.getUsedMemory()) {
			repair.setStatus(this, Repair.Status.Improved);
		} else if (before.getElapsedTime() > after.getElapsedTime() && before.getUsedMemory() >= after.getUsedMemory()) {
			repair.setStatus(this, Repair.Status.Improved);
		} else if (before.getElapsedTime() >= after.getElapsedTime() && before.getUsedMemory() > after.getUsedMemory()) {
			repair.setStatus(this, Repair.Status.Improved);
		} else if (before.getElapsedTime() == after.getElapsedTime() && before.getUsedMemory() == after.getUsedMemory()) {
			repair.setStatus(this, Repair.Status.Stay);
		} else {
			repair.setStatus(this, Repair.Status.Degraded);
		}
	}

	@Override
	public void output(List<Repair> repairs) throws IOException {
		File rootDir = EvaluatorBase.getRepairDir(outputDir, projectId);
		File dir = new File(rootDir, "performance");
		if (!repairs.isEmpty() && !dir.exists()) {
			dir.mkdirs();
		}
		// csv
		StringBuilder builder = new StringBuilder();
		// header
		// common
		builder.append(EvaluatorBase.getCommonCsvHeader());
		// specific
		builder.append(",");
		builder.append("Before elapsed time").append(",");
		builder.append("Before used memory").append(",");
		builder.append("After elapsed time").append(",");
		builder.append("After used memory");
		// end
		builder.append("\n");
		// content
		for (Repair repair : repairs) {
			// common
			builder.append(repair.toCsv(this)).append(",");
			// specific
			Result before = beforeResults.get(repair);
			Result after = afterResults.get(repair);
			builder.append(before == null ? "" : before.getElapsedTime()).append(",");
			builder.append(before == null ? "" : before.getUsedMemory()).append(",");
			builder.append(after == null ? "" : after.getElapsedTime()).append(",");
			builder.append(after == null ? "" : after.getUsedMemory());
			// end
			builder.append("\n");
		}
		// write
		File csv = new File(dir, EvaluatorBase.REPAIR_FILENAME);
		FileUtils.write(csv, builder.toString());
	}

	private static class Result {
		long time;
		long mem;

		private Result(long time, long mem) {
			this.time = time;
			this.mem = mem;
		}

		public long getElapsedTime() {
			return time;
		}

		public long getUsedMemory() {
			return mem;
		}
	}
}
