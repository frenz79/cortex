
TileWorld world = new TileWorld(40, 40);
WorldRenderer renderer = new WorldRenderer();

AnimationTimer timer = new AnimationTimer() {

    @Override
    public void handle(long now) {

        float[] lidar = world.sense();

        // 👉 qui colleghi la tua rete
        // retina/lidar → neuroni → output

        float left = motorLeft.getRecentFiringRate(now);
        float right = motorRight.getRecentFiringRate(now);

        world.update(left, right);

        float reward = world.reward();
        supervisor.processReward(reward, now);

        renderer.render(gc, world, 20);
    }
};

timer.start();
