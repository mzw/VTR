package jp.mzw.vtr.repair;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.dom4j.DocumentException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import difflib.DiffUtils;
import difflib.Patch;
import difflib.PatchFailedException;
import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.CheckoutConductor;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.maven.MavenUtils;
import jp.mzw.vtr.maven.PitInstrumenter;
import jp.mzw.vtr.validate.ValidatorBase;

public class Repairer {
	protected static Logger LOGGER = LoggerFactory.getLogger(Repairer.class);

	protected String projectId;
	protected File projectDir;
	protected File outputDir;
	protected File mavenHome;

	protected String sut;

	protected List<Repair> repairs;

	public Repairer(Project project, String sut) {
		this.projectId = project.getProjectId();
		this.projectDir = project.getProjectDir();
		this.outputDir = project.getOutputDir();
		this.mavenHome = project.getMavenHome();
		this.sut = sut;
		this.repairs = new ArrayList<>();
	}

	public Repairer parse() {
		File projectDir = new File(this.outputDir, this.projectId);
		File validateDir = new File(projectDir, ValidatorBase.VALIDATOR_DIRNAME);
		for (File commitDir : validateDir.listFiles()) {
			if (commitDir.isDirectory()) {
				String commitId = commitDir.getName();
				for (File patternDir : commitDir.listFiles()) {
					for (File patch : patternDir.listFiles()) {
						if (!patch.getName().endsWith(".patch")) {
							continue;
						}
						String[] name = patch.getName().replace(".patch", "").split("#");
						String clazz = name[0];
						String method = name[1];
						Repair repair = new Repair(new Commit(commitId, null), patch).setTestCaseNames(clazz, method);
						repairs.add(repair);
					}
				}
			}
		}
		return this;
	}

	public List<Repair> getRepairs() {
		return this.repairs;
	}

	public void repair(CheckoutConductor cc, Repair repair) throws IOException, GitAPIException, MavenInvocationException, DocumentException,
			PatchFailedException {
		cc.checkout(repair.getCommit());
		PitInstrumenter pi = new PitInstrumenter(this.projectDir, this.sut, repair.getTestCaseClassName());
		boolean modified = pi.instrument();
		// Before applying patch
		LOGGER.info("Invoke PIT for original version");
		MavenUtils.maven(this.projectDir, Arrays.asList("compile", "test-compile"), this.mavenHome);
		MavenUtils.maven(this.projectDir, Arrays.asList("org.pitest:pitest-maven:mutationCoverage"), this.mavenHome);
		for (File resultDir : PitInstrumenter.getPitResultsDir(this.projectDir)) {
			org.codehaus.plexus.util.FileUtils.copyDirectoryStructure(resultDir, getCopyDestinationDir(repair.getPatch(), "before"));
			org.codehaus.plexus.util.FileUtils.deleteDirectory(resultDir);
			LOGGER.info("Found PIT results: {}", resultDir.getPath());
		}
		// Apply patch
		Patch<String> patch = DiffUtils.parseUnifiedDiff(FileUtils.readLines(repair.getPatch()));
		File file = repair.getFile(this.projectDir);
		List<String> target = FileUtils.readLines(file);
		List<String> applied = patch.applyTo(target);
		FileUtils.writeLines(file, applied);
		// After applying patch
		LOGGER.info("Invoke PIT for applied version");
		MavenUtils.maven(this.projectDir, Arrays.asList("compile", "test-compile"), this.mavenHome);
		MavenUtils.maven(this.projectDir, Arrays.asList("org.pitest:pitest-maven:mutationCoverage"), this.mavenHome);
		for (File resultDir : PitInstrumenter.getPitResultsDir(this.projectDir)) {
			org.codehaus.plexus.util.FileUtils.copyDirectoryStructure(resultDir, getCopyDestinationDir(repair.getPatch(), "after"));
			org.codehaus.plexus.util.FileUtils.deleteDirectory(resultDir);
			LOGGER.info("Found PIT results: {}", resultDir.getPath());
		}
		// Finalize
		if (modified) {
			pi.revert();
		}
		FileUtils.writeLines(file, target);
		MavenUtils.maven(this.projectDir, Arrays.asList("clean"), this.mavenHome);
	}

	private File getCopyDestinationDir(File patch, String name) {
		File parentDir = new File(patch.getParentFile(), patch.getName().replace(".patch", ""));
		File dir = new File(parentDir, name);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		return dir;
	}
}
