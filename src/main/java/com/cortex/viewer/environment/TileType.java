package com.cortex.viewer.environment;

public enum TileType {
	EMPTY,
    WALL,
    FOOD,
    DANGER;

    public boolean isWalkable() {
        return this != WALL;
    }
}
