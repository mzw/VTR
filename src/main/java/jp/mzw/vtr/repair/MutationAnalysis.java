package jp.mzw.vtr.repair;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.dom4j.DocumentException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.maven.MavenUtils;
import jp.mzw.vtr.maven.PitInstrumenter;
import jp.mzw.vtr.validate.ValidatorBase;
import jp.mzw.vtr.validate.exception_handling.AddFailStatementsForHandlingExpectedExceptions;
import jp.mzw.vtr.validate.exception_handling.DoNotSwallowTestErrorsSilently;
import jp.mzw.vtr.validate.junit.AssertNotNullToInstances;

public class MutationAnalysis extends EvaluatorBase {
	protected static Logger LOGGER = LoggerFactory.getLogger(MutationAnalysis.class);

	String classesUnderTest;

	Map<Repair, Result> results;

	/**
	 * @ @param
	 *       project
	 */
	public MutationAnalysis(Project project) {
		super(project);
		this.classesUnderTest = PitInstrumenter.getTargetClasses(projectDir);
		results = new HashMap<>();
	}
	
	@Override
	public String getName() {
		return "mutation";
	}

	@Override
	public List<Class<? extends ValidatorBase>> includeValidators() {
		final List<Class<? extends ValidatorBase>> includes = new ArrayList<>();

		/*
		 * Mutation analysis
		 */
		// JUnit
		includes.add(AssertNotNullToInstances.class); // +SuppressWarnings
		// Exception handling
		includes.add(AddFailStatementsForHandlingExpectedExceptions.class);
		includes.add(DoNotSwallowTestErrorsSilently.class);

		return includes;
	}

	@Override
	public void evaluateBefore(Repair repair) {
		try {
			PitInstrumenter pi = new PitInstrumenter(projectDir, classesUnderTest, repair.getTestCaseClassName());
			boolean modified = pi.instrument();

			File dir = getBeforeDir(repair);
			if (dir.exists()) {
				LOGGER.info("Already invoked PIT for original version");
			} else {
				LOGGER.info("Invoke PIT for original version");
				int compile = MavenUtils.maven(this.projectDir, Arrays.asList("compile", "test-compile"), mavenHome, mavenOutput);
				if (compile != 0) {
					LOGGER.warn("Failed to compile: {} at {}", repair.getTestCaseFullName(), repair.getCommit().getId());
					if (modified) {
						pi.revert();
					}
					return;
				}
				MavenUtils.maven(this.projectDir, Arrays.asList("org.pitest:pitest-maven:mutationCoverage"), mavenHome, mavenOutput);
				for (File resultDir : PitInstrumenter.getPitResultsDir(this.projectDir)) {
					if (!dir.exists()) {
						dir.mkdirs();
					}
					org.codehaus.plexus.util.FileUtils.copyDirectoryStructure(resultDir, dir);
					org.codehaus.plexus.util.FileUtils.deleteDirectory(resultDir);
					LOGGER.info("Found PIT results: {}", resultDir.getPath());
				}
			}
			if (modified) {
				pi.revert();
			}
		} catch (IOException | MavenInvocationException e) {
			LOGGER.warn("Failed to invoke PIT mutation testing: {} at {}", repair.getTestCaseClassName(), repair.getCommit().getId());
		}
	}

	@Override
	public void evaluateAfter(Repair repair) {
		if (!getBeforeDir(repair).exists()) {
			LOGGER.warn("Original failed to compile: {} at {}", repair.getTestCaseFullName(), repair.getCommit().getId());
			return;
		}
		try {
			PitInstrumenter pi = new PitInstrumenter(projectDir, classesUnderTest, repair.getTestCaseClassName());
			boolean modified = pi.instrument();
			File dstPatchFile = new File(getAfterDir(repair), repair.getPatchFile().getName());
			if (dstPatchFile.exists()) {
				if (repair.isSameContent(dstPatchFile)) {
					LOGGER.info("Patch not changed and already measured: {} at {} by {}", repair.getTestCaseFullName(), repair.getCommit().getId(),
							this.getClass().getName());
					if (modified) {
						pi.revert();
					}
					return;
				}
			}
			LOGGER.info("Invoke PIT for applied version");
			int compile = MavenUtils.maven(this.projectDir, Arrays.asList("compile", "test-compile"), mavenHome, mavenOutput);
			if (compile != 0) {
				LOGGER.warn("Failed to compile: {} at {}", repair.getTestCaseFullName(), repair.getCommit().getId());
				if (modified) {
					pi.revert();
				}
				return;
			}
			int pit = MavenUtils.maven(this.projectDir, Arrays.asList("org.pitest:pitest-maven:mutationCoverage"), mavenHome, mavenOutput);
			if (pit == MavenUtils.FAIL_TEST_WITHOUT_MUTATION) {
				FileUtils.writeStringToFile(new File(getAfterDir(repair), "vtr_report.txt"), Integer.toString(MavenUtils.FAIL_TEST_WITHOUT_MUTATION));
				FileUtils.copyFile(repair.getPatchFile(), dstPatchFile);
				if (modified) {
					pi.revert();
				}
				return;
			}
			for (File resultDir : PitInstrumenter.getPitResultsDir(this.projectDir)) {
				org.codehaus.plexus.util.FileUtils.copyDirectoryStructure(resultDir, getAfterDir(repair));
				org.codehaus.plexus.util.FileUtils.deleteDirectory(resultDir);
				LOGGER.info("Found PIT results: {}", resultDir.getPath());
				FileUtils.copyFile(repair.getPatchFile(), dstPatchFile);
			}
			// Finalize
			if (modified) {
				pi.revert();
			}
		} catch (IOException | MavenInvocationException e) {
			LOGGER.warn("Failed to invoke PIT mutation testing: {} at {}", repair.getTestCaseClassName(), repair.getCommit().getId());
		}
	}

