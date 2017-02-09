package jp.mzw.vtr.validate.outputs.suppress_warnings.add_suppress_warnings_annotation;

import jp.mzw.vtr.core.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by TK on 2017/02/09.
 */
public class AddSuppressWarningsRawtypesAnnotation extends AddSuppressWarningsAnnotationBase {
    protected static Logger LOGGER = LoggerFactory.getLogger(AddSuppressWarningsRawtypesAnnotation.class);

    public AddSuppressWarningsRawtypesAnnotation(Project project) {
        super(project);
    }

    @Override
    public String SuppressWarning() {
        return "rawtypes";
    }

    @Override
    public boolean targetMessage(String message) {
        return WarningMessage(message) &&
                (message.contains("raw型が見つかりました") || message.contains("found raw type"));
    }
}
