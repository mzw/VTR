package jp.mzw.vtr.command;

import jp.mzw.vtr.CLI;
import jp.mzw.vtr.cluster.HCluster;
import jp.mzw.vtr.cluster.grouminer.GrouMiner;
import jp.mzw.vtr.cluster.gumtreediff.GumTreeDiff;
import jp.mzw.vtr.cluster.similarity.DistAnalyzer;
import jp.mzw.vtr.cluster.similarity.DistMap;
import jp.mzw.vtr.cluster.testedness.ResultAnalyzer;
import jp.mzw.vtr.cluster.testedness.Testedness;
import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.detect.DetectionResult;
import jp.mzw.vtr.detect.Detector;
import jp.mzw.vtr.detect.TestCaseModification;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import static jp.mzw.vtr.CLI.CONFIG_FILENAME;

/**
 * cluster commits including test case modifications for previously released source programns
 */
public class ClusterCommand {
    public static void command(String... args) throws IOException, ParseException, NoHeadException, GitAPIException {
        if (args.length == 1) {
            String mode = args[0];
            if (mode.equals("grouminer")) {
                grouminer();
            } else if (mode.equals("gumtreediff")) {
                gumtreediff();
            } else if (mode.equals("testedness")) {
                testedness();
            } else if (mode.equals("add-pattens-for-testedness")) {
                Project project = new Project(null).setConfig(CLI.CONFIG_FILENAME);
                ResultAnalyzer.analyze(project.getSubjectsDir(), project.getOutputDir());
            }

        } else if (args.length == 4) {
            String analyzer = args[1];
            String strategy = args[2];
            double threshold = Double.parseDouble(args[3]);
            cluster(new Project(null).setConfig(CONFIG_FILENAME), analyzer, strategy, threshold);
        } else {
            System.out.println("$ java -cp=<class-path> jp.mzw.vtr.CLI cluster <similarity> <cluster-method> <threshold>");
            System.out.println("$ java -cp=<class-path> jp.mzw.vtr.CLI cluster <mode: e.g., grouminer>");
        }
    }
    private static void cluster(Project project, String analyzer, String strategy, double threshold)
            throws IOException, ParseException, NoHeadException, GitAPIException {
        // Similarity
        DistAnalyzer distAnalyzer = DistAnalyzer.analyzerFactory(project.getOutputDir(), analyzer);
        List<TestCaseModification> tcmList = distAnalyzer.parseTestCaseModifications();
        DistMap map = distAnalyzer.analyze(tcmList);
        String timestamp = distAnalyzer.output(map);
        // Clustering
        HCluster cluster = new HCluster(project.getOutputDir(), distAnalyzer.getMethodName()).parse();
        cluster.cluster(HCluster.getStrategy(strategy), threshold);
        cluster.output(timestamp);
    }

    private static void grouminer() throws IOException, GitAPIException, ParseException {
        Project project = new Project(null).setConfig(CLI.CONFIG_FILENAME);
        List<DetectionResult> results = Detector.getDetectionResults(project.getSubjectsDir(), project.getOutputDir());
        GrouMiner miner = new GrouMiner(project.getSubjectsDir(), project.getOutputDir());
        miner.apply(results);
    }

    private static void gumtreediff() throws IOException, GitAPIException, ParseException {
        Project project = new Project(null).setConfig(CLI.CONFIG_FILENAME);
        List<DetectionResult> results = Detector.getDetectionResults(project.getSubjectsDir(), project.getOutputDir());
        GumTreeDiff differ = new GumTreeDiff(project.getSubjectsDir(), project.getOutputDir());
        differ.run(results);
    }

    private static void testedness() throws IOException, GitAPIException, ParseException {
        Project project = new Project(null).setConfig(CLI.CONFIG_FILENAME);
        List<DetectionResult> results = Detector.getDetectionResults(project.getSubjectsDir(), project.getOutputDir());
        Testedness testedness = new Testedness(project.getSubjectsDir(), project.getOutputDir());
        testedness.run(results);
    }
}
