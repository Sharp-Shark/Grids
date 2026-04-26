package io.github.game;

import java.util.Vector;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.ExtendViewport;

public class Util {
    // for debugging performance
	static public float logTime (long start, String text) {
		final float e6 = 1_000_000;
		final float delta = (System.nanoTime() - start) / e6;

		System.out.println(text.concat(String.valueOf(delta)));

		return delta;
	}

	static public float mod (float a, float b) {
		return (a % b + b) % b;
	}
}
