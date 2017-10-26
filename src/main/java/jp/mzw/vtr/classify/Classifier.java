package jp.mzw.vtr.classify;

import jp.mzw.vtr.classify.pattern.UpgradeJUnit;
import jp.mzw.vtr.classify.pattern.limitation.ReorganizeTestCases;
import jp.mzw.vtr.cluster.similarity.LcsAnalyzer;
import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.detect.TestCaseModification;
import jp.mzw.vtr.classify.pattern.FormatSourceCode;
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
        if (UpgradeJUnit.match(testCaseModification)) {
            return "#1";
        }
        if (FormatSourceCode.match(testCaseModification)) {
            return "#28";
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
