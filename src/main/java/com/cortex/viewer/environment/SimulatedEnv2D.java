package com.cortex.viewer.environment;

/*
// 1️⃣ sensing
        float[] lidar = world.sense();

        // 2️⃣ input → rete
        // (qui colleghi il lidar ai neuroni input)
        retina.setFromLidar(lidar, now);

        // 3️⃣ output dalla rete
        float left = motorLeft.getRecentFiringRate(now);
        float right = motorRight.getRecentFiringRate(now);

        // 4️⃣ update mondo
        world.update(left, right);

        // 5️⃣ reward
        float reward = world.reward();
        supervisor.applyReward(reward, now);

        // 6️⃣ rendering
package com;


TileWorld world = new TileWorld(40, 40);
WorldRenderer renderer = new WorldRenderer();


AnimationTimer timer = new AnimationTimer() {

    @Override
    public void handle(long now) {

        
        renderer.render(gc, world, tileSize);
        renderer.drawLidar(gc, world, lidar, tileSize);
    }
};


timer.start();
*/