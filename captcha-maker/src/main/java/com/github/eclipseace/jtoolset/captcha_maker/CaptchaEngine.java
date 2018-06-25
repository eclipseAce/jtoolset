package com.github.eclipseace.jtoolset.captcha_maker;

public interface CaptchaEngine<P, C> {
	C getChallenge(P prototype);

	P getPrototype();
}
