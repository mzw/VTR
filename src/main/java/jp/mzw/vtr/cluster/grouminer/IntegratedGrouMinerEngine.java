package jp.mzw.vtr.cluster.grouminer;

import com.paypal.digraph.parser.GraphEdge;
import com.paypal.digraph.parser.GraphNode;
import com.paypal.digraph.parser.GraphParser;
import groum.GROUMEdge;
import groum.GROUMGraph;
import groum.GROUMNode;
import jp.mzw.vtr.core.VtrUtils;
import org.apache.commons.lang3.StringEscapeUtils;
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
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;

public class IntegratedGrouMinerEngine implements IGrouMinerEngine {
    private static Logger LOGGER = LoggerFactory.getLogger(IntegratedGrouMinerEngine.class);

    private final String projectId;
    private final File projectDir;
    private final File outputDir;

    public IntegratedGrouMinerEngine(String projectId, File projectDir, File outputDir) {
        this.projectId = projectId;
        this.projectDir = projectDir;
        this.outputDir = outputDir;
    }

    protected void createDotFiles(String commit) {
        GROUMBuilder groumBuilder = new GROUMBuilder(projectDir.getPath());
        groumBuilder.build();
        ArrayList<GROUMGraph> graphs = groumBuilder.getGroums();
        for (GROUMGraph graph : graphs) {
            StringBuilder sb = new StringBuilder();
            sb.append(getDotStart());
            sb.append(addNodesToDot(graph, commit));
            sb.append(addEdgesToDot(graph));
            sb.append(getDotEnd());
            outputDotFile(commit, graph.getName(), sb.toString());
        }
    }


    protected GrouMiner.PatchPattern compareGroums(String prvCommit, String curCommit, String className, String methodName) {
        String fileName = String.join(".", className.substring(className.lastIndexOf(".") + 1), methodName);
        Pair<Map<String, GraphNode>, Map<String, GraphEdge>> prvDot = null;
        try {
            prvDot = parse(getPathToDotFile(prvCommit, fileName));
        } catch (NoSuchFileException e) {
            // do nothing
        }
        Pair<Map<String, GraphNode>, Map<String, GraphEdge>> curDot = null;
        try {
            curDot = parse(getPathToDotFile(curCommit, fileName));
        } catch (NoSuchFileException e) {
            // do nothing
        }

        // when dot files are not generated.
        if (prvDot == null && curDot == null) {
            return GrouMiner.PatchPattern.None;
        } else if (prvDot != null && curDot == null) {
            return GrouMiner.PatchPattern.Subtractive;
        } else if (prvDot == null) { // curDOt != null is always true.
            return GrouMiner.PatchPattern.Additive;
        }

        Map<String, GraphEdge> prvEdges = prvDot.getRight();
        Map<String, GraphNode> prvNodes = prvDot.getLeft();
        Map<String, GraphEdge> curEdges = curDot.getRight();
        Map<String, GraphNode> curNodes = curDot.getLeft();

        if (prvEdges.size() < curEdges.size()) { // the num of edges increases -> Additive
            return GrouMiner.PatchPattern.Additive;
        } else if (prvEdges.size() > curEdges.size()) { // the num of edges decreases -> Subtractive
            return GrouMiner.PatchPattern.Subtractive;
        } else if (isSameNodes(prvNodes, curNodes) && !isSameEdges(prvEdges, curEdges)) {
            // nodes don't change and edges change -> altering
            return GrouMiner.PatchPattern.Altering;
        } else {
            return GrouMiner.PatchPattern.None;
        }
    }

    /* Compare Dot files */
    private boolean isSameNodes(Map<String, GraphNode> prvNodes, Map<String, GraphNode> curNodes) {
        if (prvNodes.size() != curNodes.size()) {
            return false;
        }
        for (GraphNode prvNode : prvNodes.values()) {
            boolean found = false;
            String prvLabel = (String) prvNode.getAttribute("label");
            for (GraphNode curNode : curNodes.values()) {
                String curLabel = (String) curNode.getAttribute("label");
                if (prvLabel.equals(curLabel)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }
    private boolean isSameEdges(Map<String, GraphEdge> prvEdges, Map<String, GraphEdge> curEdges) {
        if (prvEdges.size() != curEdges.size()) {
            return false;
        }
        for (GraphEdge prvEdge : prvEdges.values()) {
            boolean found = false;
            String prvNode1Label = (String) prvEdge.getNode1().getAttribute("label");
            String prvNode2Label = (String) prvEdge.getNode2().getAttribute("label");
            for (GraphEdge curEdge : curEdges.values()) {
                String curNode1Label = (String) curEdge.getNode1().getAttribute("label");
                String curNode2Label = (String) curEdge.getNode2().getAttribute("label");
                if (prvNode1Label.equals(curNode1Label) && prvNode2Label.equals(curNode2Label)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    /* Parse Dot file */
    private Pair<Map<String, GraphNode>, Map<String, GraphEdge>> parse(Path pathToDotFile) throws NoSuchFileException {
        GraphParser parser = new GraphParser(getInputStream(pathToDotFile));
        Map<String, GraphNode> nodes = parser.getNodes();
        Map<String, GraphEdge> edges = parser.getEdges();
        return new ImmutablePair<>(nodes, edges);
    }
    private InputStream getInputStream(Path path) throws NoSuchFileException {
        InputStream ret = null;
        try {
            ret = Files.newInputStream(path);
        } catch (NoSuchFileException e) {
            LOGGER.error(e.getMessage());
            throw new NoSuchFileException(path.toString());
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
        }
        return ret;
    }
    /* Path To Dot files */
    private Path getPathToDotFile(String commit, String fileName) {
        return VtrUtils.getPathToFile(outputDir.getPath(), projectId, GrouMiner.GROUM_OUTPUT_DIR, "dot", commit, fileName + "dot");
    }
    /* To output Dot files */
    private void outputDotFile(String commit, String fileName, String content) {
        VtrUtils.writeContent(getPathToDotFile(commit, fileName), content);
    }
    /* To create Dot file */
    private static String getDotStart() {
        return "digraph G {\n";
    }
    private static String getDotEnd() {
        return "}";
    }
    private static String addNodesToDot(GROUMGraph graph, String commit) {
        StringBuilder sb = new StringBuilder();
        for (GROUMNode node : graph.getNodes()) {
            int id = node.getId();
            String label = node.getLabel();
            sb.append(nodeInfo(id, label, "box", "rounded", null, null));
        }
        sb.append(nodeInfo(graph.getId(), graph.getName(), "box", null, null, null));
        return sb.toString();
    }
    private static String addEdgesToDot(GROUMGraph graph) {
        StringBuilder sb = new StringBuilder();
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
        sb.append(id + " [label=\"" + StringEscapeUtils.escapeHtml4(label) + "\"");
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
