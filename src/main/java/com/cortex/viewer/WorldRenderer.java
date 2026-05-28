
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


    
public void drawLidar(GraphicsContext g, TileWorld world, float[] distances, double tileSize) {

    Agent agent = world.getAgent();

    int rays = distances.length;
    float fov = (float)Math.toRadians(120);

    double cx = agent.x * tileSize;
    double cy = agent.y * tileSize;

    g.setStroke(Color.LIMEGREEN);

    for (int i = 0; i < rays; i++) {

        float t = (i / (float)(rays - 1)) - 0.5f;
        float rayAngle = agent.angle + t * fov;

        float dist = distances[i];

        double ex = (agent.x + Math.cos(rayAngle) * dist) * tileSize;
        double ey = (agent.y + Math.sin(rayAngle) * dist) * tileSize;

        g.strokeLine(cx, cy, ex, ey);
    }
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


    
public void fillFOV(GraphicsContext g, TileWorld world, float[] distances, double tileSize) {

    Agent agent = world.getAgent();
    int rays = distances.length;
    float fov = (float)Math.toRadians(120);

    double cx = agent.x * tileSize;
    double cy = agent.y * tileSize;

    double[] xs = new double[rays + 2];
    double[] ys = new double[rays + 2];

    xs[0] = cx;
    ys[0] = cy;

    for (int i = 0; i < rays; i++) {

        float t = (i / (float)(rays - 1)) - 0.5f;
        float angle = agent.angle + t * fov;

        float dist = distances[i];

        xs[i + 1] = (agent.x + Math.cos(angle) * dist) * tileSize;
        ys[i + 1] = (agent.y + Math.sin(angle) * dist) * tileSize;
    }

    xs[rays + 1] = cx;
    ys[rays + 1] = cy;

    g.setFill(Color.color(0, 1, 0, 0.15));
    g.fillPolygon(xs, ys, rays + 2);
}


}
