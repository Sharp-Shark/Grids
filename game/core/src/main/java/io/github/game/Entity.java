package io.github.game;

import com.badlogic.gdx.math.Vector2;

public class Entity {
    Vector2 pos;
    Vector2 vel;
	Vector2 gravity = new Vector2(0, -5);
	float friction = 0.9f;
	float mass = 0;
	boolean immobile = false;

    float widthAABB;
    float heightAABB;

	GridManager gridManager;
	boolean removed = false;

    public Entity (GridManager gridManager) {
        pos = new Vector2();
        vel = new Vector2();
		gravity = new Vector2(gravity);

		this.gridManager = gridManager;
    }

	public Vector2 getRelativePosition (float x, float y) {
		return new Vector2((1 + x) * widthAABB / 2f, (1 + y) * heightAABB / 2f);
	}

	public boolean isTouchingAABB (Entity entity) {
		if (removed) return false;

		return Math.abs(pos.x - entity.pos.x + (widthAABB - entity.widthAABB) / 2f) <= (widthAABB + entity.widthAABB) / 2f && 
		Math.abs(pos.y - entity.pos.y + (heightAABB - entity.heightAABB) / 2f) <= (heightAABB + entity.heightAABB) / 2f;
	}
}