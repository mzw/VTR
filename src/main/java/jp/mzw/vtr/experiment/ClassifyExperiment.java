package jp.mzw.vtr.experiment;

import jp.mzw.vtr.classify.Classifier;
import jp.mzw.vtr.core.Project;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassifyExperiment {
    static Logger LOGGER = LoggerFactory.getLogger(ClassifyExperiment.class);

    static final String CLASSIFY_ANSWER = "src/main/resources/training-data.csv";
    static final String[] KNOWN_PATTERNS = {
            "#1", "#2", "#3", "#4", "#5", "#6", "#7", "#8", "#9", "#10",
            "#11", "#12", "#13", "#14", "#15", "#16", "#17", "#18", "#19", "#20",
            "#21", "#22", "#23", "#24", "#25", "#26", "#27", "#28", "#29", "#30",
            "#31", "#32", "#33", "#34", "#35", "#36", "#37", "#38", "#39", "#40",
            "#41", "#42",
            "#L1", "#L2", "#L3", "#L4", "#L5", "#L6", "#L7", "#L8", "#L9",
            "#FP1",
            "Nan", "NullAST", "NullCommit"
    };

    static final String TRUE_POSITIVE  = ": true positive";
    static final String FALSE_POSITIVE = ": false positive";
    static final String FALSE_NEGATIVE = ": false negative";

    public static void experiment(Project project) {
        // Id, Subject(Project), Commit, Class, Method, Item
        List<CSVRecord> answers = getCsvRecords(Paths.get(CLASSIFY_ANSWER));
        // Subject(Project), Commit, Class, Method, Item
        List<CSVRecord> results = getCsvRecords(Paths.get(String.join("/",
                project.getOutputDir().getName(), Classifier.CLASSIFY_DIR, Classifier.CLASSIFY_FILE)));
        Map<String, Integer> counts = new HashMap<>();
        for (CSVRecord result : results) {
            for (CSVRecord answer : answers) {
                if (!(answer.get(1).equals(result.get(0))
                        && answer.get(2).equals(result.get(1))
                        && answer.get(3).equals(result.get(2))
                        && answer.get(4).equals(result.get(3))
                )) {
                    continue;
                }
                if (answer.get(5).equals(result.get(4))) {
                    String truePositive = answer.get(5) + TRUE_POSITIVE;
                    counts.merge(truePositive, 1, (v1, v2) -> v1 + v2);
                } else {
//                    System.out.print("Answer(5): " + answer.get(5));
                    String falseNegative = answer.get(5) + FALSE_NEGATIVE;
                    counts.merge(falseNegative, 1, (v1, v2) -> v1 + v2);
//                    System.out.print("Result(4): " + result.get(4));
                    String falsePositive = result.get(4) + FALSE_POSITIVE;
                    counts.merge(falsePositive, 1, (v1, v2) -> v1 + v2);
                }
            }
        }

        for (String pattern : KNOWN_PATTERNS) {
            int cntTP = (counts.get(pattern + TRUE_POSITIVE)  == null) ? 0 : counts.get(pattern + TRUE_POSITIVE);
            int cntFP = (counts.get(pattern + FALSE_POSITIVE) == null) ? 0 : counts.get(pattern + FALSE_POSITIVE);
            int cntFN = (counts.get(pattern + FALSE_NEGATIVE) == null) ? 0 : counts.get(pattern + FALSE_NEGATIVE);
            double precision = cntTP / ((double) (cntTP + cntFP));
            double recall    = cntTP / ((double) (cntTP + cntFN));
            double Fmeasure  = 2 * precision * recall / (precision + recall);

            StringBuilder sb = new StringBuilder();
            sb.append("Pattern: ").append(pattern)
                    .append(", Precision: ").append(String.format("%.2f", precision))
                    .append(", Recall: ").append(String.format("%.2f", recall))
                    .append(", F-Measure: ").append(String.format("%.2f", Fmeasure))
                    .append(", True positive: ").append(String.format("%2d", cntTP))
                    .append(", False positive: ").append(String.format("%2d", cntFP))
                    .append(", False negative: ").append(String.format("%2d", cntFN));
            System.out.println(sb.toString());
        }
    }

    protected static List<CSVRecord> getCsvRecords(Path path) {
        List<CSVRecord> records = null;
        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            CSVParser parser = CSVFormat.DEFAULT.parse(br);
            records = parser.getRecords();
        } catch (IOException e) {
            LOGGER.error("can't get csv records @{}", path);
        }
        return records;
    }
}
