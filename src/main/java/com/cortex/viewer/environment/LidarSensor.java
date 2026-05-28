package com.cortex.viewer.environment;

public class LidarSensor {

    private final int rays;
    private final float fov;
    private final float maxDistance;

    public LidarSensor(int rays, float fovDeg, float maxDistance) {
        this.rays = rays;
        this.fov = (float) Math.toRadians(fovDeg);
        this.maxDistance = maxDistance;
    }

    public float[] scan(TileMap map, Agent agent) {
        float[] out = new float[rays];

        for (int i = 0; i < rays; i++) {

            float t = (i / (float)(rays - 1)) - 0.5f;
            float rayAngle = agent.angle + t * fov;

            float dx = (float)Math.cos(rayAngle);
            float dy = (float)Math.sin(rayAngle);

            out[i] = cast(map, agent.x, agent.y, dx, dy);
        }

        return out;
    }

    private float cast(TileMap map, float ox, float oy, float dx, float dy) {

        float step = 0.1f;

        for (float t = 0; t < maxDistance; t += step) {

            int tx = (int)(ox + dx * t);
            int ty = (int)(oy + dy * t);

            if (!map.isInside(tx, ty))
                return t;

            if (map.get(tx, ty) == TileType.WALL)
                return t;
        }

        return maxDistance;
    }
}
