package io.github.game;

import java.util.ArrayDeque;
import java.util.ArrayList;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

public class EntityManager {
    ArrayList<Entity> entities;
    ArrayList<Grid> grids;
    ArrayDeque<Entity> newEntityQueue;

    public EntityManager () {
        grids = new ArrayList<Grid>();
        entities = new ArrayList<Entity>();
        newEntityQueue = new ArrayDeque<Entity>();
    }

    public Entity addEntity (Vector2 pos, float width, float height) {
        Entity entity = new Entity(this);
        entity.pos = pos;
        entity.resize(width, height);
        newEntityQueue.add(entity);
        return entity;
    }

    public Grid addGrid (Vector2 pos, int width, int height) {
        Grid grid = new Grid(width, height, this);
        grid.pos = pos;
        newEntityQueue.add(grid);
        return grid;
    }

    public void removeEntities (ArrayList<? extends Entity> list) {
        int index = 0;
        while (index < list.size()) {
            Entity entity = list.get(index);
            if (entity.removed) {
                list.remove(index);
            } else {
                index += 1;
            }
        }
    }

    public void update (float dt) {
        removeEntities(grids);
        removeEntities(entities);
        for (Entity entity : entities) {
            if (entity.removed) continue;

            entity.update(dt);
        }
        while (newEntityQueue.size() > 0) {
            Entity entity = newEntityQueue.pop();
            if (entity instanceof Grid) {
                Grid grid = (Grid) entity;
                grids.add(grid);
            }
            entities.add(entity);
        }
    }

	public void draw (SpriteBatch sb, Sprite sprite, BitmapFont font, Viewport viewport) {
        for (Entity entity : entities) {
            entity.draw(sb, sprite, font, viewport, true);
        }
        for (Entity entity : entities) {
            entity.draw(sb, sprite, font, viewport, false);
        }
    }
}