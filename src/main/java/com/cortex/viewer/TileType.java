
public enum TileType {public enum Tile EMPTY,
    WALL,
    FOOD,
    DANGER;

    public boolean isWalkable() {
        return this != WALL;
    }
}
