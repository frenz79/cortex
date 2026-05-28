
public class RewardSystem {

    public float compute(TileMap map, Agent agent) {

        int tx = (int) agent.x;
        int ty = (int) agent.y;

        TileType t = map.get(tx, ty);

        switch (t) {
            case FOOD: return 1f;
            case DANGER: return -1f;
            default: return -0.001f;
        }
    }
}
