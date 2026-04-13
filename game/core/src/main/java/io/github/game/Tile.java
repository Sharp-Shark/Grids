package io.github.game;

import java.lang.Math;
import java.util.Vector;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

public class Tile {
    Grid grid;
    TilePrefab prefab;
    float health;

    public Tile (Grid grid, TilePrefab prefab) {
        this.grid = grid;
        this.prefab = prefab;
        this.health = prefab.maxHealth;

        grid.mass += this.getMass();
    }

    public void setPrefab (TilePrefab newPrefab, boolean revive) {
        if (health > 0) {
            grid.mass += newPrefab.mass - prefab.mass;
        }
        this.prefab = newPrefab;
        if (revive) {
            this.setHealth(newPrefab.maxHealth);
        } else {
            this.setHealth(health);
        }
    }

    public void setGrid (Grid newGrid) {
        grid.mass -= this.getMass();
        newGrid.mass += this.getMass();
        grid = newGrid;
    }

    public void setHealth (float newHealth) {
        float oldHealth = health;
        health = Math.max(prefab.minHealth, Math.min(prefab.maxHealth, newHealth));
        if (oldHealth > 0 && health <= 0) {
            grid.mass -= prefab.mass;
        } else if (oldHealth <= 0 && health > 0) {
            grid.mass += prefab.mass;
        }
    }

    public float getMass () {
        return health > 0 ? prefab.mass : 0;
    }

    public boolean isNoCollision () {
        return prefab.noCollision || (health <= 0);
    }

    public boolean isNoDraw () {
        return prefab.noDraw || (health <= 0);
    }

    public void draw (SpriteBatch sb, Sprite sprite, Vector2 pos) {
        if (this.isNoDraw()) return;

        sprite.setPosition(pos.x, pos.y);
        sprite.setColor(new Color(prefab.color).mul(1, 1, 1, 0.5f + 0.5f * health / Math.max(1, prefab.maxHealth)));
        sprite.draw(sb);
    }
}