	@Override
	public void compare(Repair repair) {
		File beforeFile = new File(getBeforeDir(repair), "index.html");
		File afterFile = new File(getAfterDir(repair), "index.html");
		if (!beforeFile.exists()) {
			repair.setStatus(this, Repair.Status.Broken);
			results.put(repair, new Result(-1, -1));
			return;
		}
		if (afterFile.exists()) {
			try {
				int before = getNumOfKilledMutants(beforeFile);
				int after = getNumOfKilledMutants(afterFile);
				if (before < after) {
					repair.setStatus(this, Repair.Status.Improved);
				} else if (before > after) {
					repair.setStatus(this, Repair.Status.Degraded);
				} else {
					repair.setStatus(this, Repair.Status.Stay);
				}
				results.put(repair, new Result(before, after));
			} catch (IOException e) {
				LOGGER.warn("Failed to read files: {}", e.getMessage());
				repair.setStatus(this, Repair.Status.Broken);
				results.put(repair, new Result(-1, -1));
			}
		} else {
			afterFile = new File(getAfterDir(repair), "vtr_report.txt");
			if (afterFile.exists()) {
				repair.setStatus(this, Repair.Status.Improved);
				results.put(repair, new Result(-100, -100));
			} else {
				repair.setStatus(this, Repair.Status.Broken);
				results.put(repair, new Result(-1, -1));
			}
		}
	}

	private static int getNumOfKilledMutants(File file) throws IOException {
		String content = FileUtils.readFileToString(file);
		Document document = Jsoup.parse(content, "", Parser.xmlParser());
		Elements elements = document.select("body > table:nth-child(3) > tbody > tr > td:nth-child(3) > div > div.coverage_ledgend");
		String text = elements.get(0).text();
		String[] split = text.split("/");
		return Integer.parseInt(split[0]);
	}

	@Override
	public void output(List<Repair> repairs) throws IOException {
		File rootDir = EvaluatorBase.getRepairDir(outputDir, projectId);
		File dir = new File(rootDir, "mutation");
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
		builder.append("Before score").append(",");
		builder.append("After score");
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
			Result result = results.get(repair);
			builder.append(result == null ? "" : result.getBefore()).append(",");
			builder.append(result == null ? "" : result.getAfter());
			// end
			builder.append("\n");
		}
		// write
		File csv = new File(dir, EvaluatorBase.REPAIR_FILENAME);
		FileUtils.write(csv, builder.toString());
	}

	private File getBeforeDir(Repair repair) {
		File rootDir = EvaluatorBase.getRepairDir(outputDir, projectId);
		File mutationDir = new File(rootDir, "mutation");
		File resultsDir = new File(mutationDir, "results");
		File commitDir = new File(resultsDir, repair.getCommit().getId());
		return new File(commitDir, repair.getTestCaseFullName());
	}

	private File getAfterDir(Repair repair) {
		File rootDir = EvaluatorBase.getRepairDir(outputDir, projectId);
		File mutationDir = new File(rootDir, "mutation");
		File commitDir = new File(mutationDir, repair.getCommit().getId());
		File validatorDir = new File(commitDir, repair.getValidatorName());
		String patchName = repair.getPatchFile().getName().replace(".patch", "");
		File patchDir = new File(validatorDir, patchName);
		if (!patchDir.exists()) {
			patchDir.mkdirs();
		}
		return patchDir;
	}

	private static class Result {
		private int before;
		private int after;

		private Result(int before, int after) {
			this.before = before;
			this.after = after;
		}

		public int getBefore() {
			return before;
		}

		public int getAfter() {
			return after;
		}
	}
}
