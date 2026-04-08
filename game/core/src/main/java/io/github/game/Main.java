package io.github.game;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main implements ApplicationListener {
    SpriteBatch spriteBatch;
    OrthographicCamera camera;
    ExtendViewport viewport;
    BitmapFont font;
    Texture squareTexture;
    Sprite squareSprite;
    GridManager gridManager;

    int selectedIndex = -1;
    int frame = 0;

    @Override
    public void create() {
        spriteBatch = new SpriteBatch();
        camera = new OrthographicCamera();
        viewport = new ExtendViewport(10, 10, camera);

        font = new BitmapFont();
	    font.setUseIntegerPositions(false);

        squareTexture = new Texture("square.png");
        squareSprite = new Sprite(squareTexture);
        
        gridManager = new GridManager();
        gridManager.addGrid(new Vector2(0, 0), 8, 8).fill(TilePrefab.solid);
        gridManager.addGrid(new Vector2(5, 0), 16, 16).fill(TilePrefab.solid);
        gridManager.addGrid(new Vector2(-9, 0), 8, 8).fill(TilePrefab.solid);
        gridManager.addGrid(new Vector2(-20, 0), 8, 8).fill(TilePrefab.solid);
    }

    @Override
    public void resize(int width, int height) {
        if(width <= 0 || height <= 0) return;

        viewport.update(width, height, false);
    }

    @Override
    public void render() {
		float dt = Gdx.graphics.getDeltaTime();

        input(dt);
        update(dt);
        draw(spriteBatch);
    }

    private void input(float dt) {
        float speed = 8f;
        float damage = 4f;

        camera.position.x += dt * speed * camera.zoom * ((Gdx.input.isKeyPressed(Input.Keys.D) ? 1 : 0) - (Gdx.input.isKeyPressed(Input.Keys.A) ? 1 : 0));
        camera.position.y += dt * speed * camera.zoom * ((Gdx.input.isKeyPressed(Input.Keys.W) ? 1 : 0) - (Gdx.input.isKeyPressed(Input.Keys.S) ? 1 : 0));
        camera.zoom += dt * speed * (0.2f) * camera.zoom * ((Gdx.input.isKeyPressed(Input.Keys.E) ? 1 : 0) - (Gdx.input.isKeyPressed(Input.Keys.Q) ? 1 : 0));

        Vector2 cursorPos = new Vector2(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(cursorPos);

        if (Gdx.input.isTouched()) {
            if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                int priorityWinner, priority;
                priorityWinner = 0;
                priority = 0;

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
            Vector2 thrust;
            if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
                thrust = new Vector2(cursorPos).sub(grid.getRelativePosition(0f, 0f).add(grid.pos));
            } else {
                thrust = new Vector2(
                    (Gdx.input.isKeyPressed(Input.Keys.RIGHT) ? 1 : 0) - (Gdx.input.isKeyPressed(Input.Keys.LEFT) ? 1 : 0),
                    (Gdx.input.isKeyPressed(Input.Keys.UP) ? 1 : 0) - (Gdx.input.isKeyPressed(Input.Keys.DOWN) ? 1 : 0)
                );
            }
            thrust.clamp(0f, 1f).scl(dt * speed);
            grid.vel.add(thrust);
            int index = grid.posToIndex(cursorPos);
            if (index != -1) {
                if (Gdx.input.isKeyPressed(Input.Keys.Z)) {
                    grid.setTileHealth(index, grid.tiles[index].health + dt * damage);
                } else if (Gdx.input.isKeyPressed(Input.Keys.X)) {
                    grid.setTileHealth(index, grid.tiles[index].health - dt * damage);
                } else if (Gdx.input.isKeyPressed(Input.Keys.C)) {
                    grid.tiles[index].setPrefab(TilePrefab.empty, false);
                } else if (Gdx.input.isKeyPressed(Input.Keys.V)) {
                    grid.tiles[index].setPrefab(TilePrefab.solid, false);
                } else if (Gdx.input.isKeyPressed(Input.Keys.B)) {
                    grid.tiles[index].setPrefab(TilePrefab.background, false);
                }
            }
        }

        frame += 1;
    }
    
    private void update (float dt) {
        gridManager.update(dt);
    }

    private void draw (SpriteBatch sb) {
        ScreenUtils.clear(Color.BLACK);
        viewport.apply();
        spriteBatch.setProjectionMatrix(viewport.getCamera().combined);
        sb.begin();

        if (selectedIndex != -1) {
            Grid grid = gridManager.grids.get(selectedIndex);
            squareSprite.setSize(Grid.tileSize * (grid.width + 1), Grid.tileSize * (grid.height + 1));
            squareSprite.setPosition(grid.pos.x - Grid.tileSize / 2f, grid.pos.y - Grid.tileSize / 2f);
            squareSprite.setColor(new Color(1.0f, 1.0f, 1.0f, 0.2f));
            squareSprite.draw(sb);
        }

        gridManager.draw(sb, squareSprite, font);

        font.getData().setScale(0.04f);
        font.draw(sb, "Controls: [W][A][S][D] [Z][X] [C][V][B] [LMB] [RMB]", -6f, -1f);

        sb.end();
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