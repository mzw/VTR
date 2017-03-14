package jp.mzw.vtr.command;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by TK on 3/10/17.
 */
public class CommandUtils {

    public static final String CONFIG_FILENAME = "config.properties";

    public static Map<CSVRecord, Double> improvedMutationRecords(List<CSVRecord> records) {
        Map<CSVRecord, Double> ret = new HashMap<>();
        for (CSVRecord record : records) {
            if (!record.get(4).equals("Improved")) {
                continue;
            }
            double beforeScore = Double.parseDouble(record.get(5));
            double afterScore = Double.parseDouble(record.get(6));
            double improveRate;
            //improveRate = (beforeScore != 0) ? (afterScore - beforeScore) / beforeScore * 100 : 100;
            improveRate = afterScore - beforeScore;
            ret.put(record, improveRate);
        }
        return ret;
    }

    public static Map<CSVRecord, Double> improvedReadabilityRecords(List<CSVRecord> records) {
        Map<CSVRecord, Double> ret = new HashMap<>();
        for (CSVRecord record : records) {
            if (!record.get(4).equals("Improved")) {
                continue;
            }
            double beforeScore = Double.parseDouble(record.get(5));
            double afterScore = Double.parseDouble(record.get(7));
            double improveRate;
            improveRate = (beforeScore != 0) ? (afterScore - beforeScore) / beforeScore * 100 : 100;
            ret.put(record, improveRate);
        }
        return ret;
    }

    public static Map<CSVRecord, Pair<Double, Double>> improvedPerformanceRecords(List<CSVRecord> records) {
        Map<CSVRecord, Pair<Double, Double>> ret = new HashMap<>();
        for (CSVRecord record : records) {
            if (!(record.get(4).equals("Improved") || record.get(4).equals("PartiallyImproved"))) {
                continue;
            }
            double beforeElapsedTime = Double.parseDouble(record.get(5));
            double afterElapsedTime = Double.parseDouble(record.get(7));
            double beforeUsedMemory = Double.parseDouble(record.get(6));
            double afterUsedMemory = Double.parseDouble(record.get(8));
            double elapsedTimeImproveRate;
            elapsedTimeImproveRate = (beforeElapsedTime != 0) ? (beforeElapsedTime - afterElapsedTime) / beforeElapsedTime * 100 : 100;
            double usedMemoryImproveRate;
            usedMemoryImproveRate = (beforeUsedMemory != 0) ? (beforeUsedMemory - afterUsedMemory) / beforeUsedMemory * 100 : 100;
            ret.put(record, new ImmutablePair<>(elapsedTimeImproveRate, usedMemoryImproveRate));
        }
        return ret;
    }

    public static Map<CSVRecord, Pair<Double, Double>> partiallyImprovedPerformanceRecords(List<CSVRecord> records) {
        Map<CSVRecord, Pair<Double, Double>> ret = new HashMap<>();
        for (CSVRecord record : records) {
            if (!record.get(4).equals("PartiallyImproved")) {
                continue;
            }
            double beforeElapsedTime = Double.parseDouble(record.get(5));
            double afterElapsedTime = Double.parseDouble(record.get(7));
            double beforeUsedMemory = Double.parseDouble(record.get(6));
            double afterUsedMemory = Double.parseDouble(record.get(8));
            double elapsedTimeImproveRate;
            elapsedTimeImproveRate = (beforeElapsedTime != 0) ? (beforeElapsedTime - afterElapsedTime) / beforeElapsedTime * 100 : 100;
            double usedMemoryImproveRate;
            usedMemoryImproveRate = (beforeUsedMemory != 0) ? (beforeUsedMemory - afterUsedMemory) / beforeUsedMemory * 100 : 100;
            ret.put(record, new ImmutablePair<>(elapsedTimeImproveRate, usedMemoryImproveRate));
        }
        return ret;
    }

