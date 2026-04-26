package io.github.game;

import java.util.ArrayList;
import java.util.HashMap;

public class TileStructurePrefab {
    final static TileStructurePrefab test = new TileStructurePrefab(
        "test",
        2,
        2,
        new TilePrefab[]{
            TilePrefab.solid, TilePrefab.solid,
            TilePrefab.solid, TilePrefab.solid,
        }
    );
    static ArrayList<TileStructurePrefab> prefabList;
    static HashMap<String, TileStructurePrefab> prefabMap;

    String name;
    int width;
    int height;
    TilePrefab[] tilePrefabs;

    public TileStructurePrefab (String name, int width, int height, TilePrefab[] tilePrefabs) {
        this.name = name;
        this.width = width;
        this.height = height;
        this.tilePrefabs = tilePrefabs;

        if (prefabList == null) { prefabList = new ArrayList<TileStructurePrefab>(); }
        if (prefabMap == null) { prefabMap = new HashMap<String, TileStructurePrefab>(); }
        prefabList.add(this);
        prefabMap.put(name, this);
    }
}