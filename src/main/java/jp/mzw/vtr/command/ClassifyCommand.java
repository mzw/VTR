package jp.mzw.vtr.command;

import jp.mzw.vtr.CLI;
import jp.mzw.vtr.classify.Classifier;
import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.experiment.ClassifyExperiment;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;


public class ClassifyCommand {
    public static void command(String... args) throws IOException, GitAPIException {
        if (args.length == 0) {
            classify(new Project(null).setConfig(CLI.CONFIG_FILENAME));
        } else if (args.length == 1) {
            classify(new Project(null).setConfig(CLI.CONFIG_FILENAME), true);
        } else {
            System.out.println("$ java -cp=<class-path> jp.mzw.vtr.CLI classify <isPrintScore>");
        }
    }
    private static void classify(Project project) throws IOException, GitAPIException{
        classify(project, false);
    }

    private static void classify(Project project, boolean printScore) throws IOException, GitAPIException{
        new Classifier(project).output();
        if (printScore) {
            System.out.println("----------------- Score Report ---------------------");
            ClassifyExperiment.experiment(project);
        }
    }
}