    public static Map<CSVRecord, Double> improvedCompileOutputRecords(List<CSVRecord> records) {
        Map<CSVRecord, Double> ret = new HashMap<>();
        for (CSVRecord record : records) {
            if (!record.get(4).equals("Improved")) {
                continue;
            }
            double beforeScore = Double.parseDouble(record.get(5));
            double afterScore = Double.parseDouble(record.get(7));
            double improveRate;
            improveRate = (beforeScore != 0) ? (beforeScore - afterScore) / beforeScore * 100 : 100;
            ret.put(record, improveRate);
        }
        return ret;
    }

    public static Map<CSVRecord, Double> improvedJavadocOutputRecords(List<CSVRecord> records) {
        Map<CSVRecord, Double> ret = new HashMap<>();
        for (CSVRecord record : records) {
            if (!record.get(4).equals("Improved")) {
                continue;
            }
            double beforeScore = Double.parseDouble(record.get(5));
            double afterScore = Double.parseDouble(record.get(6));
            double improveRate;
            improveRate = (beforeScore != 0) ? (beforeScore - afterScore) / beforeScore * 100 : 100;
            ret.put(record, improveRate);
        }
        return ret;
    }

    public static Map<CSVRecord, Double> improvedRuntimeOutputRecords(List<CSVRecord> records) {
        Map<CSVRecord, Double> ret = new HashMap<>();
        for (CSVRecord record : records) {
            if (!record.get(4).equals("Improved")) {
                continue;
            }
            double beforeScore = Double.parseDouble(record.get(5));
            double afterScore = Double.parseDouble(record.get(6));
            double improveRate;
            improveRate = (beforeScore != 0) ? (beforeScore - afterScore) / beforeScore * 100 : 100;
            ret.put(record, improveRate);
        }
        return ret;
    }

    public static String patternFromId(String patternId) {
        if (patternId.equals("#1")) {
            return "AddTestAnnotations";
        } else if (patternId.equals("#2")) {
            return "UseAssertArrayEqualsProperly";
        } else if (patternId.equals("#3")) {
            return "UseAssertEqualsProperly";
        } else if (patternId.equals("#4")) {
            return "UseAssertFalseProperly";
        } else if (patternId.equals("#5")) {
            return "UseAssertNotSameProperly";
        } else if (patternId.equals("#6")) {
            return "UseAssertNullProperly";
        } else if (patternId.equals("#7")) {
            return "UseAssertTrueProperly";
        } else if (patternId.equals("#8")) {
            return "UseFailInsteadOfAssertTrueFalse";
        } else if (patternId.equals("#9")) {
            return "UseStringContains";
        } else if (patternId.equals("#10")) {
            return "SwapActualExpectedValues";
        } else if (patternId.equals("#11")) {
            return "AssertNotNullToInstances";
        } else if (patternId.equals("#12")) {
            return "ModifyAssertImports";
        } else if (patternId.equals("#13")) {
            return "DoNotSwallowTestErrorsSiliently";
        } else if (patternId.equals("#14")) {
            return "AddFailStatementsForHandlingExpectedExceptions";
        } else if (patternId.equals("#15")) {
            return "HandleExpectedExecptionsProperly";
        } else if (patternId.equals("#16")) {
            return "RemoveUnusedExceptions";
        } else if (patternId.equals("#17")) {
            return "CloseResources";
        } else if (patternId.equals("#18")) {
            return "UseTryWithResources";
        } else if (patternId.equals("#19")) {
            return "UseProcessWaitfor";
        } else if (patternId.equals("#20")) {
            return "AddSuppressWarnings";
        } else if (patternId.equals("#21")) {
            return "DeleteUnnecessaryAssignmenedVariables";
        } else if (patternId.equals("#22")) {
            return "IntroduceAutoBoxing";
        } else if (patternId.equals("#23")) {
            return "RemoveUnnecessaryCasts";
        } else if (patternId.equals("#24")) {
            return "RemovePrintStatements";
        } else if (patternId.equals("#25")) {
            return "FixJavadocErrors";
        } else if (patternId.equals("#26")) {
            return "ReplaceAtTodoWithTODO";
        } else if (patternId.equals("#27")) {
            return "UseCodeAnnotationsAtJavaDoc";
        } else if (patternId.equals("#28")) {
            return "FormatCode";
        } else if (patternId.equals("#29")) {
            return "ConvertForLoopsToEnhanced";
        } else if (patternId.equals("#30")) {
            return "UseFinalModifierWherePossible";
        } else if (patternId.equals("#31")) {
            return "AddExplicitBlocks";
        } else if (patternId.equals("#32")) {
            return "UseThisIfNecessary";
        } else if (patternId.equals("#33")) {
            return "AddSerialVersionUIDs";
        } else if (patternId.equals("#34")) {
            return "AddOverrideAnnotation";
        } else if (patternId.equals("#35")) {
            return "UseDiamondOperators";
        } else if (patternId.equals("#36")) {
            return "UseArithmeticAssignmentOperators";
        } else if (patternId.equals("#37")) {
            return "AccessFilesProperly";
        } else if (patternId.equals("#38")) {
            return "AddCastToNull";
        } else if (patternId.equals("#39")) {
            return "AccessStaticMethodsAtDefinedClasses";
        } else if (patternId.equals("#40")) {
            return "AccessStaticFieldsAtDefinedClasses";
        } else if (patternId.equals("#FP1")) {
            return "FalsePositive";
        } else {
            return "Limitation";
        }
    }

