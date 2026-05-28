package com.cortex.viewer.environment;

public class Agent {

    public float x;
    public float y;
    public float angle;

    public Agent(float x, float y) {
        this.x = x;
        this.y = y;
        this.angle = 0;
    }

    public void update(float motorLeft, float motorRight, TileMap map) {

        float speed = (motorLeft + motorRight) * 0.5f;
        float rotation = (motorRight - motorLeft);

        angle += rotation * 0.1f;

        float nx = (float)(x + Math.cos(angle) * speed * 0.1f);
        float ny = (float)(y + Math.sin(angle) * speed * 0.1f);

        if (map.isWalkable(nx, ny)) {
            x = nx;
            y = ny;
        }
    }
}
