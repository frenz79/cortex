package com.cortex.viewer.environment;

public class TileWorld {
	
	private final TileMap map;
    private final Agent agent;
    private final LidarSensor lidar;
    private final RewardSystem reward;

    public TileWorld(int w, int h) {

        this.map = new TileMap(w, h);
        this.agent = new Agent(w * 0.5f, h * 0.5f);
        this.lidar = new LidarSensor(16, 120, 10f);
        this.reward = new RewardSystem();

        generateBasicWorld();
    }

    private void generateBasicWorld() {

        // muri bordi
        for (int x = 0; x < map.getWidth(); x++) {
            map.set(x, 0, TileType.WALL);
            map.set(x, map.getHeight()-1, TileType.WALL);
        }

        for (int y = 0; y < map.getHeight(); y++) {
            map.set(0, y, TileType.WALL);
            map.set(map.getWidth()-1, y, TileType.WALL);
        }

        // qualche ostacolo
        for (int i = 5; i < 15; i++) {
            map.set(i, 10, TileType.WALL);
        }

        map.set(20, 20, TileType.FOOD);
    }

    public float[] sense() {
        return lidar.scan(map, agent);
    }

    public void update(float left, float right) {
        agent.update(left, right, map);
    }

    public float reward() {
        return reward.compute(map, agent);
    }

    public TileMap getMap() { return map; }
    public Agent getAgent() { return agent; }
}
