package jp.mzw.vtr.cluster.grouminer;

import com.paypal.digraph.parser.GraphEdge;
import com.paypal.digraph.parser.GraphNode;
import com.paypal.digraph.parser.GraphParser;
import groum.GROUMEdge;
import groum.GROUMGraph;
import groum.GROUMNode;
import jp.mzw.vtr.git.Commit;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import groum.GROUMBuilder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;

public class IntegratedGrouMinerEngine implements IGrouMinerEngine {
    private static Logger LOGGER = LoggerFactory.getLogger(IntegratedGrouMinerEngine.class);

    private final File subjectDir;
    private final File outputDir;

    public IntegratedGrouMinerEngine(File subjectDir, File outputDir) {
        this.subjectDir = subjectDir;
        this.outputDir = outputDir;
    }

    protected void createDotFiles(Commit commit) {
        GROUMBuilder groumBuilder = new GROUMBuilder(subjectDir.getPath());
        groumBuilder.build();
        ArrayList<GROUMGraph> graphs = groumBuilder.getGroums();
        for (GROUMGraph graph : graphs) {
            StringBuilder sb = new StringBuilder();
            sb.append(getDotStart());
            sb.append(addNodesToDot(graph));
            sb.append(addEdgesToDot(graph));
            sb.append(getDotEnd());
            outputDotFile(commit, graph.getName(), sb.toString());
        }
    }


    protected GrouMiner.PatchPattern compareGroums(Commit prvCommit, Commit curCommit, String className, String methodName) {
        String fileName = String.join(".", className, methodName, "dot");
        Pair<Map<String, GraphNode>, Map<String, GraphEdge>> prvDot = parse(getPathToDotFile(prvCommit, fileName));
        Pair<Map<String, GraphNode>, Map<String, GraphEdge>> curDot = parse(getPathToDotFile(curCommit, fileName));

        // ここで何らかの比較
        return GrouMiner.PatchPattern.Additive;
    }

    private Pair<Map<String, GraphNode>, Map<String, GraphEdge>> parse(Path pathToDotFile) {
        GraphParser parser = new GraphParser(getInputStream(pathToDotFile));
        Map<String, GraphNode> nodes = parser.getNodes();
        Map<String, GraphEdge> edges = parser.getEdges();
        return new ImmutablePair<>(nodes, edges);
    }

    private InputStream getInputStream(Path path) {
        InputStream ret = null;
        try {
            ret = Files.newInputStream(path);
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
        }
        return ret;
    }

    /* To output Dot files */
    private void outputDotFile(Commit commit, String fileName, String content) {
        if (!Files.exists(getPathToDotFile(commit, fileName))) {
            try {
                Files.createDirectories(getPathToDotDirectory(commit));
                Files.createFile(getPathToDotFile(commit, fileName));
            } catch (IOException e) {
                e.printStackTrace();
                LOGGER.error(e.getMessage());
            }
        }
        try (BufferedWriter bw = Files.newBufferedWriter(getPathToDotFile(commit, content))) {
            bw.write(content);
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
        }
    }

    private Path getPathToDotFile(Commit commit, String fileName) {
        return Paths.get(String.join("/", getPathToDotDirectory(commit).toString(), fileName + ".dot"));
    }

    private Path getPathToDotDirectory(Commit commit) {
        return Paths.get(String.join("/", outputDir.getPath(), "dot", commit.getId()));
    }

    /* To create Dot file */
    private static String getDotStart() {
        return "digraph G {\n";
    }
    private static String getDotEnd() {
        return "}";
    }
    private static String addNodesToDot(GROUMGraph graph) {
        StringBuilder sb = new StringBuilder();
        // add nodes
        for (GROUMNode node : graph.getNodes()) {
            int id = node.getId();
            String label = node.getLabel();
            sb.append(nodeInfo(id, label, "box", "rounded", null, null));
        }
        sb.append(nodeInfo(graph.getId(), graph.getName(), "rounded", null, null, null));
        return sb.toString();
    }
    private static String addEdgesToDot(GROUMGraph graph) {
        StringBuilder sb = new StringBuilder();
        // add edges
        for (GROUMNode node : graph.getNodes()) {
            for (GROUMEdge edge : node.getInEdges()) {
                int srcId = edge.getSrc().getId();
                int destId = edge.getDest().getId();
                sb.append(edgeInfo(srcId, destId, null, null, ""));
            }
        }
        return sb.toString();
    }
    private static String nodeInfo(int id, String label, String shape, String style, String borderColor, String fontColor) {
        StringBuilder sb = new StringBuilder();
        sb.append(id + " [label=\"" + label + "\"");
        if (shape != null && !shape.isEmpty())
            sb.append(" shape=" + shape);
        if (style != null && !style.isEmpty())
            sb.append(" style=" + style);
        if (borderColor != null && !borderColor.isEmpty())
            sb.append(" color=" + borderColor);
        if (fontColor != null && !fontColor.isEmpty())
            sb.append(" fontcolor=" + fontColor);
        sb.append("]\n");
        return sb.toString();
    }
    private static String edgeInfo(int sId, int eId, String style, String color, String label) {
        StringBuilder sb = new StringBuilder();
        if (label == null)
            label = "";
        sb.append(sId + " -> " + eId + " [label=\"" + label + "\"");
        if (style != null && !style.isEmpty())
            sb.append(" style=" + style);
        if (color != null && !color.isEmpty())
            sb.append(" color=" + color);
        sb.append("];\n");
        return sb.toString();
    }

}
