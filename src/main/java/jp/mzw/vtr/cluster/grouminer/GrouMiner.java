package jp.mzw.vtr.cluster.grouminer;

import com.hp.gagawa.java.elements.S;
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

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

public class GrouMiner {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrouMiner.class);

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

    /** A engine to invoke GrouMiner and get patch patterns */
    private IntegratedGrouMinerEngine grouMinerEngine;

    /**
     * Constructor
     *
     * @param subjectDir is a directory containing projects under analysis
     * @param outputDir is a directory to output analysis results
     */
    public GrouMiner(final File subjectDir, final File outputDir) {
        this.subjectDir = subjectDir;
        this.outputDir = outputDir;
        grouMinerEngine = new IntegratedGrouMinerEngine(subjectDir, outputDir);
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
        for (final DetectionResult result : results) {
            final String subjectName = result.getSubjectName();
            LOGGER.info("Subject: " + subjectName);

            final Project project = new Project(subjectName).setConfig(CLI.CONFIG_FILENAME);
            final CheckoutConductor git = new CheckoutConductor(project);
            final Dictionary dict = new Dictionary(this.outputDir, subjectName).parse().createPrevCommitByCommitIdMap();

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
//                        System.out.println("PrevCommit: " + prevCommit);
//                        System.out.println("CurCommit: " + commit);
//                        System.out.println("Class Name: " + className);
//                        System.out.println("Method Name: " + methodName);
                    }
                    // output
                }
            }
        }
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
        Other
    }
}
