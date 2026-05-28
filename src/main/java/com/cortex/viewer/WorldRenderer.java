
public class WorldRenderer {

    public void render(GraphicsContext g, TileWorld world, double tileSize) {

        TileMap map = world.getMap();
        Agent agent = world.getAgent();

        g.setFill(Color.BLACK);
        g.fillRect(0, 0, map.getWidth()*tileSize, map.getHeight()*tileSize);

        // tiles
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {

                switch (map.get(x,y)) {
                    case WALL -> g.setFill(Color.DARKGRAY);
                    case FOOD -> g.setFill(Color.GREEN);
                    case DANGER -> g.setFill(Color.RED);
                    default -> g.setFill(Color.BLACK);
                }

                g.fillRect(x * tileSize, y * tileSize, tileSize, tileSize);
            }
        }

        // agente
        g.setFill(Color.YELLOW);
        g.fillOval(agent.x * tileSize - 5, agent.y * tileSize - 5, 10, 10);

        // direzione
        g.setStroke(Color.ORANGE);
        g.strokeLine(
            agent.x * tileSize,
            agent.y * tileSize,
            (agent.x + Math.cos(agent.angle)) * tileSize,
            (agent.y + Math.sin(agent.angle)) * tileSize
        );

        renderFOV(g, world, tileSize);
    }

    
public void renderFOV(GraphicsContext g, TileWorld world, double tileSize) {

    Agent agent = world.getAgent();

    float fov = (float)Math.toRadians(120); // stesso del lidar
    float radius = 5f; // visivo

    int segments = 20; // qualità

    g.setStroke(Color.color(0, 1, 0, 0.5));
    g.setLineWidth(1);

    double cx = agent.x * tileSize;
    double cy = agent.y * tileSize;

    for (int i = 0; i <= segments; i++) {

        float t = (i / (float)segments) - 0.5f;
        float angle = agent.angle + t * fov;

        float dx = (float)Math.cos(angle);
        float dy = (float)Math.sin(angle);

        double ex = (agent.x + dx * radius) * tileSize;
        double ey = (agent.y + dy * radius) * tileSize;

        g.strokeLine(cx, cy, ex, ey);
    }
}

}
