package jp.mzw.vtr.validate.outputs.suppress_warnings.add_suppress_warnings_annotation;

import jp.mzw.vtr.core.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by TK on 2017/02/08.
 */
public class AddSuppressWarningsDeprecationAnnotation extends AddSuppressWarningsAnnotationBase {
    protected static Logger LOGGER = LoggerFactory.getLogger(AddSuppressWarningsDeprecationAnnotation.class);

    public AddSuppressWarningsDeprecationAnnotation(Project project) {
        super(project);
    }

    @Override
    public String SuppressWarning() {
        return "deprecation";
    }

    @Override
    public boolean targetMessage(String message) {
        return WarningMessage(message) && (message.contains("deprecated") || message.contains("非推奨"));
    }

}
