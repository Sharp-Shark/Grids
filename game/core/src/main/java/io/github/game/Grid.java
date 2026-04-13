package io.github.game;

import java.lang.Math;
import java.util.ArrayDeque;
import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

public class Grid {
	final static Vector2[] cornerOffsets = {new Vector2(0, 0), new Vector2(1, 0), new Vector2(1, 1), new Vector2(0, 1)};
	final static int[][] neighbours = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};
	final static float tileSize = 0.5f;

	public enum SplitType {
		DEFAULT,
		REMEMBER,
		SHED,
		REMOVE,
		ATOMIC
	}

	Vector2 pos;
	Vector2 vel;
	Vector2 gravity = new Vector2(0, -5);
	float friction = 0.9f;
	float mass = 0;
	boolean immobile = false;

	int width;
	int height;
	Tile[] tiles;

	SplitType splitType = SplitType.DEFAULT;
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
		}
		deletedIndexes = new ArrayList<Integer>(width * height);
		
		this.gridManager = gridManager;
		removed = false;
	}

	public void resize (int translateX, int translateY, int newWidth, int newHeight) {
		Tile[] newTiles = new Tile[newWidth * newHeight];
		this.mass = 0;

		Tile tile;
		for (int index = 0; index < newTiles.length; index++) {
			int x = index % newWidth + translateX;
			int y = index / newWidth + translateY;
			if (x < 0 || y < 0 || x > width - 1 || y > height - 1) {
				tile = new Tile(this, TilePrefab.empty);
				newTiles[index] = tile;
			} else {
				tile = tiles[x + y * width];
				newTiles[index] = tile;
				this.mass += tile.getMass();
			}
		}

		width = newWidth;
		height = newHeight;
		tiles = newTiles;
		pos.add(translateX * tileSize, translateY * tileSize);
	}

	public void fill (TilePrefab newPrefab) {
		for (int i = 0; i < tiles.length; i++) {
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
		int y = index / width + translateY;
		if (x < 0) return -1;
		if (y < 0) return -1;
		if (x >= width) return -1;
		if (y >= height) return -1;
		return x + y * width;
	}

	public int posToIndex (Vector2 pos) {
		if (pos.x - this.pos.x <= 0) return -1;
		if (pos.y - this.pos.y <= 0) return -1;
		if (pos.x - this.pos.x >= tileSize * width) return -1;
		if (pos.y - this.pos.y >= tileSize * height) return -1;
		int x = (int) ((pos.x - this.pos.x) / tileSize);
		int y = (int) ((pos.y - this.pos.y) / tileSize);
		return Math.min(width - 1, x) + Math.min(height - 1, y) * width;
	}

	public void setTileHealth (int index, float newHealth) {
		float oldHealth = tiles[index].health;
		tiles[index].setHealth(newHealth);
		if ((splitType != SplitType.ATOMIC) && oldHealth > 0 && tiles[index].health <= 0) {
			deletedIndexes.add(index);
		}
	}

	public void setTilePrefab (int index, TilePrefab tilePrefab, boolean revive) {
		float oldHealth = tiles[index].health;
		tiles[index].setPrefab(tilePrefab, revive);
		if ((splitType != SplitType.ATOMIC) && oldHealth > 0 && tiles[index].health <= 0) {
			deletedIndexes.add(index);
		}
	}

	public void split () {
		if ((splitType == SplitType.ATOMIC) || deletedIndexes.size() <= 0) return;

		int[] tileRegion = new int[tiles.length]; for (int i = 0; i < tiles.length; i++) { tileRegion[i] = -1; }
		ArrayDeque<Integer> queue = new ArrayDeque<Integer>();
		int regionCount = deletedIndexes.size() * neighbours.length;
		int[] regionMap = new int[regionCount]; for (int i = 0; i < regionMap.length; i++) { regionMap[i] = i; }
		float[] regionSortMetric = new float[regionCount]; // currently is the # of tiles, but could be changed to use the cumulative mass, "control points" or "material cost"
		int[][] regionRect = new int[regionCount][];  for (int i = 0; i < regionMap.length; i++) { regionRect[i] = new int[]{0, 0, 0, 0}; }

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
					int x = index % width; int y = index / width;
					regionRect[region][0] = x;
					regionRect[region][1] = y;
					regionRect[region][2] = x;
					regionRect[region][3] = y;
				}
			} else {
				regionMap[region] = -1;
			}
		}
		int[] deletedIndexesTemp = new int[deletedIndexes.size()]; for (int i = 0; i < deletedIndexes.size(); i++) { deletedIndexesTemp[i] = deletedIndexes.get(i); }
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
							if (regionMap[i] == changed) {
								regionMap[i] = regionMap[region];
							}
						}
					} else {
						queue.addLast(index);
						tileRegion[index] = region;
						regionSortMetric[region] += 1;
						int x = index % width; int y = index / width;
						regionRect[region][0] = Math.min(regionRect[region][0], x);
						regionRect[region][1] = Math.min(regionRect[region][1], y);
						regionRect[region][2] = Math.max(regionRect[region][2], x);
						regionRect[region][3] = Math.max(regionRect[region][3], y);
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
			// transfer
			if (regionMap[region] != -1 && regionMap[region] != region) {
				regionSortMetric[regionMap[region]]  += regionSortMetric[region];
				regionSortMetric[region] = 0;
				
				regionRect[regionMap[region]][0] = Math.min(regionRect[regionMap[region]][0], regionRect[region][0]);
				regionRect[regionMap[region]][1] = Math.min(regionRect[regionMap[region]][1], regionRect[region][1]);
				regionRect[regionMap[region]][2] = Math.max(regionRect[regionMap[region]][2], regionRect[region][2]);
				regionRect[regionMap[region]][3] = Math.max(regionRect[regionMap[region]][3], regionRect[region][3]);
			}
		}

		if (regionCount <= 1) { return; }

		if (splitType == SplitType.REMOVE) {
			removed = true;
		} else {
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

			if (splitType == SplitType.SHED) {
				for (int index = 0; index < tileRegion.length; index++) {
					if (tileRegion[index] == -1) continue;
					int region = regionMap[tileRegion[index]]; 
					if (region > -1 && regionExists[region] && (regionGrid[region] > 0)) {
						this.setTilePrefab(index, TilePrefab.empty, true);
					}
				}
			} else {
				Grid[] grids = new Grid[regionCount];
				for (int i = 1; i < regionCount; i++) {
					int[] rect = regionRect[regions[i]];
					Vector2 pos = new Vector2(this.pos).add(rect[0] * tileSize, rect[1] * tileSize);
					Grid grid = gridManager.addGrid(pos, rect[2] - rect[0] + 1, rect[3] - rect[1] + 1);
					grid.vel = new Vector2(this.vel);
					grids[i] = grid;
				}

				for (int index = 0; index < tileRegion.length; index++) {
					if (tileRegion[index] == -1) continue;
					int region = regionMap[tileRegion[index]]; 
					if (region > -1 && regionExists[region] && (regionGrid[region] > 0)) {
						Grid grid = grids[regionGrid[region]];
						int indexMapped = index % width - regionRect[region][0] + (index / width - regionRect[region][1]) * grid.width;
						Tile tile = grid.tiles[indexMapped];
						grid.tiles[indexMapped] = tiles[index];
						grid.tiles[indexMapped].setGrid(grid);
						tiles[index] = tile;
						tiles[index].setGrid(this);
						if (splitType == SplitType.REMEMBER) this.setTilePrefab(index, grid.tiles[indexMapped].prefab, false);
					}
				}
			}

			if (splitType != SplitType.REMEMBER) {
				int[] rect = regionRect[regions[0]];
				this.resize(rect[0], rect[1], rect[2] - rect[0] + 1, rect[3] - rect[1] + 1);
			}
		}
	}

	public void merge (Grid[] grids) {
		// to be done: makes a new grid with the tiles of all the other grids and delete the other grids
		// if I ever add grid resizing, instead it could pick the biggest grid, resize that so all the other grids fit
		// and then add the tiles from the other grids and finally delete the other grids
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

		for (int i = 0; i < tiles.length; i++) {
			Tile tile = tiles[i];
			if (!tile.isNoCollision()) {
				for (Vector2 cornerOffset : cornerOffsets) {
					Vector2 pos = this.indexToPos(i).add(new Vector2(cornerOffset).scl(tileSize));
					int index = grid.posToIndex(pos);
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

	public void update (float dt) {
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
			grid = this.getCollidingGrid(gridManager.grids);
			if (grid != null) {
				pos.x -= vel.x * dt;

				totalMass = mass + grid.mass;
				dv = grid.vel.x - vel.x;
				vel.x += (restitution + 1) * grid.mass * dv / totalMass;
				grid.vel.x -= (restitution + 1) * mass * dv / totalMass;
			}
			// vertical
			pos.y += vel.y * dt;
			grid = this.getCollidingGrid(gridManager.grids);
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

	public void draw (SpriteBatch sb, Sprite sprite, BitmapFont font, Viewport viewport, boolean drawFront) {
		if (removed) return;

		sprite.setSize(tileSize, tileSize);

		Vector2 blc = viewport.unproject(new Vector2(0, viewport.getScreenHeight()));
		Vector2 trc = viewport.unproject(new Vector2(viewport.getScreenWidth(), 0));

		int offsetX = (int) (Math.max(0, blc.x - pos.x) / tileSize);
		int y = (int) (Math.max(0, blc.y - pos.y) / tileSize);
		Vector2 drawPos = new Vector2(pos.x + offsetX * tileSize, pos.y + y * tileSize);
		while (y < height && drawPos.y <= trc.y) {
			int x = offsetX;
			while (x < width && drawPos.x <= trc.x) {
				int index = this.translateIndex(0, x, y);
				if (index != -1 && tiles[index].isNoCollision() == drawFront) {
					tiles[index].draw(sb, sprite, drawPos);
				}
				x += 1;
				drawPos.x += tileSize;
			}
			y += 1;
			drawPos.x = pos.x + offsetX * tileSize;
			drawPos.y += tileSize;
		}
	}
}