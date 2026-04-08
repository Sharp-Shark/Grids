package io.github.game;

import com.badlogic.gdx.graphics.Color;

public class TilePrefab {
    final static TilePrefab solid = new TilePrefab(
        0f,
        1f,
        1f,
        false,
        Color.CYAN,
        false
    );
    final static TilePrefab background = new TilePrefab(
        0f,
        1f,
        0f,
        true,
        Color.BLUE,
        false
    );
    final static TilePrefab empty = new TilePrefab(
        0f,
        0f,
        0f,
        true,
        Color.RED,
        true
    );

    float minHealth;
    float maxHealth;
    float mass;
    boolean noCollision;
    Color color;
    boolean noDraw;

    public TilePrefab (float minHealth, float maxHealth, float mass, boolean noCollision, Color color, boolean noDraw) {
        this.minHealth = minHealth;
        this.maxHealth = maxHealth;
        this.mass = mass;
        this.noCollision = noCollision;
        this.color = color;
        this.noDraw = noDraw;
    }
}