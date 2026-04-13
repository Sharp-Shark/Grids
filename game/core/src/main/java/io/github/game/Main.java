package io.github.game;

import org.w3c.dom.css.Rect;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main implements ApplicationListener {
    SpriteBatch spriteBatch;
    OrthographicCamera camera;
    ExtendViewport viewport;
    ExtendViewport viewportGUI;
    BitmapFont font;
    Texture squareTexture;
    Sprite squareSprite;
    GridManager gridManager;

    int selectedIndex = -1;
    int frame = 0;

    // for debugging performance
	static public void logTime (long start, String text) {
		final float e6 = 1_000_000;
		System.out.println(text.concat(String.valueOf((System.nanoTime()- start) / e6)));
	}

    @Override
    public void create() {
        spriteBatch = new SpriteBatch();
        camera = new OrthographicCamera();
        viewport = new ExtendViewport(10, 10, camera);
        viewportGUI = new ExtendViewport(10, 10);

        font = new BitmapFont();
	    font.setUseIntegerPositions(false);

        squareTexture = new Texture("square.png");
        squareSprite = new Sprite(squareTexture);
        
        gridManager = new GridManager();

        Grid world = gridManager.addGrid(new Vector2(-128, -128), 512, 256);
        world.fill(TilePrefab.earth);
        world.splitType = Grid.SplitType.SHED;
        world.immobile = true;

        float x = 0;
        for (int i = 0; i < 16; i++) {
            Grid grid = gridManager.addGrid(new Vector2(x, 0), 8, 8);
            grid.fill(TilePrefab.solid);
            grid.splitType = Grid.SplitType.REMEMBER;

            x -= 9 * Grid.tileSize;
        }
    }

    @Override
    public void resize(int width, int height) {
        if(width <= 0 || height <= 0) return;

        viewport.update(width, height, false);
        viewportGUI.update(width, height, true);
    }

    @Override
    public void render() {
		float dt = Gdx.graphics.getDeltaTime();

        input(dt);
        update(dt);
        draw(dt);
    }

    private void input(float dt) {
        float speed = 8f;

        camera.position.x += dt * speed * camera.zoom * ((Gdx.input.isKeyPressed(Input.Keys.D) ? 1 : 0) - (Gdx.input.isKeyPressed(Input.Keys.A) ? 1 : 0));
        camera.position.y += dt * speed * camera.zoom * ((Gdx.input.isKeyPressed(Input.Keys.W) ? 1 : 0) - (Gdx.input.isKeyPressed(Input.Keys.S) ? 1 : 0));
        camera.zoom += dt * speed * (0.2f) * camera.zoom * ((Gdx.input.isKeyPressed(Input.Keys.E) ? 1 : 0) - (Gdx.input.isKeyPressed(Input.Keys.Q) ? 1 : 0));
        //camera.zoom = Math.min(10f, camera.zoom);

        Vector2 cursorPos = new Vector2(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(cursorPos);

        if (Gdx.input.isTouched()) {
            if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                int priorityWinner, priority;
                priorityWinner = 0;
                priority = 0;
                selectedIndex = -1;

                Grid grid;
                for (int i = 0; i < gridManager.grids.size(); i++) {
                    grid = gridManager.grids.get(i);
                    if (grid.removed) continue;
                    int index = grid.posToIndex(cursorPos);
                    if (index != -1 && !grid.tiles[index].isNoDraw()) {
                        priority = grid.tiles[index].isNoCollision() ? 0 : 1;
                        if (priority >= priorityWinner) {
                            selectedIndex = i;
                            priorityWinner = priority;
                        }
                    }
                }
            }
        }

        if (selectedIndex != -1 && gridManager.grids.get(selectedIndex).removed) { selectedIndex = -1; }
        if (selectedIndex != -1) {
            Grid grid = gridManager.grids.get(selectedIndex);

            float force = 10f;
            Vector2 thrust;
            if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
                thrust = new Vector2(cursorPos).sub(grid.getRelativePosition(0f, 0f).add(grid.pos));
            } else {
                thrust = new Vector2(
                    (Gdx.input.isKeyPressed(Input.Keys.RIGHT) ? 1 : 0) - (Gdx.input.isKeyPressed(Input.Keys.LEFT) ? 1 : 0),
                    (Gdx.input.isKeyPressed(Input.Keys.UP) ? 1 : 0) - (Gdx.input.isKeyPressed(Input.Keys.DOWN) ? 1 : 0)
                );
            }
            thrust.clamp(0f, 1f).scl(dt * force);
            grid.vel.add(thrust);

            float damage = dt * 8f * ((Gdx.input.isKeyPressed(Input.Keys.X) ? 1 : 0) - (Gdx.input.isKeyPressed(Input.Keys.Z) ? 1 : 0));
            int index = grid.posToIndex(cursorPos);
            if (index != -1) {
                if (Gdx.input.isKeyPressed(Input.Keys.C)) {
                    grid.setTilePrefab(index, TilePrefab.empty, true);
                } else if (Gdx.input.isKeyPressed(Input.Keys.V)) {
                    grid.setTilePrefab(index, TilePrefab.solid, true);
                } else if (Gdx.input.isKeyPressed(Input.Keys.B)) {
                    grid.setTilePrefab(index, TilePrefab.background, true);
                }
                if (damage != 0) {
                    grid.setTileHealth(index, grid.tiles[index].health - damage);
                }
            }
        }

        frame += 1;
    }
    
    private void update (float dt) {
        gridManager.update(dt);

        if (selectedIndex != -1 && gridManager.grids.get(selectedIndex).removed) { selectedIndex = -1; }
    }

    private void draw (float dt) {
        ScreenUtils.clear(Color.BLACK);
        viewport.apply();
        spriteBatch.setProjectionMatrix(viewport.getCamera().combined);
        spriteBatch.begin();

        gridManager.draw(spriteBatch, squareSprite, font, viewport);
        
        if (selectedIndex != -1) {
            Grid grid = gridManager.grids.get(selectedIndex);
            squareSprite.setSize(Grid.tileSize * (grid.width + 1), Grid.tileSize * (grid.height + 1));
            squareSprite.setPosition(grid.pos.x - Grid.tileSize / 2f, grid.pos.y - Grid.tileSize / 2f);
            squareSprite.setColor(new Color(1.0f, 1.0f, 1.0f, 0.1f));
            squareSprite.draw(spriteBatch);
        }


        font.getData().setScale(0.04f);
        font.draw(spriteBatch, "Controls: [Q][E] [W][A][S][D] [Z][X] [C][V][B] [LMB] [RMB] [Arrows]", -6f, -1f);

        spriteBatch.setProjectionMatrix(viewportGUI.getCamera().combined);
        font.getData().setScale(0.05f);
        font.draw(spriteBatch, String.valueOf((int) (1 / dt)), 0f, 10);

        spriteBatch.end();
    }

    @Override
    public void pause() {
        // Invoked when your application is paused.
    }

    @Override
    public void resume() {
        // Invoked when your application is resumed after pause.
    }

    @Override
    public void dispose() {
        // Destroy application's resources here.
    }
}