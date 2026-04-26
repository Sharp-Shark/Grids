package io.github.game;

import java.time.DayOfWeek;
import java.util.ArrayList;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;

public class Entity {
	static float ambientDensity = 0.01f;

    Vector2 pos;
    Vector2 vel;
	Vector2 gravity = new Vector2(0, -8);
	float airFriction = 0.95f;
	float floorFriction = 0.25f;
	float restitution = 0.5f;
	float mass = 0;
	float area = 0; // displaced area does not count background tiles
	float density = 1 / (Grid.tileSize * Grid.tileSize);
	boolean immobile = false;

    float widthAABB;
    float heightAABB;
	Vector2[] points;

	enum EntityType {
		DEFAULT,
		GRID
	}
	EntityType entityType = EntityType.DEFAULT;
	EntityManager entityManager;
	boolean removed = false;

    public Entity (EntityManager entityManager) {
        pos = new Vector2();
        vel = new Vector2();
		gravity = new Vector2(gravity);

		this.entityManager = entityManager;
    }

	public void resize (float newWidth, float newHeight) {
		widthAABB = newWidth;
		heightAABB = newHeight;
		area = widthAABB * heightAABB;
		mass = area * density;
		points = this.getGridPoints();
	}

	public Vector2 getRelativePosition (float x, float y) {
		return new Vector2((1 + x) * widthAABB / 2f, (1 + y) * heightAABB / 2f);
	}

    public Vector2[] getGridPoints () {
        int widthInteger = (int) Math.ceil(widthAABB / Grid.tileSize) + 1;
        int heightInteger = (int) Math.ceil(heightAABB / Grid.tileSize) + 1;
        Vector2[] points = new Vector2[widthInteger * heightInteger];
        int pointCount = 0;
        for (int y = 0; y < heightInteger; y++) {
            for (int x = 0; x < widthInteger; x++) {
                points[pointCount] = new Vector2(Math.min(widthAABB, x * Grid.tileSize), Math.min(heightAABB, y * Grid.tileSize));
                pointCount += 1;
            }
        }

        return points;
    }

	public boolean isTouchingPoint (Vector2 point) {
		return Math.abs(pos.x - point.x + widthAABB / 2f) <= widthAABB / 2f && 
		Math.abs(pos.y - point.y + heightAABB / 2f) <= heightAABB / 2f;
	}

	public boolean isTouchingAABB (Entity entity) {
		return Math.abs(pos.x - entity.pos.x + (widthAABB - entity.widthAABB) / 2f) <= (widthAABB + entity.widthAABB) / 2f && 
		Math.abs(pos.y - entity.pos.y + (heightAABB - entity.heightAABB) / 2f) <= (heightAABB + entity.heightAABB) / 2f;
	}

	public boolean isTouchingEntity (Entity entity) {
		return this.isTouchingAABB(entity);
	}

	public boolean isTouchingGrid (Grid grid) {
		return grid.isTouchingEntity(this);
	}

	public boolean isTouchingGeneric (Entity entity) {
		switch (entity.entityType) {
			case DEFAULT :
				return this.isTouchingEntity(entity);
			case GRID:
				Grid grid = (Grid) entity;
				return this.isTouchingGrid(grid);
			default :
				return false; // maybe it should error instead?
		}
	}

	public Entity getCollidingEntity (ArrayList<Entity> entities) {
		if (removed) return null;

		for (Entity entity : entities) {
			if ((this != entity) && !entity.removed && this.isTouchingEntity(entity)) {
				return entity;
			}
		}
		return null;
	}
	
	public Grid getCollidingGrid (ArrayList<Grid> grids) {
		if (removed) return null;

		for (Grid grid : grids) {
			if ((this != grid) && !grid.removed && this.isTouchingGrid(grid)) {
				return grid;
			}
		}
		return null;
	}

	public Entity getCollidingGeneric (ArrayList<? extends Entity> entities) {
		if (removed) return null;

		for (Entity entity : entities) {
			if ((this != entity) && !entity.removed && this.isTouchingGeneric(entity)) {
				return entity;
			}
		}
		return null;
	}

	void updatePhysics (float dt) {
		if (immobile) return;

		Entity entity;
		float totalMass, dv;
		float restitution;
		// horizontal
		pos.x += vel.x * dt;
		entity = this.getCollidingGeneric(entityManager.entities);
		if (entity != null) {
			pos.x -= vel.x * dt;

			restitution = this.restitution * entity.restitution;
			if (entity.immobile) {
				vel.x -= (restitution + 1) * vel.x;
			} else {
				totalMass = mass + entity.mass;
				dv = entity.vel.x - vel.x;
				vel.x += (restitution + 1) * entity.mass * dv / totalMass;
				entity.vel.x -= (restitution + 1) * mass * dv / totalMass;
			}
		}
		// vertical
		pos.y += vel.y * dt;
		entity = this.getCollidingGeneric(entityManager.entities);
		if (entity != null) {
			pos.y -= vel.y * dt;
			
			if (vel.y < 0) {
				vel.x *= Math.pow(entity.floorFriction, dt);
			} else {
				entity.vel.x *= Math.pow(this.floorFriction, dt);
			}

			restitution = this.restitution * entity.restitution;
			if (entity.immobile) {
				vel.y -= (restitution + 1) * vel.y;
			} else {
				totalMass = mass + entity.mass;
				dv = entity.vel.y - vel.y;
				vel.y += (restitution + 1) * entity.mass * dv / totalMass;
				entity.vel.y -= (restitution + 1) * mass * dv / totalMass;
			}
		}
		// gravity
		vel.add(new Vector2(gravity).scl(dt));
		// buoyancy
		vel.sub(new Vector2(gravity).scl(area * ambientDensity / mass * dt));
		// friction
		vel.scl((float) Math.pow(airFriction, dt));
	}

	void update (float dt) {
		if (removed) return;

		if (mass <= 0) {
			removed = true;
		}

		updatePhysics(dt);
	}

	void draw (SpriteBatch sb, Sprite sprite, BitmapFont font, Viewport viewport, boolean drawFront) {
		sprite.setSize(widthAABB, heightAABB);
        sprite.setPosition(pos.x, pos.y);
        sprite.setColor(Color.WHITE);
        sprite.draw(sb);

		/*
		for (Vector2 point : points) {
			sprite.setSize(Grid.tileSize / 4f, Grid.tileSize / 4f);
        	sprite.setPosition(point.x + pos.x, point.y + pos.y);
        	sprite.setColor(Color.GREEN);
        	sprite.draw(sb);
		}
		*/

		//font.draw(sb, String.valueOf(density), pos.x, pos.y);
	}
}