package org.opnfre.util.ocr.captcha;

import java.io.IOException;

import junit.framework.TestCase;

public class RecognitionTest extends TestCase {
	public void testRecognition() throws IOException, InterruptedException {
		String filename = "/home/liuchong/Pictures/Web/captcha.png";
		String captcha = "v6ta";
		Recognition r = new Recognition(filename);
		String result = r.go();
		assertTrue(result == captcha);
	}
}