    public static String patternIdFromValidatorName(String validatorName) {
        if (validatorName.endsWith("DoNotSwallowTestErrorsSilently") || validatorName.endsWith("AddFailStatementsForHandlingExpectedExceptions") ||
                validatorName.endsWith("UseFailInsteadOfAssertTrueFalse")) {
            return "#1";
        } else if (validatorName.endsWith("AssertNotNullToInstances")) {
            return "UseAssertArrayEqualsProperly";
        } else if (validatorName.endsWith("UseProcessWaitfor")) {
            return "#3";
        } else if (validatorName.endsWith("CloseResources") || validatorName.endsWith("UseTryWithResources")) {
            return "#4";
        } else if (validatorName.endsWith("DeleteUnnecessaryAssignmenedVariables") || validatorName.endsWith("RemoveUnnecessaryCasts") ||
                validatorName.endsWith("IntroduceAutoBoxing") || validatorName.endsWith("AddOverrideAnnotationToMethodsInConstructors") ||
                validatorName.endsWith("AddOverrideAnnotationToTestCase") || validatorName.endsWith("AddSerialVersionUids") || validatorName.endsWith("AddSuppressWarningsDeprecationAnnotation") ||
                validatorName.endsWith("AddSuppressWarningsRawtypesAnnotation") || validatorName.endsWith("AddSuppressWarningsUncheckedAnnotation")) {
            return "#5";
        } else if (validatorName.endsWith("RemovePrintStatements")) {
            return "#6";
        } else if (validatorName.endsWith("FixJavadocErrors") || validatorName.endsWith("ReplaceAtTodoWithTODO") ||
                validatorName.endsWith("UseCodeAnnotationsAtJavaDoc")) {
            return "#7";
        } else if (validatorName.endsWith("AddTestAnnotations")) {
            return "#8";
        } else if (validatorName.endsWith("ModifyAssertImports")) {
            return "#9";
        } else if (validatorName.endsWith("UseAssertArrayEqualsProperly") || validatorName.endsWith("UseAssertEqualsProperly") ||
                validatorName.endsWith("UseAssertFalseProperly") || validatorName.endsWith("UseAssertNotSameProperly") ||
                validatorName.endsWith("UseAssertNullProperly") || validatorName.endsWith("UseAssertTrueProperly")) {
            return "#10";
        } else if (validatorName.endsWith("SwapActualExpectedValues")) {
            return "#11";
        } else if (validatorName.endsWith("HandleExpectedExecptionsProperly")) {
            return "#12";
        } else if (validatorName.endsWith("FormatCode") || validatorName.endsWith("ConvertForLoopsToEnhanced") ||
                validatorName.endsWith("UseModifierFinalWherePossible") || validatorName.endsWith("AddExplicitBlocks") ||
                validatorName.endsWith("UseThisIfNecessary") || validatorName.endsWith("UseDiamondOperators") ||
                validatorName.endsWith("UseArithmeticAssignmentOperators") || validatorName.endsWith("RemoveUnusedExceptions")) {
            return "#13";
        } else if (validatorName.endsWith("AccessStaticFieldsAtDefinedSuperClass") || validatorName.endsWith("AccessStaticMethodsAtDefinedSuperClass")) {
            return "#14";
        } else if (validatorName.endsWith("AccessFilesProperly")) {
            return "#15";
        } else if (validatorName.endsWith("AddCastToNull")) {
            return "#16";
        } else {
            return "#20";
        }
    }

