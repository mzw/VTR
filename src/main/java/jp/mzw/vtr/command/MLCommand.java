package jp.mzw.vtr.command;

import static jp.mzw.vtr.CLI.CONFIG_FILENAME;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;

import jp.mzw.vtr.cluster.similarity.DistAnalyzer;
import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.detect.TestCaseModification;

public class MLCommand {

	public static void command(String... args) throws IOException, ParseException, NoHeadException, GitAPIException {
		if (args.length == 1) {
			String mode = args[0];
			Project dummyProject = new Project(null).setConfig(CONFIG_FILENAME);

			if ("csv".equals(mode)) {
				List<TrainingData> data = readTrainingData();
				String csv = makeCsv(dummyProject.getOutputDir(), data);
				FileUtils.writeStringToFile(new File(dummyProject.getOutputDir(), "weka.csv"), csv);
			} else if ("learn".equals(mode)) {
				// TODO to be implemented
			}
		} else {
			System.out.println("$ java -cp=<class-path> jp.mzw.vtr.CLI ml <mode: csv or learn>");
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
			if (tcm.getProjectId().equals(this.projectId) &&
					tcm.getCommitId().equals(this.commitId) &&
					tcm.getClassName().equals(this.className) &&
					tcm.getMethodName().equals(this.methodName)) {
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
		Map<String, TestCaseModification> words = new HashMap<>();
		Map<String, TestCaseModification> originalSyntaxes = new HashMap<>();
		Map<String, TestCaseModification> revisedSyntaxes = new HashMap<>();
		for (TestCaseModification tcm : tcmList) {
			for (String word : tcm.getCommitMessage().split(" ")) {
				if ("\n".equals(word)) {
					continue;
				}
				words.put(word.trim().replace("\n", ""), tcm);
			}
			for (String syntax : tcm.getOriginalNodeClasses()) {
				originalSyntaxes.put(syntax, tcm);
			}
			for (String syntax : tcm.getRevisedNodeClasses()) {
				revisedSyntaxes.put(syntax, tcm);
			}
		}

		StringBuilder csv = new StringBuilder();
		csv.append("Subject").append(",");
		csv.append("Commit").append(",");
		csv.append("Class").append(",");
		csv.append("Method").append(",");
		csv.append("Answer").append(",");
		
		// From counting data
		for (String word : words.keySet()) {
			csv.append(StringEscapeUtils.escapeCsv(word)).append(",");
		}
		
		for (String syntax : originalSyntaxes.keySet()) {
			csv.append("origin: ").append(syntax).append(",");
		}

		for (String syntax : revisedSyntaxes.keySet()) {
			csv.append("revised: ").append(syntax).append(",");
		}
		csv.append("dummy");
		
		String delim = "";
		for (TestCaseModification tcm : tcmList) {
			StringBuilder line = new StringBuilder();
			line.append(tcm.getCsvLineHeader());
			for (final TrainingData d : data) {
				if (d.isSame(tcm)) {
					line.append(d.answer).append(",");
					break;
				}
			}

			String[] myWords = tcm.getCommitMessage().split(" ");
			for (String word : words.keySet()) {
				int count = 0;
				for (String myWord : myWords) {
					if (word.equals(myWord)) {
						count++;
					}
				}
				line.append(count);
				line.append(",");
			}

			for (String syntax : originalSyntaxes.keySet()) {
				int count = 0;
				for (String mine : tcm.getOriginalNodeClasses()) {
					if (syntax.equals(mine)) {
						count++;
					}
				}
				line.append(count).append(",");
			}
			for (String syntax : revisedSyntaxes.keySet()) {
				int count = 0;
				for (String mine : tcm.getRevisedNodeClasses()) {
					if (syntax.equals(mine)) {
						count++;
					}
				}
				line.append(count).append(",");
			}
			line.append("dummy");
			

			csv.append(line).append(delim);
			delim = "\n";
		}

		return csv.toString();
	}

}
