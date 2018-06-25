package com.github.eclipseace.jtoolset.captcha_maker;

public interface CaptchaManager<A, C> {
	C getChallenge(A answer);

	void dispose(String id);
}
