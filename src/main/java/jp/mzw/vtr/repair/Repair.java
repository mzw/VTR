package jp.mzw.vtr.repair;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jp.mzw.vtr.git.Commit;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import difflib.Chunk;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;
import difflib.PatchFailedException;

public class Repair {
	protected static Logger LOGGER = LoggerFactory.getLogger(Repair.class);

	private Commit commit;
	private String validatorName;
	private File patchFile;

	private List<String> patchLines;
	private Patch<String> patch;

	public Repair(Commit commit, String validatorName, File patchFile) throws IOException {
		this.commit = commit;
		this.validatorName = validatorName;
		this.patchFile = patchFile;

		this.patchLines = FileUtils.readLines(this.patchFile);
		patch = DiffUtils.parseUnifiedDiff(patchLines);

		results = new HashMap<>();
	}

	public Commit getCommit() {
		return this.commit;
	}

	public String getValidatorName() {
		return this.validatorName;
	}

	public File getPatchFile() {
		return this.patchFile;
	}

	public boolean isSameContent(File patch) throws IOException {
		List<String> lines = FileUtils.readLines(patch);
		if (lines == null) {
			return false;
		}
		if (patchLines.size() != lines.size()) {
			return false;
		}
		for (int i = 0; i < patchLines.size(); i++) {
			String patchLine = patchLines.get(i);
			String line = lines.get(i);
			if (!patchLine.equals(line)) {
				return false;
			}
		}
		return true;
	}

	public Patch<String> getPatch() {
		return this.patch;
	}

	public String toString() {
		return this.commit.getId() + ", " + this.validatorName + ", " + this.patchFile.getName();
	}

	private String clazz;
	private String method;

	public Repair setTestCaseNames(String clazz, String method) {
		this.clazz = clazz;
		this.method = method;
		return this;
	}

	public String getTestCaseClassName() {
		return this.clazz;
	}

	public String getTestCaseMethodName() {
		return this.method;
	}

	public String getTestCaseFullName() {
		return this.clazz + "#" + this.method;
	}

	public File testFile;
	private List<String> originalTestFileContent;
	private List<String> modifiedTestFileContent;

	public Repair parse(File projectDir) throws IOException {
		this.testFile = getFile(projectDir);
		this.originalTestFileContent = FileUtils.readLines(this.testFile);
		return this;
	}

	public File getFile(File projectDir) {
		File src = new File(projectDir, "src/test/java");
		File file = new File(src, this.clazz.replace(".", "/") + ".java");
		return file;
	}

	public List<String> getOriginalTestFileContent() {
		return this.originalTestFileContent;
	}

	public List<String> getModifiedTestFileContent() {
		return this.modifiedTestFileContent;
	}

	public boolean apply(File projectDir) throws IOException, PatchFailedException {
		try {
			modifiedTestFileContent = patch.applyTo(originalTestFileContent);
			FileUtils.writeLines(testFile, modifiedTestFileContent);
		} catch (difflib.PatchFailedException e) {
			// TODO This exception might be caused by "Incorrect Chunk: the
			// chunk content doesn't match the target"
			LOGGER.warn("Invalid patch: {} at {} with {}, {}", getTestCaseFullName(), commit.getId(), getValidatorName(), e.getMessage());
			return false;
		}
		return true;
	}

	public void revert() throws IOException, PatchFailedException {
		FileUtils.writeLines(testFile, originalTestFileContent);
	}

	public String getOriginalPart() {
		StringBuilder builder = new StringBuilder();
		for (Delta<String> delta : this.patch.getDeltas()) {
			Chunk<String> original = delta.getOriginal();
			for (int i = 0; i < original.size(); i++) {
				int index = original.getPosition() + i + 1;
				if (originalTestFileContent.size() <= index) {
					continue;
				}
				builder.append(originalTestFileContent.get(index)).append("\n");
			}
		}
		return builder.toString();
	}

	public String getRevisedPart() {
		StringBuilder builder = new StringBuilder();
		for (Delta<String> delta : this.patch.getDeltas()) {
			Chunk<String> revised = delta.getRevised();
			for (int i = 0; i < revised.size(); i++) {
				int index = revised.getPosition() + i + 1;
				if (modifiedTestFileContent.size() <= index) {
					continue;
				}
				builder.append(modifiedTestFileContent.get(index)).append("\n");
			}
		}
		return builder.toString();
	}

	private Map<EvaluatorBase, Status> results;

	public enum Status {
		Improved, PartiallyImproved, Stay, Degraded, Broken
	}

	public void setStatus(EvaluatorBase evaluator, Status status) {
		results.put(evaluator, status);
	}

	public Status getStatus(EvaluatorBase evaluator) {
		return results.get(evaluator);
	}

	public String toCsv(EvaluatorBase evaluator) {
		StringBuilder builder = new StringBuilder();
		builder.append(commit.getId()).append(",");
		builder.append(validatorName).append(",");
		builder.append(clazz).append(",");
		builder.append(method).append(",");
		builder.append(results.get(evaluator));
		return builder.toString();
	}
}
