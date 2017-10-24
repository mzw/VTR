package jp.mzw.vtr.command;

import static jp.mzw.vtr.CLI.CONFIG_FILENAME;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;

import jp.mzw.vtr.cluster.machine_larning.MachineLearning;
import jp.mzw.vtr.cluster.similarity.DistAnalyzer;
import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.detect.TestCaseModification;

public class MLCommand {
	
	public static final String FILENAME = "data.csv";

	public static void command(String... args) throws Exception {
		if (args.length == 1) {
			String mode = args[0];
			Project dummyProject = new Project(null).setConfig(CONFIG_FILENAME);

			if ("mkCsv".equals(mode)) {
				List<TrainingData> data = readTrainingData();
				String csv = makeCsv(dummyProject.getOutputDir(), data);
				FileUtils.writeStringToFile(new File(dummyProject.getOutputDir(), FILENAME), csv);
			} else if ("learn".equals(mode)) {
				MachineLearning ml = new MachineLearning();
				ml.learn(new FileInputStream(new File(dummyProject.getOutputDir(), FILENAME)),
						new FileInputStream(new File(dummyProject.getOutputDir(), FILENAME)));
			}
		} else {
			System.out.println("$ java -cp=<class-path> jp.mzw.vtr.CLI ml <mode: mkCsv or learn>");
		}
	}

	private static List<TrainingData> readTrainingData() throws IOException {
		List<TrainingData> ret = new ArrayList<>();

		InputStream is = MLCommand.class.getClassLoader().getResourceAsStream("training-data.csv");
		String content = IOUtils.toString(is);
		CSVParser parser = CSVParser.parse(content, CSVFormat.DEFAULT);
		List<CSVRecord> records = parser.getRecords();
		for (CSVRecord record : records) {
			String projectId = record.get(1);
			String commitId = record.get(2);
			String className = record.get(3);
			String methodName = record.get(4);
			String answer = record.get(5);

			TrainingData data = new TrainingData(projectId, commitId, className, methodName, answer);
			ret.add(data);
		}

		return ret;
	}

	private static class TrainingData {
		private final String projectId;
		private final String commitId;
		private final String className;
		private final String methodName;
		private final String answer;

		private TrainingData(final String projectId, final String commitId, final String className, final String methodName, final String answer) {
			this.projectId = projectId;
			this.commitId = commitId;
			this.className = className;
			this.methodName = methodName;
			this.answer = answer;
		}

		private boolean isSame(TestCaseModification tcm) {
			if (tcm.getProjectId().equals(this.projectId) && tcm.getCommitId().equals(this.commitId) && tcm.getClassName().equals(this.className)
					&& tcm.getMethodName().equals(this.methodName)) {
				return true;
			}
			return false;
		}
	}

	private static String makeCsv(final File outputDir, final List<TrainingData> data)
			throws IOException, RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException, CheckoutConflictException, GitAPIException {
		List<TestCaseModification> tcmList = DistAnalyzer.parseTestCaseModifications(outputDir, new ArrayList<String>());
		for (TestCaseModification tcm : tcmList) {
			tcm.parse().identifyCommit();
		}
		List<String> words = new ArrayList<>();
		List<String> originalSyntaxes = new ArrayList<>();
		List<String> revisedSyntaxes = new ArrayList<>();
		for (TestCaseModification tcm : tcmList) {
			for (String word : makeWords(tcm.getCommitMessage())) {
				if (!words.contains(word)) {
					words.add(word);
				}
			}
			for (String syntax : tcm.getOriginalNodeClasses()) {
				if (!originalSyntaxes.contains(syntax)) {
					originalSyntaxes.add(syntax);
				}
			}
			for (String syntax : tcm.getRevisedNodeClasses()) {
				if (!revisedSyntaxes.contains(syntax)) {
					revisedSyntaxes.add(syntax);
				}
			}
		}

		StringBuilder csv = new StringBuilder();
		csv.append("VTR: Subject").append(",");
		csv.append("VTR: Commit").append(",");
		csv.append("VTR: Class").append(",");
		csv.append("VTR: Method").append(",");
		csv.append("VTR: AuthorName").append(",");
		csv.append("VTR: AuthorEmailAddress").append(",");
		csv.append("VTR: Answer").append(",");

		// From counting data
		for (String word : words) {
			csv.append("word: " + StringEscapeUtils.escapeCsv(word)).append(",");
		}

		for (String syntax : originalSyntaxes) {
			csv.append("origin: ").append(syntax).append(",");
		}

		String delim = "";
		for (String syntax : revisedSyntaxes) {
			csv.append(delim).append("revised: ").append(syntax);
			delim = ",";
		}
		csv.append("\n");

		delim = "";
		for (TestCaseModification tcm : tcmList) {
			StringBuilder line = new StringBuilder();
			line.append(tcm.getCsvLineHeader());
			for (final TrainingData d : data) {
				if (d.isSame(tcm)) {
					line.append(d.answer).append(",");
					break;
				}
			}

			for (String word : words) {
				int count = 0;
				for (String mine : makeWords(tcm.getCommitMessage())) {
					if (word.equals(mine)) {
						count++;
					}
				}
				line.append(count);
				line.append(",");
			}

			for (String syntax : originalSyntaxes) {
				int count = 0;
				for (String mine : tcm.getOriginalNodeClasses()) {
					if (syntax.equals(mine)) {
						count++;
					}
				}
				line.append(count).append(",");
			}
			String _delim = "";
			for (String syntax : revisedSyntaxes) {
				int count = 0;
				for (String mine : tcm.getRevisedNodeClasses()) {
					if (syntax.equals(mine)) {
						count++;
					}
				}
				line.append(_delim).append(count);
				_delim = ",";
			}

			csv.append(delim).append(line);
			delim = "\n";
		}

		return csv.toString();
	}

	private static String[] makeWords(final String sentences) {
		String[] candidates = sentences.split("\\s+");
		String[] ret = new String[candidates.length];
		for (int i = 0; i < candidates.length; i++) {
			ret[i] = candidates[i].replaceAll("[^\\w]", "");
		}
		return ret;
	}
}
