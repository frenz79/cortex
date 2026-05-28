package com.cortex.viewer.environment;

public class TileMap {

    private final int width;
    private final int height;
    private final TileType[][] grid;

    public TileMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.grid = new TileType[width][height];
        fill(TileType.EMPTY);
    }

    public void fill(TileType type) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                grid[x][y] = type;
            }
        }
    }

    public boolean isInside(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    public TileType get(int x, int y) {
    	if (!isInside(x, y)) return TileType.WALL;
        return grid[x][y];
    }

    public void set(int x, int y, TileType type) {
        if (isInside(x, y)) {
            grid[x][y] = type;
        }
    }

    public boolean isWalkable(float x, float y) {
        int tx = (int) x;
        int ty = (int) y;
        return isInside(tx, ty) && grid[tx][ty].isWalkable();
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
}
