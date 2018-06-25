package com.github.eclipseace.jtoolset.captcha_maker;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

class TextImageCaptchaEngineTest {

	@Test
	void test() {
		File folder = new File("d:\\captcha-test");
		if (!folder.exists()) {
			folder.mkdirs();
		}
		TextImageCaptchaEngine engine = new TextImageCaptchaEngine();
		long total = 0;
		for (int i = 0; i < 100; ++i) {
			String path = Paths.get(folder.getAbsolutePath(), String.format("%d.png", i)).toString();
			try (OutputStream fstream = new FileOutputStream(path)) {
				long start = System.currentTimeMillis();
				BufferedImage image = engine.getChallenge(engine.getPrototype());
				long end = System.currentTimeMillis();
				total += (end - start);
				ImageIO.write(image, "png", fstream);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		System.out.println("Average time: " + total / 100);
	}
}
