package jp.mzw.vtr.validate.outputs;

import jp.mzw.vtr.core.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by TK on 2017/02/08.
 */
public class AddSuppressWarningsDeprecatedAnnotations extends AddSuppressWarningsAnnotations {
    protected static Logger LOGGER = LoggerFactory.getLogger(AddSuppressWarningsDeprecatedAnnotations.class);

    public AddSuppressWarningsDeprecatedAnnotations(Project project) {
        super(project);
    }

    @Override
    public String SuppressWarning() {
        return "deprecated";
    }

    @Override
    public boolean targetMessage(String message) {
        return WarningMessage(message) && (message.contains("deprecated") || message.contains("非推奨"));
    }

}
