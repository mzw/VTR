package jp.mzw.vtr.example;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.junit.Test;

public class FileUtilsTest {

	@Test
	public void testGrepText() {
		File file = new File("src/test/resources", "alice.txt");
		try {
			ArrayList<String> lines = FileUtils.grep(file, "it's no use going back to yesterday");
			assertTrue(lines.get(0).contains("because I was a different person then"));
		} catch (IOException e) {
			fail("File do not exist");
		}
	}

	@Test
	public void testNotFoundText() {
		File file = new File("src/test/resources", "alice.txt");
		try {
			ArrayList<String> lines = FileUtils.grep(file, "I was the same person");
			assertEquals(0, lines.size());
		} catch (IOException e) {
			fail("File do not exist");
		}
	}

	@Test(expected=IOException.class)
	public void testNonExistFile() throws IOException {
		File file = new File("src/test/resources", "wonderland.txt");
		FileUtils.grep(file, "it's no use going back to yesterday");
	}

	@Test
	public void testGetFiles() {
		ArrayList<File> files = FileUtils.getFiles(new File("src"));
		File file = new File("src/test/resources", "alice.txt");
		assertTrue(files.contains(file));
	}
	
	@Test
	public void testGetFilesFalseNegative() {
		ArrayList<File> files = FileUtils.getFiles(new File("src"));
		File file = new File("src/test/resources", "wonderland.txt");
		assertFalse(files.contains(file));
	}

	@Test(expected=NullPointerException.class)
	public void testGetFilesWithInvalidDirectory() {
		FileUtils.getFiles(new File("foo"));
	}
	
}
