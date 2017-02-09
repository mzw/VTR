package jp.mzw.vtr.validate.suppress_warnings.add_suppress_warnings_annotation;

import jp.mzw.vtr.core.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by TK on 2017/02/08.
 */
public class AddSuppressWarningsUncheckedAnnotation extends AddSuppressWarningsAnnotationBase {
    protected static Logger LOGGER = LoggerFactory.getLogger(AddSuppressWarningsUncheckedAnnotation.class);

    public AddSuppressWarningsUncheckedAnnotation(Project project) {
        super(project);
    }

    @Override
    public String SuppressWarning() {
        return "unchecked";
    }

    @Override
    public boolean targetMessage(String message) {
        return WarningMessage(message) && (message.contains("unchecked") || message.contains("無検査"));
    }
}
