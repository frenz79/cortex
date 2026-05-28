
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
    }
}
