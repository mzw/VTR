package jp.mzw.vtr.cluster;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import difflib.Chunk;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;

public class DiffAnalyzer {
	
	protected List<String> prev_src;
	protected List<String> curr_src;
	public DiffAnalyzer(List<String> prev_src, List<String> curr_src) {
		this.prev_src = prev_src;
		this.curr_src = curr_src;
	}

	protected List<ASTNode> prev_diff_nodes;
	protected List<ASTNode> curr_diff_nodes;
	public void analyzeChunk(DetectionResult result) {
		
		/// Whole test added
		if(prev_src == null) {
			analyzeChunk4WholeTestAdded(result);
			return;
		}
		
		ArrayList<Chunk<String>> prev_diff = new ArrayList<>();
		ArrayList<Chunk<String>> curr_diff = new ArrayList<>();
		Patch<String> patch = DiffUtils.diff(prev_src, curr_src);
		for(Delta<String> delta : patch.getDeltas()) {
			
			boolean isTargetDiff = false;
			for(int cur_pos_offset = 0; cur_pos_offset < delta.getRevised().getLines().size(); cur_pos_offset++) {
				int cur_pos = delta.getRevised().getPosition() + cur_pos_offset + 1;
				
				/// As a cushion for different diff-patch systems
				String revised_line = delta.getRevised().getLines().get(cur_pos_offset);
				if("".equals(revised_line.trim())) continue;
				
				if(result.getTestMethodLines().contains(cur_pos)) {
					isTargetDiff = true;
				} else {
					isTargetDiff = false;
					break;
				}
			}
			if(!isTargetDiff) {
				continue;
			}
			prev_diff.add(delta.getOriginal());
			curr_diff.add(delta.getRevised());
		}
		
		/// if the cushion does not work
		if(curr_diff.size() == 0) {
			analyzeChunk4WholeTestAdded(result);
			return;
		}
		
		prev_diff_nodes = getDiffSyntaxElements(prev_diff, prev_src);
		curr_diff_nodes = getDiffSyntaxElements(curr_diff, curr_src);
	}
	
	private void analyzeChunk4WholeTestAdded(DetectionResult result) {
		prev_diff_nodes = new ArrayList<>();
		
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setSource(changeSourceType(curr_src));
		CompilationUnit cu = (CompilationUnit) parser.createAST(new NullProgressMonitor());
		AllElementsFindVisitor visitor = new AllElementsFindVisitor();
		cu.accept(visitor);
		List<ASTNode> nodes = visitor.getNodes();
		
		curr_diff_nodes = new ArrayList<>();
		for(ASTNode node : nodes) {
			boolean isTargetNode = false;
			
			int start_line_num = cu.getLineNumber(node.getStartPosition());
			int end_line_num = cu.getLineNumber(node.getStartPosition() + node.getLength());
			
			for(int offset = 0; start_line_num + offset <= end_line_num; offset++) {
				int line_num = start_line_num + offset;
				if(result.getTestMethodLines().contains(line_num)) {
					isTargetNode = true;
				} else {
					isTargetNode = false;
					break;
				}
			}
			
			if(!isTargetNode) {
				continue;
			}
			
			curr_diff_nodes.add(node);
		}
		
	}
	
	public List<ASTNode> getCurrDiffNodes() {
		return this.curr_diff_nodes;
	}
	public List<ASTNode> getPrevDiffNodes() {
		return this.prev_diff_nodes;
	}

	private static List<ASTNode> getDiffSyntaxElements(List<Chunk<String>> chunkList, List<String> source) {
		ArrayList<ASTNode> ret = new ArrayList<>();
		
		/// Get diff range
		ArrayList<Integer> range = new ArrayList<>();
		for(Chunk<String> chunk : chunkList) {
			int start_pos = chunk.getPosition()+1;
			for(int offset = 0; offset <= chunk.getLines().size(); offset++) {
				range.add(start_pos + offset);
			}
		}
		/// Parse source
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setSource(changeSourceType(source));
		CompilationUnit cu = (CompilationUnit) parser.createAST(new NullProgressMonitor());
		AllElementsFindVisitor visitor = new AllElementsFindVisitor();
		cu.accept(visitor);
		List<ASTNode> nodes = visitor.getNodes();
		/// Find diff syntax elements
		for(ASTNode node : nodes) {
			int _start_pos = cu.getLineNumber(node.getStartPosition());
			int _end_pos = cu.getLineNumber(node.getStartPosition() + node.getLength());
			boolean inRange = false;
			for(int _pos = _start_pos; _pos <= _end_pos; _pos++) {
				if(range.contains(_pos)) {
					inRange = true;
				} else {
					inRange = false;
					break;
				}
			}
			if(inRange) {
				ret.add(node);
			}
		}
		return ret;
	}
	
	private static char[] changeSourceType(List<String> lines) {
		StringBuilder builder = new StringBuilder();
		for(String line : lines) {
			builder.append(line).append("\n");
		}
		return builder.toString().toCharArray();
	}
	
	
}
