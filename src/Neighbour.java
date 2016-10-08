/**
 * Created by matthew on 8/10/16.
 *
 * The representation of neighbour
 * Store all useful information of a neighbour
 *
 */
public class Neighbour {

    public final String id;
    public final int cost;
    public final int port;

    public Neighbour(String id, int cost, int port){
        this.id = id;
        this.cost = cost;
        this.port = port;
    }
}
