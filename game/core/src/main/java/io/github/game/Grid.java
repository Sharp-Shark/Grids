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
	final static float tileSize = 0.5f;

	Vector2 pos;
	Vector2 vel;
	Vector2 gravity = new Vector2(0, -5);
	float friction = 0.9f;
	float mass = 0;
	boolean immobile = false;

	int width;
	int height;
	Tile[] tiles;

	boolean atomic = false; // cannot be split
	ArrayList<Integer> deletedIndexes;

	GridManager gridManager;
	boolean removed = false;

	public Grid (int width, int height, GridManager gridManager) {
		pos = new Vector2();
		vel = new Vector2();
		gravity = new Vector2(gravity);

		this.width = width;
		this.height = height;
		tiles = new Tile[width * height];
		Tile tile;
		for (int i = 0; i < this.tiles.length; i++) {
			tile = new Tile(this, TilePrefab.empty);
			tiles[i] = tile;
			mass += tile.getMass();
		}
		deletedIndexes = new ArrayList<Integer>(width * height);
		
		this.gridManager = gridManager;
		this.removed = false;
	}

	public void fill (TilePrefab newPrefab) {
		for (int i = 0; i < this.tiles.length; i++) {
			this.setTilePrefab(i, newPrefab, true);
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
		if (!atomic && oldHealth > 0 && tiles[index].health <= 0) {
			deletedIndexes.add(index);
		}
	}

	public void setTilePrefab (int index, TilePrefab tilePrefab, boolean revive) {
		float oldHealth = tiles[index].health;
		tiles[index].setPrefab(tilePrefab, revive);
		if (!atomic && oldHealth > 0 && tiles[index].health <= 0) {
			deletedIndexes.add(index);
		}
	}

	/*
	public void logStuff (int[] tileRegion, int[] regionMap) {
		System.out.println("LOGGIN");
		for (int y = height - 1; y >= 0; y--) {
			String row = "";
			for (int x = 0; x < width; x++) {
				int i = x + y * width;
				if (tileRegion[i] != -1) {
					row = row.concat(String.valueOf(regionMap[tileRegion[i]]));
				} else {
					if (tiles[i].health <= 0) {
						row = row.concat("#");
					} else {
						row = row.concat("_");
					}
				}
			}
			System.out.println(row);
		}
	}
	*/

	public void logTime (long start, String text) {
		final float e6 = 1_000_000;
		System.out.println(text.concat(String.valueOf((System.nanoTime()- start) / e6)));
	}

	public void split () {
		if (atomic || deletedIndexes.size() <= 0) return;

		int[] tileRegion = new int[tiles.length]; for (int i = 0; i < tiles.length; i++) { tileRegion[i] = -1; }
		ArrayDeque<Integer> queue = new ArrayDeque<Integer>();
		int regionCount = deletedIndexes.size() * neighbours.length;
		int[] regionMap = new int[regionCount]; for (int i = 0; i < regionMap.length; i++) { regionMap[i] = i; }
		float[] regionSortMetric = new float[regionCount]; // currently is the # of tiles, but could be changed to use the cumulative mass, "control points" or cost

		for (int region = 0; region < regionCount; region++) {
			int indexDeleted = deletedIndexes.get(region / neighbours.length);
			int[] neighbour = neighbours[region % neighbours.length];
			int index = translateIndex(indexDeleted, neighbour[0], neighbour[1]);
			if (index != -1 && tiles[index].health > 0) {
				if (tileRegion[index] != -1) {
					int changed = regionMap[tileRegion[index]];
					for (int i = 0; i < regionMap.length; i++) {
						if (regionMap[i] == changed) regionMap[i] = regionMap[region];
					}
				} else {
					queue.addLast(index);
					tileRegion[index] = region;
					regionSortMetric[region] += 1;
					regionMap[region] = region;
				}
			} else {
				regionMap[region] = -1;
			}
		}
		deletedIndexes.clear();

		while (queue.size() > 0) {
			int indexDeleted = queue.pop();
			int region = tileRegion[indexDeleted];
			for (int[] neighbour : neighbours) {
				int index = translateIndex(indexDeleted, neighbour[0], neighbour[1]);
				if (index != -1 && tiles[index].health > 0) {
					if (tileRegion[index] != -1) {
						int changed = regionMap[tileRegion[index]];
						for (int i = 0; i < regionMap.length; i++) {
							if (regionMap[i] == changed) regionMap[i] = regionMap[region];
						}
					} else {
						queue.addLast(index);
						tileRegion[index] = region;
						regionSortMetric[region] += 1;
					}
				}
			}
			boolean halt = true;
			int first = -1;
			for (int i = 0; i < regionMap.length; i++) {
				if (regionMap[i] != -1) {
					if (first == -1) {
						first = regionMap[i];
					} else if (first != regionMap[i]) {
						halt = false;
						break;
					}
				}
			}
			if (halt) { return; }
		}
		
		int regionCountOld = regionCount;
		boolean[] regionExists = new boolean[regionCount];
		int[] regions = new int[regionCount];
		regionCount = 0;
		for (int region = 0; region < regionMap.length; region++) {
			if (regionMap[region] != -1 && !regionExists[regionMap[region]]) {
				regionExists[regionMap[region]] = true;
				regions[regionCount] = regionMap[region];
				regionCount += 1;
			}
			// transfer counts
			if (regionMap[region] != -1 && regionMap[region] != region) {
				regionSortMetric[regionMap[region]]  += regionSortMetric[region];
				regionSortMetric[region] = 0;
			}
		}

		if (regionCount <= 1) { return; }

		Grid[] grids = new Grid[regionCount];
		for (int i = 1; i < regionCount; i++) {
			Grid grid = gridManager.addGrid(new Vector2(pos), width, height);
			grid.vel = new Vector2(this.vel);
			grids[i] = grid;
		}

		// quadratic sort
		int[] regionGrid = new int[regionCountOld]; for (int i = 0; i < regionGrid.length; i++) { regionGrid[i] = -1; }
		for (int i = 0; i < regionCount; i++) {
			int region = 0;
			int winnerIndex = 0;
			int winner = -1;
			float winnerSortMetric = 0;
			for (int j = i; j < regionCount; j++) {
				region = regions[j];
				if (regionSortMetric[region] >= winnerSortMetric) {
					winnerIndex = j;
					winner = region;
					winnerSortMetric = regionSortMetric[region];
				}
			}
			if (winner != -1) {
				int temp = regions[i];
				regions[i] = winner;
				regions[winnerIndex] = temp;
				regionGrid[winner] = i;
			}
		}

		for (int index = 0; index < tileRegion.length; index++) {
			if (tileRegion[index] == -1) continue;
			int region = regionMap[tileRegion[index]];
			if (region > -1 && regionExists[region] && (regionGrid[region] > 0)) {
				Grid grid = grids[regionGrid[region]];
				Tile tile = grid.tiles[index];
				grid.tiles[index] = tiles[index];
				grid.tiles[index].setGrid(grid);
				tiles[index] = tile;
				tiles[index].setGrid(this);
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

		if (grid.tiles.length < tiles.length) {
			return grid.isTouchingGridPerTile(this);
		}

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

		if (!immobile) {
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
			// gravity
			vel.add(new Vector2(gravity).scl(dt));
			// friction
			vel.scl((float) Math.pow(friction, dt));
		} else {
			vel.set(0, 0);
		}

		this.split();
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