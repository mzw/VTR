package jp.mzw.vtr.classify;

import jp.mzw.vtr.classify.pattern.AddCastToNullPattern;
import jp.mzw.vtr.classify.pattern.AddFailStatementsForExpectedExceptionPattern;
import jp.mzw.vtr.classify.pattern.AddOverrideAnnotaionPattern;
import jp.mzw.vtr.classify.pattern.AddSerialVersionUidPattern;
import jp.mzw.vtr.classify.pattern.AddSuppressAnnotationPattern;
import jp.mzw.vtr.classify.pattern.AddTestAnnotationPattern;
import jp.mzw.vtr.classify.pattern.AssertNotNullToINstancesPattern;
import jp.mzw.vtr.classify.pattern.CloseResourcesPattern;
import jp.mzw.vtr.classify.pattern.ConvertControlStatementToBlockPattern;
import jp.mzw.vtr.classify.pattern.DoNotSwallowTestErrorsSilientlyPattern;
import jp.mzw.vtr.classify.pattern.UsePrimitiveValueMethodPattern;
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
import jp.mzw.vtr.classify.pattern.UseAssertArrayEqualsProperlyPattern;
import jp.mzw.vtr.classify.pattern.UseAssertEqualsProperlyPattern;
import jp.mzw.vtr.classify.pattern.UseAssertFalseProperlyPattern;
import jp.mzw.vtr.classify.pattern.UseAssertNotSameProperlyPattern;
import jp.mzw.vtr.classify.pattern.UseAssertNullProperlyPattern;
import jp.mzw.vtr.classify.pattern.UseAssertTrueProperlyPattern;
import jp.mzw.vtr.classify.pattern.UseAssignmentOperatorPattern;
import jp.mzw.vtr.classify.pattern.UseAtCodeInsteadOfTtTag;
import jp.mzw.vtr.classify.pattern.UseDiamondOperatorPattern;
import jp.mzw.vtr.classify.pattern.UseEnhancedForLoopPattern;
import jp.mzw.vtr.classify.pattern.UseFailInsteadOfAssertTruePattern;
import jp.mzw.vtr.classify.pattern.UseFinalModifierPattern;
import jp.mzw.vtr.classify.pattern.UseProcessWaitForPattern;
import jp.mzw.vtr.classify.pattern.UseProperFileConstructorPattern;
import jp.mzw.vtr.classify.pattern.UseStaticFieldDirectlyPattern;
import jp.mzw.vtr.classify.pattern.UseStaticMethodDirectlyPattern;
import jp.mzw.vtr.classify.pattern.UseStringContainsPattern;
import jp.mzw.vtr.classify.pattern.UseTryWithResourcesPattern;
import jp.mzw.vtr.classify.pattern.UseValueofMethodPattern;
import jp.mzw.vtr.classify.pattern.limitation.ChangeAPIPattern;
import jp.mzw.vtr.classify.pattern.limitation.ChangeNamePattern;
import jp.mzw.vtr.classify.pattern.limitation.ChangeTestDataPattern;
import jp.mzw.vtr.classify.pattern.limitation.FixTypoPattern;
import jp.mzw.vtr.classify.pattern.limitation.NewTestCasesPattern;
import jp.mzw.vtr.classify.pattern.limitation.RemoveCommentPattern;
import jp.mzw.vtr.classify.pattern.limitation.ReorganizeTestCasesPattern;
import jp.mzw.vtr.classify.pattern.limitation.RevertCommitPattern;
import jp.mzw.vtr.classify.pattern.limitation.SkipTestCasesToRunPattern;
import jp.mzw.vtr.classify.pattern.limitation.UtilityMethodPattern;
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
        if (UseAssertArrayEqualsProperlyPattern.match(testCaseModification)) {
            // XXX: unimplemented due to no data
            return "#2";
        }
        if (UseAssertEqualsProperlyPattern.match(testCaseModification)) {
            return "#3";
        }
        if (UseAssertFalseProperlyPattern.match(testCaseModification)) {
            // XXX: unimplemented due to no data
            return "#4";
        }
        if (UseAssertNotSameProperlyPattern.match(testCaseModification)) {
            return "#5";
        }
        if (UseAssertNullProperlyPattern.match(testCaseModification)) {
            return "#6";
        }
        if (UseAssertTrueProperlyPattern.match(testCaseModification)) {
            // XXX: unimplemented due to no data
            return "#7";
        }
        if (UseFailInsteadOfAssertTruePattern.match(testCaseModification)) {
            // XXX: unimplemented due to no data
            return "#8";
        }
        if (UseStringContainsPattern.match(testCaseModification)) {
            return "#9";
        }
        if (SwapActualExpectedValuePattern.match(testCaseModification)) {
            return "#10";
        }
        if (AssertNotNullToINstancesPattern.match(testCaseModification)) {
            // XXX: unimplemented due to unexpected error
            // Actually, there is a real data for this pattern.
            // https://github.com/apache/commons-pool/blame/5720792c81415909ac7972b050be7254d2e51c08/src/test/java/org/apache/commons/pool2/TestPoolUtils.java#L62
            // However, the commit doesn't exist already.
            // ----------------------------------------------------------------------------------
            // [tsukakei@mbp13-2: commons-pool]$ git log 5720792c81415909ac7972b050be7254d2e51c08
            // fatal: bad object 5720792c81415909ac7972b050be7254d2e51c08
            return "#11";
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
            return "#22";
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
            // XXX: unimplemented due to unexpected error, but there is a commit https://github.com/apache/commons-fileupload/commit/8296784dab7104e30cf1b1527ea20926a33eebcf
            // [tsukakei@mbp13-2: commons-fileupload]$ git log 8296784dab7104e30cf1b1527ea20926a33eebcf
            // fatal: bad object 8296784dab7104e30cf1b1527ea20926a33eebcf
             return "#37";
        }
        if (AddCastToNullPattern.match(testCaseModification)) {
            return "#38";
        }
        if (UseStaticMethodDirectlyPattern.match(testCaseModification)) {
            // TODO: implement
             return "#39";
        }
        if (UseStaticFieldDirectlyPattern.match(testCaseModification)) {
            // TODO: implement
             return "#40";
        }
        if (UsePrimitiveValueMethodPattern.match(testCaseModification)) {
            return "#N1";
        }
        if (UseValueofMethodPattern.match(testCaseModification)) {
            return "#N2";
        }
        if (NewTestCasesPattern.match(testCaseModification)) {
            // TODO: implement
            return "#L1";
        }
        if (SkipTestCasesToRunPattern.match(testCaseModification)) {
            // TODO: implement
             return "#L2";
        }
        if (ReorganizeTestCasesPattern.match(testCaseModification)) {
            // TODO: implement
            return "#L3";
        }
        if (ChangeNamePattern.match(testCaseModification)) {
            // TODO: implement
            return "#L4";
        }
        if (UtilityMethodPattern.match(testCaseModification)) {
            // TODO: implement
            return "#L5";
        }
        if (ChangeTestDataPattern.match(testCaseModification)) {
            // TODO: implement
            return "#L6";
        }
        if (RemoveCommentPattern.match(testCaseModification)) {
            // TODO: implement
            return "#L7";
        }
        if (FixTypoPattern.match(testCaseModification)) {
            // TODO: implement
            return "#L8";
        }
        if (RevertCommitPattern.match(testCaseModification)) {
            return "#L9";
        }
        if (ChangeAPIPattern.match(testCaseModification)) {
            // TODO: implement
            return "#FP1";
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
