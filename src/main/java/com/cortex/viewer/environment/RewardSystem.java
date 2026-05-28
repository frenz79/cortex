package com.cortex.viewer.environment;

public class RewardSystem {

    public float compute(TileMap map, Agent agent) {

        int tx = (int) agent.x;
        int ty = (int) agent.y;

        TileType t = map.get(tx, ty);

        switch (t) {
            case FOOD: return 1f;
            case DANGER: return -1f;
            default: return evaluateReward();
        }
    }

    private float evaluateReward(){
    	/*
        reward += speed * 0.02f;
        
        // penalità collisioni
        if (collision) reward -= 1f;
        
        // penalità debole inattività
        if (speed < 0.01f) reward -= 0.001f;
        return reward;
        */
    	return 0.0f;
    }
}
