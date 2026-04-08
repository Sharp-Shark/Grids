package io.github.game;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

public class GridManager {
    ArrayList<Grid> grids;

    public GridManager () {
        grids = new ArrayList<Grid>();
    }

    public Grid addGrid (Vector2 pos, int width, int height) {
        Grid grid = new Grid(width, height, this);
        grid.pos = pos;
        grids.add(grid);
        return grid;
    }

    public void update (float dt) {
        for (Grid grid : grids) {
            if(grid.removed) {
                System.out.println("this guy SHOULD be removed but I have not written the remove code yet =)");
            }
        }
        for (Grid grid : grids) {
            if(grid.removed) continue;
            
            grid.update(dt, grids);
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