    public static List<String> getCoveredLinesLatestTag(File file) throws IOException {
        String content = FileUtils.readFileToString(file);
        List<String> ret = new ArrayList<>();
        Document document = Jsoup.parse(content, "", Parser.xmlParser());
        Elements tbodys = document.select("tbody");
        Elements trs = new Elements();
        for (int i = 1; i < tbodys.size(); i++) {
            trs.addAll(tbodys.get(i).select("tr.target"));
        }
        Elements tds = new Elements();
        for (Element tr : trs) {
            tds.add(tr.select("td").first());
        }
        Elements hrefs = tds.select("a[href]");
        for (Element href : hrefs) {
            ret.add((href.text()));
        }
        return ret;
    }
    public static String patternIdFromItemId(String itemId) {
        if (itemId.equals("#1")) {
            return "#8";
        } else if (itemId.equals("#2")) {
            return "#10";
        } else if (itemId.equals("#3")) {
            return "#10";
        } else if (itemId.equals("#4")) {
            return "#10";
        } else if (itemId.equals("#5")) {
            return "#10";
        } else if (itemId.equals("#6")) {
            return "#10";
        } else if (itemId.equals("#7")) {
            return "#10";
        } else if (itemId.equals("#8")) {
            return "#10";
        } else if (itemId.equals("#9")) {
            return "#10";
        } else if (itemId.equals("#10")) {
            return "#11";
        } else if (itemId.equals("#11")) {
            return "#2";
        } else if (itemId.equals("#12")) {
            return "#9";
        } else if (itemId.equals("#13")) {
            return "#1";
        } else if (itemId.equals("#14")) {
            return "#1";
        } else if (itemId.equals("#15")) {
            return "#1";
        } else if (itemId.equals("#16")) {
            return "#12";
        } else if (itemId.equals("#17")) {
            return "#4";
        } else if (itemId.equals("#18")) {
            return "#4";
        } else if (itemId.equals("#19")) {
            return "#3";
        } else if (itemId.equals("#20")) {
            return "#5";
        } else if (itemId.equals("#21")) {
            return "#5";
        } else if (itemId.equals("#22")) {
            return "#5";
        } else if (itemId.equals("#23")) {
            return "#5";
        } else if (itemId.equals("#24")) {
            return "#6";
        } else if (itemId.equals("#25")) {
            return "#7";
        } else if (itemId.equals("#26")) {
            return "#7";
        } else if (itemId.equals("#27")) {
            return "#7";
        } else if (itemId.equals("#28")) {
            return "#13";
        } else if (itemId.equals("#29")) {
            return "#13";
        } else if (itemId.equals("#30")) {
            return "#13";
        } else if (itemId.equals("#31")) {
            return "#13";
        } else if (itemId.equals("#32")) {
            return "#13";
        } else if (itemId.equals("#33")) {
            return "#13";
        } else if (itemId.equals("#34")) {
            return "#13";
        } else if (itemId.equals("#35")) {
            return "#13";
        } else if (itemId.equals("#36")) {
            return "#13";
        } else if (itemId.equals("#37")) {
            return "#15";
        } else if (itemId.equals("#38")) {
            return "#16";
        } else if (itemId.equals("#39")) {
            return "#14";
        } else if (itemId.equals("#40")) {
            return "#14";
        } else {
            return "";
        }
    }
}
