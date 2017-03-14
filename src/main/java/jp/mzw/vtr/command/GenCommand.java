package jp.mzw.vtr.command;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.git.CheckoutConductor;
import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.validate.ValidationResult;
import jp.mzw.vtr.validate.ValidatorBase;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.List;

import static jp.mzw.vtr.CLI.CONFIG_FILENAME;

/**
 * generate test case modification patches
 */
public class GenCommand {
    public static Logger LOGGER = LoggerFactory.getLogger(GenCommand.class);

    public static void command(String... args) throws IOException, ParseException, GitAPIException, InstantiationException, IllegalAccessException,
    IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException {
        if (args.length == 1) {
            String projectId = args[1];
            Project project = new Project(projectId).setConfig(CONFIG_FILENAME);
            gen(project);
        } else {
            System.out.println("$ java -cp=<class-path> jp.mzw.vtr.CLI gen <project>");
        }
    }
    private static void gen(Project project) throws IOException, ParseException, GitAPIException, InstantiationException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException {
        List<ValidatorBase> validators = ValidatorBase.getValidators(project, ValidatorBase.VALIDATORS_LIST);
        String curCommitId = null;
        for (ValidatorBase validator : validators) {
            List<ValidationResult> results = ValidatorBase.parse(project);
            for (ValidationResult result : results) {
                if (!validator.getClass().getName().equals(result.getValidatorName())) {
                    continue;
                }
                if (new Boolean(false).equals(result.isTruePositive())) {
                    LOGGER.info("Skip due to false-positive: {}#{} @ {} by {}", result.getTestCaseClassName(), result.getTestCaseMathodName(),
                            result.getCommitId(), result.getValidatorName());
                    continue;
                } else if (result.isTruePositive() == null) {
                    LOGGER.info("Check whether true-positive or not: {}#{} @ {} by {}", result.getTestCaseClassName(), result.getTestCaseMathodName(),
                            result.getCommitId(), result.getValidatorName());
                    continue;
                }
                String commitId = result.getCommitId();
                if (!commitId.equals(curCommitId)) {
                    new CheckoutConductor(project).checkout(new Commit(commitId, null));
                    curCommitId = commitId;
                }
                validator.generate(result);
            }
        }
    }
}
