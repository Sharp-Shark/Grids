package io.github.game;

import java.util.ArrayDeque;
import java.util.ArrayList;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

public class GridManager {
    ArrayList<Grid> grids;
    ArrayDeque<Grid> newGridQueue;

    public GridManager () {
        grids = new ArrayList<Grid>();
        newGridQueue = new ArrayDeque<Grid>();
    }

    public Grid addGrid (Vector2 pos, int width, int height) {
        Grid grid = new Grid(width, height, this);
        grid.pos = pos;
        newGridQueue.add(grid);
        return grid;
    }

    public void removeGrids () {
        int index = 0;
        Grid grid;
        while (index < grids.size()) {
            grid = grids.get(index);
            if (grid.removed) {
                grids.remove(index);
            } else {
                index += 1;
            }
        }
    }

    public void update (float dt) {
        removeGrids();
        for (Grid grid : grids) {
            if (grid.removed) continue;
            
            grid.update(dt, grids);
        }
        while (newGridQueue.size() > 0) {
            grids.add(newGridQueue.pop());
        }
    }

	public void draw (SpriteBatch sb, Sprite sprite, BitmapFont font) {
        for (Grid grid : grids) {
            grid.drawBack(sb, sprite, font);
        }
        for (Grid grid : grids) {
            grid.drawFront(sb, sprite, font);
        }
    }
}