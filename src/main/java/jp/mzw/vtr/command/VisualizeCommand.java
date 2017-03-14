package jp.mzw.vtr.command;

import jp.mzw.vtr.core.Project;
import jp.mzw.vtr.cluster.visualize.VisualizerBase;
import jp.mzw.vtr.detect.Detector;

import java.io.IOException;
import java.text.ParseException;

import static jp.mzw.vtr.CLI.CONFIG_FILENAME;

/**
 * visualize detecting results
 */
public class VisualizeCommand {
    public static void command(String... args) throws IOException, ParseException {
        String method = args[1];
        VisualizerBase visualizer = VisualizerBase.visualizerFactory(method, new Project(null).setConfig(CONFIG_FILENAME).getOutputDir());
        if (visualizer != null) {
            visualizer.loadGeneratedSourceFileList(Detector.GENERATED_SOURCE_FILE_LIST).visualize();
        } else {
            System.out.println("$ java -cp=<class-path> jp.mzw.vtr.CLI visualize <method>");
        }
    }
}
