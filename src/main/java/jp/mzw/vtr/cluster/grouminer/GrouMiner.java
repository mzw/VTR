package jp.mzw.vtr.cluster.grouminer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jp.mzw.vtr.CLI;
import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.core.VtrUtils;
import jp.mzw.vtr.detect.Detector;
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
        GrouMiner miner = new GrouMiner(project.getSubjectsDir(), project.getOutputDir());
        List<DetectionResult> results = miner.getDetectionResults();
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
     * Gets detection results
     *
     * @return a list of detection results
     */
    public List<DetectionResult> getDetectionResults() {
        final List<String> subjectList = Lists.newArrayList();
        for (final File subject : this.subjectDir.listFiles()) {
            if (subject.isDirectory()) {
                subjectList.add(subject.getName());
            }
        }
        final List<DetectionResult> results = Lists.newArrayList();
        for (final File subject : this.outputDir.listFiles()) {
            if (subject.isDirectory() && subjectList.contains(subject.getName())) {
                DetectionResult result = new DetectionResult(subject.getName());
                for (final File detect : subject.listFiles()) {
                    if (detect.isDirectory() && detect.getName().equals(Detector.DETECT_DIR)) {
                        final Map<String, List<String>> commits = Maps.newHashMap();
                        for (final File commit : detect.listFiles()) {
                            final List<String> testcases = Lists.newArrayList();
                            for (final File testcase : commit.listFiles()) {
                                final String testcaseName = VtrUtils.getNameWithoutExtension(testcase);
                                testcases.add(testcaseName);
                            }
                            if (!testcases.isEmpty()) {
                                commits.put(commit.getName(), testcases);
                            }
                        }
                        if (!commits.isEmpty()) {
                            result.setResult(commits);
                        }
                    }
                }
                if (result.hasResult()) {
                    results.add(result);
                }
            }
        }
        return results;
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
                // Here

                // Applying groums into "before" version of programs under analysis
                String prevCommit = dict.getPrevCommitBy(commit).getId();
                LOGGER.info("Checkout: " + prevCommit + " previous to " + commit);
                git.checkout(CheckoutConductor.Type.At, prevCommit); // before
                // Here

                // Classifying additive, subtractive, or altering patches
                List<String> testcases = commits.get(commit);
                for (final String testcase : testcases) {
                    final String className = TestCase.getClassName(testcase);
                    final String methodName = TestCase.getMethodName(testcase);
                    // Here
                }
            }
        }
    }

    /**
     * Container of Detection Results of Test Case Modifications across Software Release
     *
     * @author Yuta Maezawa
     */
    public static class DetectionResult {

        /**
         * Represents a project id
         */
        private final String subjectName;

        /**
         * A key is a commit Id and its value(s) are a list of test cases modified at the commit Id
         */
        private Map<String, List<String>> results;

        /**
         * True if any results, otherwise false
         */
        private boolean hasResult;

        /**
         * Constructor: must initially have no detection result
         *
         * @param subjectName represents a project id
         */
        public DetectionResult(final String subjectName) {
            this.subjectName = subjectName;
            this.hasResult = false;
        }

        /**
         * Sets a map value containing detection results.
         *
         * @param results
         */
        public void setResult(final Map<String, List<String>> results) {
            this.hasResult = true;
            this.results = results;
        }

        /**
         * A boolean
         *
         * @return true if any results, otherwise false.
         */
        public boolean hasResult() {
            return this.hasResult;
        }

        /**
         * Gets this project id
         *
         * @return this project id
         */
        public String getSubjectName() {
            return this.subjectName;
        }

        /**
         * Gets detection results
         *
         * @return detection results
         */
        public Map<String, List<String>> getResults() {
            return this.results;
        }

    }
}
