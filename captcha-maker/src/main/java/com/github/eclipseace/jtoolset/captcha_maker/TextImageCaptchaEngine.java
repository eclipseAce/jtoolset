package com.github.eclipseace.jtoolset.captcha_maker;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;

public class TextImageCaptchaEngine implements CaptchaEngine<String, BufferedImage> {
	public static final int DEFAULT_WIDTH = 120;
	public static final int DEFAULT_HEIGHT = 80;
	public static final int DEFAULT_LENGTH = 4;
	public static final int DEFAULT_MAX_FONT_SIZE = 50;
	public static final int DEFAULT_MIN_FONT_SIZE = 30;
	public static final boolean DEFAULT_CASE_SENSITIVE = false;
	public static final String DEFAULT_CHARACTERS = "0123456789";
	public static final String DEFAULT_FONT_NAMES = "Arial,Helvetica,Times New Roman,Courier New";

	private final Random random = new SecureRandom();

	private int width;
	private int height;
	private int length;
	private int maxFontSize;
	private int minFontSize;
	private boolean caseSensitive;
	private String characters;
	private List<Font> fonts;

	public TextImageCaptchaEngine() {
		setWidth(DEFAULT_WIDTH);
		setHeight(DEFAULT_HEIGHT);
		setLength(DEFAULT_LENGTH);
		setMaxFontSize(DEFAULT_MAX_FONT_SIZE);
		setMinFontSize(DEFAULT_MIN_FONT_SIZE);
		setCaseSensitive(DEFAULT_CASE_SENSITIVE);
		setCharacters(DEFAULT_CHARACTERS);
		setFontNames(Arrays.asList(DEFAULT_FONT_NAMES.split(",")));
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getLength() {
		return length;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public int getMaxFontSize() {
		return maxFontSize;
	}

	public void setMaxFontSize(int maxFontSize) {
		this.maxFontSize = maxFontSize;
	}

	public int getMinFontSize() {
		return minFontSize;
	}

	public void setMinFontSize(int minFontSize) {
		this.minFontSize = minFontSize;
	}

	public boolean isCaseSensitive() {
		return caseSensitive;
	}

	public void setCaseSensitive(boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
	}

	public String getCharacters() {
		return characters;
	}

	public void setCharacters(String characters) {
		this.characters = characters;
	}

	public Collection<Font> getFonts() {
		return fonts;
	}

	public void setFonts(Collection<? extends Font> fonts) {
		this.fonts = ImmutableList.copyOf(fonts);
	}

	public void setFontNames(Collection<? extends String> fontNames) {
		setFonts(fontNames.stream().map(name -> new Font(name, Font.BOLD, 1)).collect(Collectors.toList()));
	}

	@Override
	public BufferedImage getChallenge(String prototype) {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = createGraphics(image);

		int textWidth = 0;
		int textLength = prototype.length();
		char[] chars = prototype.toCharArray();
		Font[] charFonts = new Font[textLength];
		int[] charAscents = new int[textLength];
		Rectangle2D[] charBounds = new Rectangle2D[textLength];
		for (int i = 0; i < textLength; ++i) {
			int fontSize = minFontSize + random.nextInt(maxFontSize - minFontSize);
			chars[i] = characters.charAt(random.nextInt(characters.length()));
			charFonts[i] = fonts.get(random.nextInt(fonts.size())).deriveFont((float) fontSize);
			FontMetrics metrics = g2d.getFontMetrics(charFonts[i]);
			charBounds[i] = metrics.getStringBounds(String.valueOf(chars[i]), g2d);
			charAscents[i] = metrics.getAscent();

			textWidth += charBounds[i].getWidth();
		}

		int x = (width - textWidth) / 2;
		for (int i = 0; i < textLength; ++i) {
			int chHeight = (int) charBounds[i].getHeight();
			int chWidth = (int) charBounds[i].getWidth();
			int y = (height - chHeight) / 2;
			int offsetX = x + random.nextInt(chWidth / 4) * (random.nextBoolean() ? 1 : -1);
			int offsetY = y + random.nextInt(y) * (random.nextBoolean() ? 1 : -1);
			double rotate = Math.PI / 6 * random.nextDouble() * (random.nextBoolean() ? -1 : 1);
			double rotateX = offsetX + chWidth / 2;
			double rotateY = offsetY + chHeight / 2;

			g2d.setFont(charFonts[i]);

			TextLayout charLayout = new TextLayout(String.valueOf(chars[i]), charFonts[i], g2d.getFontRenderContext());
			AffineTransform atTranslate = AffineTransform.getTranslateInstance(offsetX, offsetY + charAscents[i]);
			Shape textOutline = charLayout.getOutline(atTranslate);
			AffineTransform atRotate = AffineTransform.getRotateInstance(random.nextDouble() * Math.PI, rotateX, rotateY);
			int rectWidth = 1 + random.nextInt(6);
			Shape rect = atRotate.createTransformedShape(new Rectangle(offsetX + chWidth / 2 - rectWidth, 0,
					rectWidth, offsetY + chHeight));
			Area textPart1 = new Area(textOutline);
			textPart1.subtract(new Area(rect));
			Area textPart2 = new Area(textOutline);
			textPart2.intersect(new Area(rect));

			g2d.rotate(rotate, rotateX, rotateY);
			g2d.setStroke(new BasicStroke(1 + random.nextInt(3)));
			if (random.nextBoolean()) {
				g2d.draw(textPart1);
				g2d.fill(textPart2);
			} else {
				g2d.draw(textPart2);
				g2d.fill(textPart1);
			}
			g2d.rotate(-rotate, rotateX, rotateY);

			x += chWidth;
		}
		g2d.dispose();
		return image;
	}

	@Override
	public String getPrototype() {
		char[] chs = new char[length];
		for (int i = 0; i < length; ++i) {
			chs[i] = characters.charAt(random.nextInt(characters.length()));
		}
		return new String(chs);
	}

	private Graphics2D createGraphics(BufferedImage image) {
		int imgWidth = image.getWidth();
		int imgHeight = image.getHeight();

		Graphics2D g2d = image.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		float bound = randomFloat(0.1f, 0.9f);
		Point pt1 = new Point(random.nextInt(imgWidth / 2), random.nextInt(imgHeight));
		Point pt2 = new Point(imgWidth + random.nextInt(imgWidth / 2), random.nextInt(imgHeight));
		Color dcl1 = new Color(randomFloat(0, bound), randomFloat(0, bound), randomFloat(0, bound));
		Color dcl2 = new Color(randomFloat(0, bound), randomFloat(0, bound), randomFloat(0, bound));
		Color lcl1 = new Color(randomFloat(bound, 1), randomFloat(bound, 1), randomFloat(bound, 1));
		Color lcl2 = new Color(randomFloat(bound, 1), randomFloat(bound, 1), randomFloat(bound, 1));

		if (random.nextBoolean()) {
			g2d.setPaint(new GradientPaint(pt1, lcl1, pt2, lcl2));
			g2d.fill(new Rectangle(0, 0, imgWidth, imgHeight));
			g2d.setPaint(new GradientPaint(pt1, dcl1, pt2, dcl2));
		} else {
			g2d.setPaint(new GradientPaint(pt1, dcl1, pt2, dcl2));
			g2d.fill(new Rectangle(0, 0, imgWidth, imgHeight));
			g2d.setPaint(new GradientPaint(pt1, lcl1, pt2, lcl2));
		}
		return g2d;
	}

	private float randomFloat(float min, float max) {
		return min + random.nextFloat() * (max - min);
	}
}
