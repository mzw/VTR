package jp.mzw.vtr.repair;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.shared.invoker.MavenInvocationException;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.core.VtrUtils;
import jp.mzw.vtr.maven.MavenUtils;
import jp.mzw.vtr.validate.ValidatorBase;
import jp.mzw.vtr.validate.coding_style.UseProcessWaitfor;
import jp.mzw.vtr.validate.resources.CloseResources;
import jp.mzw.vtr.validate.resources.UseTryWithResources;

public class Performance extends EvaluatorBase {

	public Performance(Project project) {
		super(project);
	}
	
	@Override
	public String getName() {
		return "performance";
	}

	@Override
	public List<Class<? extends ValidatorBase>> includeValidators() {
		final List<Class<? extends ValidatorBase>> includes = new ArrayList<>();

		/*
		 * Performance
		 */
		// Resources
		includes.add(UseProcessWaitfor.class);
		includes.add(CloseResources.class);
		includes.add(UseTryWithResources.class);

		return includes;
	}

	@Override
	public void evaluateBefore(Repair repair) {
		try {
			LOGGER.info("Start: Performance (Before): {}, {} ", repair.getValidatorName(), repair.getTestCaseFullName());
			File dstPatchFile = measure(repair);
			if (dstPatchFile == null) {
				return;
			}

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

			long time = endTime - startTime;
			long mem = endMem - startMem;

			FileUtils.writeStringToFile(getFile(repair, Phase.Before, Type.Time), Long.toString(time));
			FileUtils.writeStringToFile(getFile(repair, Phase.Before, Type.Mem), Long.toString(mem));
			LOGGER.info("End: Performance (Before): {}, {} ", repair.getValidatorName(), repair.getTestCaseFullName());
		} catch (MavenInvocationException | IOException | InterruptedException e) {
			LOGGER.warn("Failed to evaluate before: {} at {} with {}", repair.getTestCaseFullName(), repair.getCommit().getId(), this.getClass().getName());
		}
	}

	@Override
	public void evaluateAfter(Repair repair) {
		try {
			LOGGER.info("Start: Performance (After): {}, {} ", repair.getValidatorName(), repair.getTestCaseFullName());
			File dstPatchFile = measure(repair);
			if (dstPatchFile == null) {
				return;
			}
			// start

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

			long time = endTime - startTime;
			long mem = endMem - startMem;

			FileUtils.writeStringToFile(getFile(repair, Phase.After, Type.Time), Long.toString(time));
			FileUtils.writeStringToFile(getFile(repair, Phase.After, Type.Mem), Long.toString(mem));
			
			// end
			FileUtils.copyFile(repair.getPatchFile(), dstPatchFile);
			LOGGER.info("End: Performance (After): {}, {} ", repair.getValidatorName(), repair.getTestCaseFullName());
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
		try {
			Result before = Result.parse(this, repair, Phase.Before);
			Result after = Result.parse(this, repair, Phase.After);
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
		} catch (NumberFormatException | IOException e) {
			repair.setStatus(this, Repair.Status.Broken);
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
			if (!RepairEvaluator.include(this, repair)) {
				continue;
			}
			// common
			builder.append(repair.toCsv(this)).append(",");
			// specific
			Result before = Result.parse(this, repair, Phase.Before);
			builder.append(before == null ? "" : before.getElapsedTime()).append(",");
			builder.append(before == null ? "" : before.getUsedMemory()).append(",");
			if (repair.getStatus(this).equals(Repair.Status.Broken)) {
				builder.append(-1).append(",");
				builder.append(-1);
			} else {
				Result after = Result.parse(this, repair, Phase.After);
				builder.append(after == null ? "" : after.getElapsedTime()).append(",");
				builder.append(after == null ? "" : after.getUsedMemory());
			}
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

		public static Result parse(Performance performence, Repair repair, Phase phase) throws NumberFormatException, IOException {
			long time = Long.parseLong(FileUtils.readFileToString(performence.getFile(repair, phase, Type.Time)));
			long mem = Long.parseLong(FileUtils.readFileToString(performence.getFile(repair, phase, Type.Mem)));
			return new Result(time, mem);
		}
	}
}
