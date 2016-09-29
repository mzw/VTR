package jp.mzw.vtr.detect;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.patch.HunkHeader;
import org.eclipse.jgit.patch.Patch;

public class PatchAnalyzer {

	protected File projectDir;

	protected Patch patch;

	protected Map<File, List<ModifiedLineRange>> modifiedLineRangesByFile;

	public PatchAnalyzer(File projectDir, Patch patch) {
		this.projectDir = projectDir;
		this.patch = patch;
		this.modifiedLineRangesByFile = new HashMap<>();
	}

	public PatchAnalyzer analyze() {
		for (FileHeader fh : patch.getFiles()) {
			List<ModifiedLineRange> modifiedLineRanges = new ArrayList<>();
			for (HunkHeader hunk : fh.getHunks()) {
				for (Edit edit : hunk.toEditList()) {
					List<Integer> newLineRange = new ArrayList<>();
					List<Integer> oldLineRange = new ArrayList<>();
					for (int lineno = edit.getBeginA() + 1; lineno <= edit.getEndA(); lineno++) {
						oldLineRange.add(lineno);
					}
					for (int lineno = edit.getBeginB() + 1; lineno <= edit.getEndB(); lineno++) {
						newLineRange.add(lineno);
					}
					ModifiedLineRange modifiedLineRange = new ModifiedLineRange(newLineRange, oldLineRange);
					modifiedLineRanges.add(modifiedLineRange);
				}
			}
			this.modifiedLineRangesByFile.put(new File(this.projectDir, fh.getNewPath()), modifiedLineRanges);
		}
		return this;
	}

	public List<ModifiedLineRange> getModifiedLineRanges(File file) {
		for (File key : this.modifiedLineRangesByFile.keySet()) {
			if (key.equals(file)) {
				return this.modifiedLineRangesByFile.get(key);
			}
		}
		return null;
	}

	public static class ModifiedLineRange {
		List<Integer> newLineRange;
		List<Integer> oldLineRange;

		public ModifiedLineRange(List<Integer> newLineRange, List<Integer> oldLineRange) {
			this.newLineRange = newLineRange;
			this.oldLineRange = oldLineRange;
		}

		public List<Integer> getNewLineRange() {
			return this.newLineRange;
		}

		public List<Integer> getOldLineRange() {
			return this.oldLineRange;
		}

		public static List<Integer> getMergedNewLineRange(List<ModifiedLineRange> modifiedLineRanges) {
			List<Integer> ret = new ArrayList<>();
			for (ModifiedLineRange mlr : modifiedLineRanges) {
				ret.addAll(mlr.getNewLineRange());
			}
			return ret;
		}

		public static List<Integer> getMergedOldLineRange(List<ModifiedLineRange> modifiedLineRanges) {
			List<Integer> ret = new ArrayList<>();
			for (ModifiedLineRange mlr : modifiedLineRanges) {
				ret.addAll(mlr.getOldLineRange());
			}
			return ret;
		}
	}
}
