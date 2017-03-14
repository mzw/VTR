package jp.mzw.vtr.command;

import difflib.PatchFailedException;
import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.repair.EvaluatorBase;
import jp.mzw.vtr.repair.RepairEvaluator;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.dom4j.DocumentException;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import static jp.mzw.vtr.CLI.CONFIG_FILENAME;

/**
 * apply and evaluate each patches
 */
public class RepairCommand {
    public static void command(String... args) throws IOException, ParseException, GitAPIException, MavenInvocationException,
    DocumentException, PatchFailedException {
        if (args.length == 1) {
            String projectId = args[1];
            Project project = new Project(projectId).setConfig(CONFIG_FILENAME);
            repair(project);
        } else {
            System.out.println("$ java -cp=<class-path> jp.mzw.vtr.CLI repair <project>");
        }
    }
    private static void repair(Project project)
            throws IOException, ParseException, GitAPIException, MavenInvocationException, DocumentException, PatchFailedException {
        // prepare
        RepairEvaluator evaluator = new RepairEvaluator(project).parse();
        List<EvaluatorBase> evaluators = EvaluatorBase.getEvaluators(project);
        // evaluate
        evaluator.evaluate(evaluators);
    }
}
