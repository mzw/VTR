package jp.mzw.vtr.validate.outputs;

import jp.mzw.vtr.core.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by TK on 2017/02/08.
 */
public class AddSuppressWarningsUncheckedAnnotations extends AddSuppressWarningsAnnotations {
    protected static Logger LOGGER = LoggerFactory.getLogger(AddSuppressWarningsUncheckedAnnotations.class);

    public AddSuppressWarningsUncheckedAnnotations(Project project) {
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
