package jp.mzw.vtr.command;

import jp.mzw.vtr.cluster.HCluster;
import jp.mzw.vtr.cluster.similarity.DistAnalyzer;
import jp.mzw.vtr.cluster.similarity.DistMap;
import jp.mzw.vtr.core.Project;
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
        if (args.length == 3) {
            String analyzer = args[0];
            String strategy = args[1];
            double threshold = Double.parseDouble(args[2]);
            cluster(new Project(null).setConfig(CONFIG_FILENAME), analyzer, strategy, threshold);
        } else {
            System.out.println("$ java -cp=<class-path> jp.mzw.vtr.CLI cluster <similarity> <cluster-method> <threshold>");
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
}
