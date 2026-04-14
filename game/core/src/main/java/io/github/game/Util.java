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

    static public Vector2[] rectToPoints (Vector2 startPos, float width, float height, float spacing) {
        int widthInteger = (int) Math.floor(width / spacing) + 1;
        int heightInteger = (int) Math.floor(height / spacing) + 1;
        widthInteger *= 2;
        heightInteger *= 2;
        Vector2[] points = new Vector2[widthInteger * heightInteger];
        int pointCount = 0;
        Vector2 pos = new Vector2(startPos);
        for (int y = 0; y < heightInteger; y++) {
            for (int x = 0; x < widthInteger; x++) {
                points[pointCount] = new Vector2(pos);
                pointCount += 1;
                pos.x = Math.min(pos.x + spacing, startPos.x + width);
            }
            pos.x = startPos.x;
            pos.y = Math.min(pos.y + spacing, startPos.y + height);
        }

        return points;
    }
}
