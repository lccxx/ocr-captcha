package org.opnfre.util.ocr.captcha;

import java.io.File;
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
		File file = new File("/home/liuchong/Pictures/Web/captcha/v6ta");
		String captcha = file.getName();
		Recognition r = new Recognition(file);
		String result = r.go();
		assertTrue(result.equals(captcha));
	}
}
