package jp.mzw.vtr.classify;

import jp.mzw.vtr.classify.pattern.AddFailStatementsForExpectedExceptionPattern;
import jp.mzw.vtr.classify.pattern.AddOverrideAnnotaionPattern;
import jp.mzw.vtr.classify.pattern.AddSerialVersionUidPattern;
import jp.mzw.vtr.classify.pattern.AddSuppressAnnotationPattern;
import jp.mzw.vtr.classify.pattern.AddTestAnnotationPattern;
import jp.mzw.vtr.classify.pattern.CloseResourcesPattern;
import jp.mzw.vtr.classify.pattern.ConvertControlStatementToBlockPattern;
import jp.mzw.vtr.classify.pattern.DoNotSwallowTestErrorsSilientlyPattern;
import jp.mzw.vtr.classify.pattern.FixJavadocErrorsPattern;
import jp.mzw.vtr.classify.pattern.HandleExpectedExceptionsProperlyPatrern;
import jp.mzw.vtr.classify.pattern.ImportAssertNonStaticPattern;
import jp.mzw.vtr.classify.pattern.OutputForDebugPattern;
import jp.mzw.vtr.classify.pattern.RemoveObsoleteVariableAssignmentPattern;
import jp.mzw.vtr.classify.pattern.RemoveRedundantCastPattern;
import jp.mzw.vtr.classify.pattern.RemoveThisQualifierPattern;
import jp.mzw.vtr.classify.pattern.IntroduceAutoBoxingPattern;
import jp.mzw.vtr.classify.pattern.RemoveUnusedExceptionsPattern;
import jp.mzw.vtr.classify.pattern.ReplaceAtTodoWithTODOPattern;
import jp.mzw.vtr.classify.pattern.SwapActualExpectedValuePattern;
import jp.mzw.vtr.classify.pattern.UseAssertEqualsProperlyPattern;
import jp.mzw.vtr.classify.pattern.UseAssertNotSamePattern;
import jp.mzw.vtr.classify.pattern.UseAssignmentOperatorPattern;
import jp.mzw.vtr.classify.pattern.UseAtCodeInsteadOfTtTag;
import jp.mzw.vtr.classify.pattern.UseDiamondOperatorPattern;
import jp.mzw.vtr.classify.pattern.UseEnhancedForLoopPattern;
import jp.mzw.vtr.classify.pattern.UseFinalModifierPattern;
import jp.mzw.vtr.classify.pattern.UseProcessWaitForPattern;
import jp.mzw.vtr.classify.pattern.UseProperFileConstructorPattern;
import jp.mzw.vtr.classify.pattern.UseStaticFieldDirectlyPattern;
import jp.mzw.vtr.classify.pattern.UseStaticMethodDirectlyPattern;
import jp.mzw.vtr.classify.pattern.UseStringContainsPattern;
import jp.mzw.vtr.classify.pattern.UseTryWithResourcesPattern;
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
        if (UseAssertEqualsProperlyPattern.match(testCaseModification)) {
            return "#3";
        }
        if (UseAssertNotSamePattern.match(testCaseModification)) {
            return "#5";
        }
        if (UseStringContainsPattern.match(testCaseModification)) {
            return "#9";
        }
        if (SwapActualExpectedValuePattern.match(testCaseModification)) {
            return "#10";
        }
        if (ImportAssertNonStaticPattern.match(testCaseModification)) {
            return "#12";
        }
        if (DoNotSwallowTestErrorsSilientlyPattern.match(testCaseModification)) {
            return "#13";
        }
        if (AddFailStatementsForExpectedExceptionPattern.match(testCaseModification)) {
            return "#14";
        }
        if (HandleExpectedExceptionsProperlyPatrern.match(testCaseModification)) {
            return "#15";
        }
        if (RemoveUnusedExceptionsPattern.match(testCaseModification)) {
            return "#16";
        }
        if (CloseResourcesPattern.match(testCaseModification)) {
            return "#17";
        }
        if (UseTryWithResourcesPattern.match(testCaseModification)) {
            return "#18";
        }
        if (UseProcessWaitForPattern.match(testCaseModification)) {
            return "#19";
        }
        if (AddSuppressAnnotationPattern.match(testCaseModification)) {
            return "#20";
        }
        if (RemoveObsoleteVariableAssignmentPattern.match(testCaseModification)) {
            return "#21";
        }
        if (RemoveRedundantCastPattern.match(testCaseModification)) {
            // TODO: implement
            // return "#22";
        }
        if (IntroduceAutoBoxingPattern.match(testCaseModification)) {
            return "#23";
        }
        if (OutputForDebugPattern.match(testCaseModification)) {
            return "#24";
        }
        if (FixJavadocErrorsPattern.match(testCaseModification)) {
            return "#25";
        }
        if (ReplaceAtTodoWithTODOPattern.match(testCaseModification)) {
            return "#26";
        }
        if (UseAtCodeInsteadOfTtTag.match(testCaseModification)) {
            return "#27";
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
        if (ConvertControlStatementToBlockPattern.match(testCaseModification)) {
            return "#31";
        }
        if (RemoveThisQualifierPattern.match(testCaseModification)) {
            return "#32";
        }
        if (AddSerialVersionUidPattern.match(testCaseModification)) {
            return "#33";
        }
        if (AddOverrideAnnotaionPattern.match(testCaseModification)) {
            return "#34";
        }
        if (UseDiamondOperatorPattern.match(testCaseModification)) {
            return "#35";
        }
        if (UseAssignmentOperatorPattern.match(testCaseModification)) {
            return "#36";
        }
        if (UseProperFileConstructorPattern.match(testCaseModification)) {
            // TODO: implement
            // return "#37";
        }
        if (UseStaticMethodDirectlyPattern.match(testCaseModification)) {
            // TODO: implement
            // return "#39";
        }
        if (UseStaticFieldDirectlyPattern.match(testCaseModification)) {
            // TODO: implement
            // return "#40";
        }
        if (ReorganizeTestCases.match(testCaseModification)) {
            // TODO: implement
            // return "#L3";
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
