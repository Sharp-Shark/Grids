package io.github.game;

import java.lang.Math;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

public class Grid {
	final static Vector2[] cornerOffsets = {new Vector2(0, 0), new Vector2(1, 0), new Vector2(1, 1), new Vector2(0, 1)};
	final static int[][] neighbours = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};
	static float tileSize = 0.5f;

	Vector2 pos;
	Vector2 vel;
	float friction;
	float mass;

	int width;
	int height;
	Tile[] tiles;

	GridManager gridManager;
	boolean removed = false;

	public Grid (int width, int height, GridManager gridManager) {
		pos = new Vector2();
		vel = new Vector2();
		friction = 0.5f;
		mass = 0;

		this.width = width;
		this.height = height;
		tiles = new Tile[width * height];
		Tile tile;
		for (int i = 0; i < this.tiles.length; i++) {
			tile = new Tile(this, TilePrefab.empty);
			tiles[i] = tile;
			mass += tile.getMass();
		}
		
		this.gridManager = gridManager;
		this.removed = false;
	}

	public void fill (TilePrefab newPrefab) {
		for (int i = 0; i < this.tiles.length; i++) {
			tiles[i].setPrefab(newPrefab, true);
		}
	}

	public Vector2 getRelativePosition (float x, float y) {
		return new Vector2((1 + x) * tileSize * width / 2f, (1 + y) * tileSize * height / 2f);
	}

	public Vector2 indexToPos (int index) {
		return new Vector2(pos.x + tileSize * (index % width), pos.y + tileSize * (index / width));
	}

	public int translateIndex (int index, int translateX, int translateY) {
		if (index == -1) return -1;
		int x = index % width + translateX;
		int y = index / height + translateY;
		if (x < 0) return -1;
		if (y < 0) return -1;
		if (x > width - 1) return -1;
		if (y > height - 1) return -1;
		return x + y * width;
	}

	public int posToIndex (Vector2 pos) {
		if (pos.x - this.pos.x < 0) return -1;
		if (pos.y - this.pos.y < 0) return -1;
		if (pos.x - this.pos.x > tileSize * width) return -1;
		if (pos.y - this.pos.y > tileSize * height) return -1;
		int x = (int) ((pos.x - this.pos.x) / tileSize);
		int y = (int) ((pos.y - this.pos.y) / tileSize);
		return Math.min(width - 1, x) + Math.min(height - 1, y) * width;
	}

	public void setTileHealth (int index, float newHealth) {
		float oldHealth = tiles[index].health;
		tiles[index].setHealth(newHealth);
		if(oldHealth > 0 && newHealth <= 0) {
			this.splinter(index);
		}
	}

	public void splinter (int indexStart) {
		HashMap<Integer, Integer> indexGroup = new HashMap<Integer, Integer>();
		ArrayDeque<Integer> queue = new ArrayDeque<Integer>();
		int[] groupMap = {0, 1, 2, 3};

		int index, indexDeleted;
		indexDeleted = indexStart;
		for (int i = 0; i < neighbours.length; i++) {
			index = translateIndex(indexDeleted, neighbours[i][0], neighbours[i][1]);
			if (index != -1 && tiles[index].health > 0) {
				queue.addLast(index);
				indexGroup.put(index, i);
			} else {
				groupMap[i] = -1;
			}
		}

		while (queue.size() > 0) {
			indexDeleted = queue.pop();
			for (int[] neighbour : neighbours) {
				index = translateIndex(indexDeleted, neighbour[0], neighbour[1]);
				if (index != -1 && tiles[index].health > 0) {
					if (indexGroup.containsKey(index)) {
						groupMap[indexGroup.get(index)] = groupMap[indexGroup.get(indexDeleted)];
					} else {
						queue.addLast(index);
						indexGroup.put(index, indexGroup.get(indexDeleted));
					}
				}
			}
		}

		for (int i : indexGroup.keySet()) {
			indexGroup.replace(i, groupMap[indexGroup.get(i)]);
		}

		boolean[] groupExists = {false, false, false, false};
		int[] groups = new int[4];
		int groupCount = 0;
		for (int i : groupMap) {
			if (i != -1 && !groupExists[i]) {
				groupExists[i] = true;
				groups[groupCount] = i;
				groupCount += 1;
			}
		}

		if (groupCount <= 1) return;

		Grid grid;
		for (int g = 1; g < groupCount; g++) {
			int group = groups[g];
			grid = gridManager.addGrid(new Vector2(pos), width, height);
			grid.vel = new Vector2(this.vel);
			for (int i : indexGroup.keySet()) {
				if(indexGroup.get(i) == group) {
					Tile tile = grid.tiles[i];
					grid.tiles[i] = tiles[i];
					tiles[i].setGrid(grid);
					tiles[i] = tile;
					tiles[i].setGrid(this);
				}
			}
		}
	}

	public boolean isTouchingGridAABB (Grid grid) {
		if (removed) return false;

		return Math.abs(pos.x - grid.pos.x + tileSize * (width - grid.width) / 2f) <= tileSize * (width + grid.width) / 2f && 
		Math.abs(pos.y - grid.pos.y + tileSize * (height - grid.height) / 2f) <= tileSize * (height + grid.height) / 2f;
	}

	public boolean isTouchingGridPerTile (Grid grid) {
		if (removed) return false;

		Vector2 pos;
		Tile tile;
		int index;
		for (int i = 0; i < this.tiles.length; i++) {
			tile = tiles[i];
			if (!tile.isNoCollision()) {
				for (Vector2 cornerOffset : cornerOffsets) {
					pos = this.indexToPos(i).add(new Vector2(cornerOffset).scl(tileSize));
					index = grid.posToIndex(pos);
					if (index != -1) {
						tile = grid.tiles[index];
						if (!tile.isNoCollision()) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	public Grid getCollidingGrid (ArrayList<Grid> grids) {
		if (removed) return null;

		for (Grid grid : grids) {
			if ((this != grid) && !grid.removed && this.isTouchingGridAABB(grid) && this.isTouchingGridPerTile(grid)) {
				return grid;
			}
		}
		return null;
	}

	public void update (float dt, ArrayList<Grid> grids) {
		if (removed) return;

		if (mass <= 0) {
			removed = true;
		}

		Grid grid;
		float totalMass, dv;
		float restitution = 0.5f;
		// horizontal
		pos.x += vel.x * dt;
		grid = this.getCollidingGrid(grids);
		if (grid != null) {
			pos.x -= vel.x * dt;

			totalMass = mass + grid.mass;
			dv = grid.vel.x - vel.x;
			vel.x += (restitution + 1) * grid.mass * dv / totalMass;
			grid.vel.x -= (restitution + 1) * mass * dv / totalMass;
		}
		// vertical
		pos.y += vel.y * dt;
		grid = this.getCollidingGrid(grids);
		if (grid != null) {
			pos.y -= vel.y * dt;

			totalMass = mass + grid.mass;
			dv = grid.vel.y - vel.y;
			vel.y += (restitution + 1) * grid.mass * dv / totalMass;
			grid.vel.y -= (restitution + 1) * mass * dv / totalMass;
		}

		vel.scl((float) Math.pow(friction, dt));
	}

	public void drawFront (SpriteBatch sb, Sprite sprite, BitmapFont font) {
		if (removed) return;

		Vector2 pos;
		sprite.setSize(tileSize, tileSize);
		for (int i = 0; i < this.tiles.length; i++) {
			if (!tiles[i].isNoCollision()) {
				pos = this.indexToPos(i);
				tiles[i].draw(sb, sprite, pos);
			}
		}
	}

	public void drawBack (SpriteBatch sb, Sprite sprite, BitmapFont font) {
		if (removed) return;

		Vector2 pos;
		sprite.setSize(tileSize, tileSize);
		for (int i = 0; i < this.tiles.length; i++) {
			if (tiles[i].isNoCollision()) {
				pos = this.indexToPos(i);
				tiles[i].draw(sb, sprite, pos);
			}
		}
	}
}