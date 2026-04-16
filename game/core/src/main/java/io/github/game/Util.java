package io.github.game;

import java.util.Vector;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.ExtendViewport;

public class Util {
    // for debugging performance
	static public void logTime (long start, String text) {
		final float e6 = 1_000_000;
		System.out.println(text.concat(String.valueOf((System.nanoTime()- start) / e6)));
	}
}
