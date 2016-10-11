/**
 * Created by matthew on 8/10/16.
 * <p>
 * The representation of neighbour
 * Store all useful information of a neighbour
 */
public class Neighbour {

    private final String id;
    private final float cost;
    private final int port;

    private boolean beat = false;
    private byte cumulativeMiss = 0;

    public Neighbour(String id, float cost, int port) {
        this.id = id;
        this.cost = cost;
        this.port = port;
    }

    public void heartbeat() {
        beat = true;
        cumulativeMiss = 0;
    }

    public boolean isDead() {
        return cumulativeMiss > 3;
    }

    /**
     * @return true if received heart beats in the interval, false if not
     */
    public boolean isNeighbourAlive() {
        boolean returnValue = beat;
        if (!beat) {
            cumulativeMiss++;
        }
        beat = false;
        return returnValue;
    }

    public String getId() {
        return id;
    }

    public float getCost() {
        return cost;
    }

    public int getPort() {
        return port;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Neighbour) {
            return id.equals(((Neighbour) obj).getId())
                    && port == ((Neighbour) obj).getPort()
                    && cost == ((Neighbour) obj).getCost();  //TODO:should check the cost ?
        }
        return false;
    }
}
