package jp.mzw.vtr.detect.difflib;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import difflib.ChangeDelta;
import difflib.Chunk;
import difflib.Patch;

/**
 * This class is based on difflib.DiffUtils
 * 
 * @author Yuta Maezawa
 *
 */
public class DiffUtils {

	private static Pattern unifiedDiffChunkRe = Pattern.compile("^@@\\s+-(?:(\\d+)(?:,(\\d+))?)\\s+\\+(?:(\\d+)(?:,(\\d+))?)\\s+@@$");

	/**
	 * Parse the given text in unified format and creates the list of deltas for
	 * it.
	 * 
	 * @param diff
	 *            the text in unified format
	 * @return the patch with deltas.
	 */
	public static Patch<ChunkTagRest> parseUnifiedDiff(List<String> diff) {
		boolean inPrelude = true;
		List<String[]> rawChunk = new ArrayList<String[]>();
		Patch<ChunkTagRest> patch = new Patch<ChunkTagRest>();

		int old_ln = 0, new_ln = 0;
		String tag;
		String rest;
		for (String line : diff) {
			// Skip leading lines until after we've seen one starting with '+++'
			if (inPrelude) {
				if (line.startsWith("+++")) {
					inPrelude = false;
				}
				continue;
			}
			Matcher m = unifiedDiffChunkRe.matcher(line);
			if (m.find()) {
				// Process the lines in the previous chunk
				if (rawChunk.size() != 0) {
					List<ChunkTagRest> oldChunkLines = new ArrayList<ChunkTagRest>();
					List<ChunkTagRest> newChunkLines = new ArrayList<ChunkTagRest>();

					for (String[] raw_line : rawChunk) {
						tag = raw_line[0];
						rest = raw_line[1];
						if (tag.equals(" ") || tag.equals("-")) {
							oldChunkLines.add(new ChunkTagRest(tag, rest));
						}
						if (tag.equals(" ") || tag.equals("+")) {
							newChunkLines.add(new ChunkTagRest(tag, rest));
						}
					}
					patch.addDelta(new ChangeDelta<ChunkTagRest>(new Chunk<ChunkTagRest>(old_ln - 1, oldChunkLines), new Chunk<ChunkTagRest>(new_ln - 1, newChunkLines)));
					rawChunk.clear();
				}
				// Parse the @@ header
				old_ln = m.group(1) == null ? 1 : Integer.parseInt(m.group(1));
				new_ln = m.group(3) == null ? 1 : Integer.parseInt(m.group(3));

				if (old_ln == 0) {
					old_ln += 1;
				}
				if (new_ln == 0) {
					new_ln += 1;
				}
			} else {
				if (line.length() > 0) {
					tag = line.substring(0, 1);
					rest = line.substring(1);
					if (tag.equals(" ") || tag.equals("+") || tag.equals("-")) {
						rawChunk.add(new String[] { tag, rest });
					}
				} else {
					rawChunk.add(new String[] { " ", "" });
				}
			}
		}

		// Process the lines in the last chunk
		if (rawChunk.size() != 0) {
			List<ChunkTagRest> oldChunkLines = new ArrayList<ChunkTagRest>();
			List<ChunkTagRest> newChunkLines = new ArrayList<ChunkTagRest>();

			for (String[] raw_line : rawChunk) {
				tag = raw_line[0];
				rest = raw_line[1];
				if (tag.equals(" ") || tag.equals("-")) {
					oldChunkLines.add(new ChunkTagRest(tag, rest));
				}
				if (tag.equals(" ") || tag.equals("+")) {
					newChunkLines.add(new ChunkTagRest(tag, rest));
				}
			}

			patch.addDelta(new ChangeDelta<ChunkTagRest>(new Chunk<ChunkTagRest>(old_ln - 1, oldChunkLines), new Chunk<ChunkTagRest>(new_ln - 1, newChunkLines)));
			rawChunk.clear();
		}

		return patch;
	}
}
