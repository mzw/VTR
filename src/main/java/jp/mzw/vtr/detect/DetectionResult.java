package jp.mzw.vtr.detect;

import java.util.List;
import java.util.Map;

/**
 * Contains Detection Results of Test Case Modifications across Software Release
 *
 * @author Yuta Maezawa
 */
public class DetectionResult {

    /**
     * Represents a project id
     */
    private final String subjectName;

    /**
     * A key is a commit Id and its value(s) are a list of test cases modified at the commit Id
     */
    private Map<String, List<String>> results;

    /**
     * True if any results, otherwise false
     */
    private boolean hasResult;

    /**
     * Constructor: must initially have no detection result
     *
     * @param subjectName represents a project id
     */
    public DetectionResult(final String subjectName) {
        this.subjectName = subjectName;
        this.hasResult = false;
    }

    /**
     * Sets a map value containing detection results.
     *
     * @param results
     */
    public void setResult(final Map<String, List<String>> results) {
        this.hasResult = true;
        this.results = results;
    }

    /**
     * A boolean
     *
     * @return true if any results, otherwise false.
     */
    public boolean hasResult() {
        return this.hasResult;
    }

    /**
     * Gets this project id
     *
     * @return this project id
     */
    public String getSubjectName() {
        return this.subjectName;
    }

    /**
     * Gets detection results
     *
     * @return detection results
     */
    public Map<String, List<String>> getResults() {
        return this.results;
    }

}
