package jp.mzw.vtr.cluster.testedness;

import jp.mzw.vtr.cluster.gumtreediff.GumTreeDiff;
import jp.mzw.vtr.core.VtrUtils;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static jp.mzw.vtr.cluster.testedness.Testedness.TESTEDNESS_DIR;

/**
 * Class for adding manual defined patterns to testedness classifying results.
 * This class requires testedness classifying results and your manual inspection results.
 */
public class ResultAnalyzer {
    private static Logger LOGGER = LoggerFactory.getLogger(ResultAnalyzer.class);
    private static final String PATTERN_DIR = "pattern";


    public static void analyze(File subjectDir, File outputDir) {
        Testedness testedness = new Testedness(subjectDir, outputDir);
        // get testedness results
        Map<String, List<CSVRecord>> testednessResult = new HashMap<>();
        for (Testedness.Type type : Testedness.Type.values()) {
            String key = type.name();
            List<CSVRecord> records = VtrUtils.getCsvRecords(testedness.getPathToOutputFile(type));
            testednessResult.put(key, records);
        }
        // get gumtreediff results
        Map<String, List<CSVRecord>> gumtreediffResult = new HashMap<>();
        for (GumTreeDiff.Type type : GumTreeDiff.Type.values()) {
            String key = type.name();
            List<CSVRecord> records = VtrUtils.getCsvRecords(
                    Paths.get(ResultAnalyzer.class.getClassLoader().getResource(type.name() + ".csv").getPath())
            );
            gumtreediffResult.put(key, records);
        }
        // add patterns
        for (Testedness.Type type : Testedness.Type.values()) {
            List<CSVRecord> records = testednessResult.get(type.name());
            StringBuilder sb = new StringBuilder();
            for (CSVRecord record : records) {
                String projectId  = record.get(0);
                String commitId   = record.get(2);
                String className  = record.get(3);
                String methodName = record.get(4);
                for (GumTreeDiff.Type gumType : GumTreeDiff.Type.values()) {
                    List<CSVRecord> gumRecords = gumtreediffResult.get(gumType.name());
                    if (gumRecords.isEmpty()) {
                        continue;
                    }
                    for (CSVRecord gumRecord : gumRecords) {
                        String gumProjectId  = gumRecord.get(0);
                        String gumCommitId   = gumRecord.get(2);
                        String gumClassName  = gumRecord.get(3);
                        String gumMethodName = gumRecord.get(4);
                        if (projectId.equals(gumProjectId) && commitId.equals(gumCommitId)
                                && className.equals(gumClassName) && methodName.equals(gumMethodName)) {
                            String pattern = gumRecord.get(5);
                            VtrUtils.addCsvRecords(sb, projectId, commitId, className, methodName, pattern);
                        }
                    }
                }
            }
            Path pathToOutput = getPathToOutput(outputDir, type);
            VtrUtils.writeContent(pathToOutput, sb.toString());
        }
    }

    private static Path getPathToOutput(File outputDir, Testedness.Type type) {
        return VtrUtils.getPathToFile(outputDir.getPath(), TESTEDNESS_DIR, PATTERN_DIR, type.name() + ".csv");
    }
}
