package jp.mzw.vtr.validate.outputs;

import jp.mzw.vtr.core.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by TK on 2017/02/09.
 */
public class AddSuppressWarningsRawtypesAnnotations extends AddSuppressWarningsAnnotations {
    protected static Logger LOGGER = LoggerFactory.getLogger(AddSuppressWarningsRawtypesAnnotations.class);

    public AddSuppressWarningsRawtypesAnnotations(Project project) {
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
