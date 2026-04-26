package io.github.game;

import java.util.ArrayList;
import java.util.HashMap;

import com.badlogic.gdx.graphics.Color;

public class TilePrefab {
    final static TilePrefab empty = new TilePrefab(
        "empty",
        0f,
        0f,
        0f,
        true,
        Color.RED,
        true
    );
    final static TilePrefab solid = new TilePrefab(
        "solid",
        0f,
        1f,
        1f,
        false,
        Color.CYAN,
        false
    );
    final static TilePrefab background = new TilePrefab(
        "background",
        0f,
        1f,
        0f,
        true,
        Color.NAVY,
        false
    );
    final static TilePrefab light = new TilePrefab(
        "light",
        0f,
        1f,
        0.1f,
        false,
        Color.SKY,
        false
    );
    final static TilePrefab earth = new TilePrefab(
        "earth",
        0f,
        1f,
        1f,
        false,
        Color.BROWN,
        false
    );
    
    static ArrayList<TilePrefab> prefabList;
    static HashMap<String, TilePrefab> prefabMap;

    String name;
    float minHealth;
    float maxHealth;
    float mass;
    float area;
    boolean noCollision;
    Color color;
    boolean noDraw;

    public TilePrefab (String name, float minHealth, float maxHealth, float mass, boolean noCollision, Color color, boolean noDraw) {
        this.name = name;
        this.minHealth = minHealth;
        this.maxHealth = maxHealth;
        this.mass = mass;
        this.area = noCollision ? 0 : Grid.tileArea;
        this.noCollision = noCollision;
        this.color = color;
        this.noDraw = noDraw;

        if (prefabList == null) { prefabList = new ArrayList<TilePrefab>(); }
        if (prefabMap == null) { prefabMap = new HashMap<String, TilePrefab>(); }
        prefabList.add(this);
        prefabMap.put(name, this);
    }
}