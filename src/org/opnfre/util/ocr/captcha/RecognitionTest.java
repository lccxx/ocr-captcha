package org.opnfre.util.ocr.captcha;

import java.io.IOException;
import java.sql.SQLException;

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
	
	public void testRecognition() throws IOException, InterruptedException,
			SQLException {
		String filename = "/home/liuchong/Pictures/Web/captcha.png";
		String captcha = "v6ta";
		Recognition r = new Recognition(filename);
		String result = r.go();
		assertTrue(result.equals(captcha));
	}
}
