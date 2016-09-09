package jp.mzw.vtr.dict;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

import jp.mzw.vtr.git.Commit;
import jp.mzw.vtr.git.Tag;

public class DictionaryBase {

	public static final String FILENAME_COMMITS_XML = "commits.xml";
	public static final String FILENAME_DICT_XML = "dict.xml";

	public static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
	
	/**
	 * Get tag relevant to given commit
	 * @param commit
	 * @param dict
	 * @return
	 */
	public static Tag getTagBy(Commit commit, Map<Tag, List<Commit>> dict) {
		for(Tag tag : dict.keySet()) {
			for(Commit _commit : dict.get(tag)) {
				if(_commit.getId().equals(commit.getId())) {
					return tag;
				}
			}
		}
		return null;
	}
}
