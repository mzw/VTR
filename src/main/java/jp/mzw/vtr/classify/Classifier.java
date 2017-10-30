package jp.mzw.vtr.classify;

import jp.mzw.vtr.classify.pattern.AddTestAnnotationPattern;
import jp.mzw.vtr.classify.pattern.DoNotSwallowTestErrorsSilientlyPattern;
import jp.mzw.vtr.classify.pattern.FixJavadocErrorsPattern;
import jp.mzw.vtr.classify.pattern.HandleExpectedExceptionsProperlyPatrern;
import jp.mzw.vtr.classify.pattern.RemoveObsoleteVariableAssignmentPattern;
import jp.mzw.vtr.classify.pattern.RemoveThisQualifierPattern;
import jp.mzw.vtr.classify.pattern.RemoveUnnecessaryCastPattern;
import jp.mzw.vtr.classify.pattern.RemoveUnusedExceptionsPattern;
import jp.mzw.vtr.classify.pattern.ReplaceAtTodoWithTODOPattern;
import jp.mzw.vtr.classify.pattern.UseEnhancedForLoopPattern;
import jp.mzw.vtr.classify.pattern.UseFinalModifierPattern;
import jp.mzw.vtr.classify.pattern.UseStringContainsPattern;
import jp.mzw.vtr.classify.pattern.limitation.ReorganizeTestCases;
import jp.mzw.vtr.cluster.similarity.LcsAnalyzer;
import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.detect.TestCaseModification;
import jp.mzw.vtr.classify.pattern.FormatCodePattern;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class Classifier {
    static Logger LOGGER = LoggerFactory.getLogger(Classifier.class);

    public static final String CLASSIFY_DIR = "classify";
    public static final String CLASSIFY_FILE = "classify.csv";

    protected Project project;

    public Classifier(Project project) {
        this.project = project;
    }

    public void output() throws GitAPIException{
        Path path = getOutputFilePath();
        try (BufferedWriter bw = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.WRITE)){
            List<String> contents = classify();
            for (String content : contents) {
                bw.write(content);
            }
        } catch (IOException e) {
            LOGGER.error("Can't write classify results @{}", path);
        }
    }

    private String filter(TestCaseModification testCaseModification) {
        if (testCaseModification.getOriginalNodeClassesWithText() == null
                || testCaseModification.getRevisedNodeClassesWithText() == null) {
            return "NullAst";
        }
        if (testCaseModification.getCommitId() == null
                || testCaseModification.getCommitMessage() == null) {
            return "NullCommit";
        }
        if (AddTestAnnotationPattern.match(testCaseModification)) {
            return "#1";
        }
        if (UseStringContainsPattern.match(testCaseModification)) {
            return "#9";
        }
        if (HandleExpectedExceptionsProperlyPatrern.match(testCaseModification)) {
            return "#15";
        }
        if (RemoveUnusedExceptionsPattern.match(testCaseModification)) {
            return "#16";
        }
        if (DoNotSwallowTestErrorsSilientlyPattern.match(testCaseModification)) {
            return "#18";
        }
        if (RemoveObsoleteVariableAssignmentPattern.match(testCaseModification)) {
            return "#21";
        }
        if (RemoveUnnecessaryCastPattern.match(testCaseModification)) {
            return "#23";
        }
        if (FixJavadocErrorsPattern.match(testCaseModification)) {
            return "#25";
        }
        if (ReplaceAtTodoWithTODOPattern.match(testCaseModification)) {
            return "#26";
        }
        if (FormatCodePattern.match(testCaseModification)) {
            return "#28";
        }
        if (UseEnhancedForLoopPattern.match(testCaseModification)) {
            return "#29";
        }
        if (UseFinalModifierPattern.match(testCaseModification)) {
            return "#30";
        }

        if (RemoveThisQualifierPattern.match(testCaseModification)) {
            return "#32";
        }
        if (ReorganizeTestCases.match(testCaseModification)) {
            return "L3";
        }
        return "Nan";
    }

    private List<String> classify() throws IOException, GitAPIException {
        List<String> contents = new ArrayList<>();
        List<TestCaseModification> testCaseModifications =
                new LcsAnalyzer(project.getOutputDir()).parseTestCaseModifications();
        for (TestCaseModification testCaseModification : testCaseModifications) {
            contents.add(_classify(testCaseModification.parse().identifyCommit()));
        }
        return contents;
    }

    private String _classify(TestCaseModification testCaseModification) {
        StringBuilder sb = new StringBuilder();
        sb.append(testCaseModification.getProjectId()).append(",");
        sb.append(testCaseModification.getCommitId()).append(",");
        sb.append(testCaseModification.getClassName()).append(",");
        sb.append(testCaseModification.getMethodName()).append(",");
        String item = filter(testCaseModification);
        sb.append(item).append("\n");
        return sb.toString();
    }

    private Path getOutputFilePath() {
        Path dirPath = Paths.get(String.join("/", project.getOutputDir().getName(), CLASSIFY_DIR));
        if (!Files.exists(dirPath)) {
            try {
                Files.createDirectories(dirPath);
            } catch (IOException e) {
                LOGGER.error("can't create directory for filter result");
            }
        }
        Path filePath = Paths.get(String.join("/", dirPath.toString(), CLASSIFY_FILE));
        if (!Files.exists(filePath)) {
            try {
                Files.createFile(filePath);
            } catch (IOException e) {
                LOGGER.error("can't create file for filter result");
            }
        }
        return filePath;
    }
}
