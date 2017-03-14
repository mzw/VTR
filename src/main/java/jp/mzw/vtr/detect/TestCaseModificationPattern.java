package jp.mzw.vtr.detect;

/**
 * Created by TK on 3/10/17.
 */
public class TestCaseModificationPattern {
    private String id;
    private String name;
    private String validatorName;
    private String aliasId;

    private TestCaseModificationPattern(String id, String name, String validatorName, String aliasId) {
        this.id = id;
        this.name = name;
        this.validatorName = validatorName;
        this.aliasId = aliasId;
    }

    public String getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public String getValidatorName() {
        return validatorName;
    }
    public String getAliasId() {
        return aliasId;
    }

}
