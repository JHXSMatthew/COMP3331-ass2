import java.util.Objects;

/**
 * Created by matthew on 8/10/16.
 *
 * The representation of neighbour
 * Store all useful information of a neighbour
 *
 */
public class Neighbour {

    private final String id;
    private final int cost;
    private final int port;

    private long lastAlive = 0;
    private byte cumulativeMiss = 0;

    public Neighbour(String id, int cost, int port){
        this.id = id;
        this.cost = cost;
        this.port = port;
    }

    public void heartbeat(){
        lastAlive = System.currentTimeMillis();
        cumulativeMiss = 0;
    }

    public boolean isDead(){
        return cumulativeMiss > 3;
    }

    public String getId() {
        return id;
    }

    public int getCost() {
        return cost;
    }

    public int getPort() {
        return port;
    }

    @Override
    public boolean equals(Object obj){
        if(obj instanceof Neighbour){
            return id.equals(((Neighbour) obj).getId())
                    && port == ((Neighbour) obj).getPort()
                    && cost == ((Neighbour) obj).getCost();  //TODO:should check the cost ?
        }
        return false;
    }
}
