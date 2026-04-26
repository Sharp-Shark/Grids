package io.github.game;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;

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
    EntityManager entityManager;

    int selectedIndex = -1;
    int frame = 0;

    @Override
    public void create() {
        spriteBatch = new SpriteBatch();
        camera = new OrthographicCamera();
        viewport = new ExtendViewport(10, 10, camera);
        viewportGUI = new ExtendViewport(10, 10);

        Gdx.graphics.setVSync(false);
        Gdx.graphics.setForegroundFPS(300);

        font = new BitmapFont();
	    font.setUseIntegerPositions(false);

        squareTexture = new Texture("square.png");
        squareSprite = new Sprite(squareTexture);
        
        entityManager = new EntityManager();

        int scale = 128;
        Grid world = entityManager.addGrid(new Vector2(-1 * scale, -0.5f * scale), 4 * scale, 1 * scale);
        world.fill(TilePrefab.earth);
        world.splitType = Grid.SplitType.SHED;
        world.immobile = true;

        float x = 0;
        float y = 0.01f;
        for (int i = 0; i < 8; i++) {
            Grid grid = entityManager.addGrid(new Vector2(x, y), 8, 8);
            grid.fill(TilePrefab.solid);
            grid.splitType = Grid.SplitType.REMEMBER;

            x -= 9 * Grid.tileSize;
        }

        Grid grid = entityManager.addGrid(new Vector2(32, y), 128, 128);
        grid.fill(TilePrefab.solid);
        grid.splitType = Grid.SplitType.REMEMBER;

        Entity player = entityManager.addEntity(new Vector2(5, y), 0.35f, 0.7f);
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
        camera.zoom = Math.max(0.1f, Math.min(10f, camera.zoom));

        Vector2 cursorPos = new Vector2(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(cursorPos);

        if (Gdx.input.isTouched()) {
            if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                selectedIndex = -1;

                Entity entity;
                for (int i = 0; i < entityManager.entities.size(); i++) {
                    entity = entityManager.entities.get(i);
                    if (entity.removed || !entity.isTouchingPoint(cursorPos)) continue;
                    selectedIndex = i;
                }
            }
        }

        if (selectedIndex != -1 && entityManager.entities.get(selectedIndex).removed) { selectedIndex = -1; }
        if (selectedIndex != -1) {
            Entity entity = entityManager.entities.get(selectedIndex);

            float force = 16f;
            Vector2 thrust;
            if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
                thrust = new Vector2(cursorPos).sub(entity.getRelativePosition(0f, 0f).add(entity.pos));
            } else {
                thrust = new Vector2(
                    (Gdx.input.isKeyPressed(Input.Keys.RIGHT) ? 1 : 0) - (Gdx.input.isKeyPressed(Input.Keys.LEFT) ? 1 : 0),
                    (Gdx.input.isKeyPressed(Input.Keys.UP) ? 1 : 0) - (Gdx.input.isKeyPressed(Input.Keys.DOWN) ? 1 : 0)
                );
            }
            thrust.clamp(0f, 1f).scl(dt * force);
            entity.vel.add(thrust);

            if (entity instanceof Grid) {
                Grid grid = (Grid) entity;

                float damage = dt * 8f * ((Gdx.input.isKeyPressed(Input.Keys.X) ? 1 : 0) - (Gdx.input.isKeyPressed(Input.Keys.Z) ? 1 : 0));
                int index = grid.posToIndex(cursorPos);
                if (index != -1) {
                    if (grid.isIndexConnected(index)) {
                        if (Gdx.input.isKeyPressed(Input.Keys.V)) {
                            grid.setTilePrefab(index, TilePrefab.solid, true);
                        } else if (Gdx.input.isKeyPressed(Input.Keys.B)) {
                            grid.setTilePrefab(index, TilePrefab.background, true);
                        } else if (Gdx.input.isKeyPressed(Input.Keys.N)) {
                            grid.setTilePrefab(index, TilePrefab.light, true);
                        } else if (Gdx.input.isKeyPressed(Input.Keys.M)) {
                            grid.setTilePrefab(index, TilePrefab.earth, true);
                        }
                        if (damage < 0) {
                            grid.setTileHealth(index, grid.tiles[index].health - damage);
                        }
                    }
                    if (Gdx.input.isKeyPressed(Input.Keys.C)) {
                        grid.setTilePrefab(index, TilePrefab.empty, true);
                    }
                    if (damage > 0) {
                        grid.setTileHealth(index, grid.tiles[index].health - damage);
                    }
                }
                /*
                if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
                    System.out.println("MERGE");
                    for (Grid gridOther : entityManager.grids) {
                        grid.merge(new Grid[]{gridOther});
                    }
                }
                */
            }
        }

        frame += 1;
    }
    
    private void update (float dt) {
        entityManager.update(dt);

        if (selectedIndex != -1 && entityManager.entities.get(selectedIndex).removed) { selectedIndex = -1; }
    }

    private void draw (float dt) {
        ScreenUtils.clear(Color.BLACK);
        viewport.apply();
        spriteBatch.setProjectionMatrix(viewport.getCamera().combined);
        spriteBatch.begin();

        entityManager.draw(spriteBatch, squareSprite, font, viewport);

        if (selectedIndex != -1) {
            Entity entity = entityManager.entities.get(selectedIndex);
            squareSprite.setSize(entity.widthAABB + Grid.tileSize, entity.heightAABB + Grid.tileSize);
            squareSprite.setPosition(entity.pos.x - Grid.tileSize / 2f, entity.pos.y - Grid.tileSize / 2f);
            squareSprite.setColor(new Color(1.0f, 1.0f, 1.0f, 0.1f));
            squareSprite.draw(spriteBatch);
            
            if (entity.entityType == Entity.EntityType.GRID) {
                Vector2 cursorPos = new Vector2(Gdx.input.getX(), Gdx.input.getY());
                viewport.unproject(cursorPos);
                ((Grid) entity).snapPos(cursorPos);
                squareSprite.setSize(Grid.tileSize, Grid.tileSize);
                squareSprite.setPosition(cursorPos.x, cursorPos.y);
                squareSprite.setColor(new Color(1.0f, 1.0f, 1.0f, 0.5f));
                squareSprite.draw(spriteBatch);
            }
        }

        font.getData().setScale(0.04f);
        font.draw(spriteBatch, "Controls: [Q][E] [W][A][S][D] [Z][X] [C][V][B][N][M] [LMB] [RMB] [Arrows]", -6f, -1f);

        spriteBatch.setProjectionMatrix(viewportGUI.getCamera().combined);
        font.getData().setScale(0.05f);
        font.draw(spriteBatch, String.valueOf((int) (1 / dt)), 0f, viewportGUI.unproject(new Vector2(0, 0)).y);

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