package jp.mzw.vtr.cluster.grouminer;

import jp.mzw.vtr.CLI;
import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.detect.Detector;
import jp.mzw.vtr.detect.DetectionResult;
import jp.mzw.vtr.dict.Dictionary;
import jp.mzw.vtr.git.CheckoutConductor;
import jp.mzw.vtr.maven.TestCase;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

public class GrouMiner {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrouMiner.class);

    protected static final String GROUM_OUTPUT_DIR = "grouminer";

    public static void main(String[] args) throws IOException, GitAPIException, ParseException {
        Project project = new Project(null).setConfig(CLI.CONFIG_FILENAME);
        List<DetectionResult> results = Detector.getDetectionResults(project.getSubjectsDir(), project.getOutputDir());
        GrouMiner miner = new GrouMiner(project.getSubjectsDir(), project.getOutputDir());
        miner.apply(results);
    }

    /** A directory containing projects under analysis */
    private final File subjectDir;

    /** A directory to output analysis results */
    private final File outputDir;

    /**
     * Constructor
     *
     * @param subjectDir is a directory containing projects under analysis
     * @param outputDir is a directory to output analysis results
     */
    public GrouMiner(final File subjectDir, final File outputDir) {
        this.subjectDir = subjectDir;
        this.outputDir = outputDir;
    }

    /**
     * Applies GrouMiner and classifies patches
     *
     * @param results
     * @throws IOException
     * @throws ParseException
     * @throws GitAPIException
     */
    public void apply(final List<DetectionResult> results) throws IOException, ParseException, GitAPIException {
        // StringBuilders to contain results
        StringBuilder additiveSb    = new StringBuilder();
        StringBuilder subtractiveSb = new StringBuilder();
        StringBuilder alteringSb    = new StringBuilder();
        StringBuilder noneSb        = new StringBuilder();
        for (final DetectionResult result : results) {
            final String subjectName = result.getSubjectName();
            LOGGER.info("Subject: " + subjectName);

            final Project project = new Project(subjectName).setConfig(CLI.CONFIG_FILENAME);
            final CheckoutConductor git = new CheckoutConductor(project);
            final Dictionary dict = new Dictionary(this.outputDir, subjectName).parse().createPrevCommitByCommitIdMap();

            final IntegratedGrouMinerEngine grouMinerEngine = new IntegratedGrouMinerEngine(subjectName,
                    project.getProjectDir(), project.getOutputDir());

            final Map<String, List<String>> commits = result.getResults();
            for (final String commit : commits.keySet()) {
                // Applying groums into "after" version of programs under analysis
                LOGGER.info("Checkout: " + commit);
                git.checkout(CheckoutConductor.Type.At, commit); // after
                grouMinerEngine.createDotFiles(commit);
                // Applying groums into "before" version of programs under analysis
                String prevCommit = dict.getPrevCommitBy(commit).getId();
                LOGGER.info("Checkout: " + prevCommit + " previous to " + commit);
                git.checkout(CheckoutConductor.Type.At, prevCommit); // before
                grouMinerEngine.createDotFiles(prevCommit);
                // Classifying additive, subtractive, or altering patches
                List<String> testcases = commits.get(commit);
                for (final String testcase : testcases) {
                    final String className = TestCase.getClassName(testcase);
                    final String methodName = TestCase.getMethodName(testcase);
                    PatchPattern pattern = grouMinerEngine.compareGroums(prevCommit, commit, className, methodName);
                    if (pattern.equals(PatchPattern.Additive)) {
                        generateContent(additiveSb, subjectName, commit, className, methodName);
                    } else if (pattern.equals(PatchPattern.Subtractive)) {
                        generateContent(subtractiveSb, subjectName, commit, className, methodName);
                    } else if (pattern.equals(PatchPattern.Altering)) {
                        generateContent(alteringSb, subjectName, commit, className, methodName);
                    } else if (pattern.equals(PatchPattern.None)) {
                        generateContent(noneSb, subjectName, commit, className, methodName);
                    }
                }
            }
        }
        // output
        outputAddictive(additiveSb.toString());
        outputSubtractive(subtractiveSb.toString());
        outputAltering(alteringSb.toString());
        outputNone(noneSb.toString());
    }
    /**
     * Class to classify patch patterns
     * defined in "Automatic Patch Generation Learned from Human-Written Patches", ICSE 2013
     *
     * @author Keita Tsukamoto
     */
    enum PatchPattern {
        Additive,    // A patch which inserts new semantic features such as new control flows
        Subtractive, // A patch which removes semantic features
        Altering,    // A patch which changes control flows by replacing semantic features
        None
    }

    /* To output results */
    private void outputAddictive(String content) {
        if (!Files.exists(getPathToOutputAdditive())) {
            try {
                Files.createDirectories(getPathToOutputDir());
                Files.createFile(getPathToOutputAdditive());
            } catch (IOException e) {
                e.printStackTrace();
                LOGGER.error(e.getMessage());
            }
        }
        try (BufferedWriter bw = Files.newBufferedWriter(getPathToOutputAdditive())) {
            bw.write(content);
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
        }
    }
    private void outputSubtractive(String content) {
        if (!Files.exists(getPathToOutputSubtractive())) {
            try {
                Files.createDirectories(getPathToOutputDir());
                Files.createFile(getPathToOutputSubtractive());
            } catch (IOException e) {
                e.printStackTrace();
                LOGGER.error(e.getMessage());
            }
        }
        try (BufferedWriter bw = Files.newBufferedWriter(getPathToOutputSubtractive())) {
            bw.write(content);
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
        }
    }
    private void outputAltering(String content) {
        if (!Files.exists(getPathToOutputAltering())) {
            try {
                Files.createDirectories(getPathToOutputDir());
                Files.createFile(getPathToOutputAltering());
            } catch (IOException e) {
                e.printStackTrace();
                LOGGER.error(e.getMessage());
            }
        }
        try (BufferedWriter bw = Files.newBufferedWriter(getPathToOutputAltering())) {
            bw.write(content);
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
        }
    }
    private void outputNone(String content) {
        if (!Files.exists(getPathToOutputNone())) {
            try {
                Files.createDirectories(getPathToOutputDir());
                Files.createFile(getPathToOutputNone());
            } catch (IOException e) {
                e.printStackTrace();
                LOGGER.error(e.getMessage());
            }
        }
        try (BufferedWriter bw = Files.newBufferedWriter(getPathToOutputNone())) {
            bw.write(content);
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
        }
    }
    /* To generate content */
    private void generateContent(StringBuilder sb, String projectId, String commit, String className, String methodName) {
        sb.append(projectId).append(",");
        sb.append(commit).append(",");
        sb.append(className).append(",");
        sb.append(methodName).append("\n");
//        sb.append(generateUrlForManualChecking(projectId, commit, className, methodName)).append("\n");
    }
    /* To generate html */
    private String generateUrlForManualChecking(String projectId, String commit, String className, String methodName) {
        String prefix = "http://mzw.jp/yuta/research/vtr/results/tmp/20161212/visual/html/origin/";
        String htmlName = String.join(":", projectId, commit, className, methodName) + ".html";
        return prefix + htmlName;
    }
    /* To get path to output */
    private Path getPathToOutputAdditive() {
        return getPathToOutputFile(PatchPattern.Additive);
    }
    private Path getPathToOutputSubtractive() {
        return getPathToOutputFile(PatchPattern.Subtractive);
    }
    private Path getPathToOutputAltering() {
        return getPathToOutputFile(PatchPattern.Altering);
    }
    private Path getPathToOutputNone() {
        return getPathToOutputFile(PatchPattern.None);
    }
    private Path getPathToOutputFile(PatchPattern pattern) {
        String filename = "";
        if (pattern.equals(PatchPattern.Additive)) {
            filename = "additive";
        } else if (pattern.equals(PatchPattern.Subtractive)){
            filename = "subtractive";
        } else if (pattern.equals(PatchPattern.Altering)) {
            filename = "altering";
        } else if (pattern.equals(PatchPattern.None)) {
            filename = "none";
        }
        return Paths.get(String.join("/", getPathToOutputDir().toString(), filename + ".csv"));
    }
    private Path getPathToOutputDir() {
        return Paths.get(String.join("/", outputDir.getPath(), GROUM_OUTPUT_DIR));
    }

}
