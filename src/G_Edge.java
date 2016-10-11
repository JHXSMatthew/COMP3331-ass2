/**
 * Created by matthew on 8/10/16.
 * The edge in this case, actually , it should be called "link" anyway.
 */
public class G_Edge {

    private G_Node[] connected = new G_Node[2];
    private float cost;

    public G_Edge(G_Node nodeA, G_Node nodeB, float cost) {
        connected[0] = nodeA;
        connected[1] = nodeB;
        this.cost = cost;
    }

    /**
     * @return the array holding, original.
     */
    public G_Node[] getNodes() {
        return connected;
    }

    public G_Node getTheOtherNode(G_Node node) {
        if (connected[0] == node) {
            return connected[1];
        }
        return connected[0];
    }

    public float getCost() {
        return cost;
    }

    public void setCost(float c) {
        this.cost = c;
    }

    public boolean isEdge(G_Node... node) {
        if (node.length > 2) {
            return false;
        }
        for (G_Node temp : node) {
            boolean isIn = false;
            for (G_Node in : connected)
                if (in == temp)
                    isIn = true;

            if (!isIn)
                return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof G_Edge) {
            return ((G_Edge) obj).isEdge(connected) && cost == ((G_Edge) obj).cost;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "[" + getNodes()[0].getId() + "<->" + getNodes()[1].getId() + "] ";
    }
}
