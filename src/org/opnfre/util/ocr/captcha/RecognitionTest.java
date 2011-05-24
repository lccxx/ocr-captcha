package org.opnfre.util.ocr.captcha;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import junit.framework.TestCase;

public class RecognitionTest extends TestCase {
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {

	}

	public void testSplit() throws IOException {
		File[] captchas = new File("/home/liuchong/Pictures/Web/captcha/")
				.listFiles();
		for (File captcha : captchas) {
			Recognition r = new Recognition(captcha);
			int[][] grid = r.getPxGrid();
			r.setAbsLevel(3);
			int[][] gridPxAbs = r.absPxGrid(grid, r.getAbsLevel());
			int[][] gridPxPri = r.cropPxGrid(gridPxAbs);
			List<List<List<Integer>>> gridPxSplit = r.splitPxGrid(gridPxPri);
			boolean result = gridPxSplit.size() <= captcha.getName().length();
			if (!result) {
				r.display(gridPxPri);
				for (List<List<Integer>> one : gridPxSplit) {
					r.display(one);
					System.out.println("\n\n");
				}
			}
			assertTrue(result);
		}
	}

	public void testRecognition() throws IOException, InterruptedException,
			SQLException {
		File file = new File("/home/liuchong/Pictures/Web/captcha/v6ta");
		String captcha = file.getName();
		Recognition r = new Recognition(file);
		String result = r.go();
		assertTrue(result.equals(captcha));
	}
}
