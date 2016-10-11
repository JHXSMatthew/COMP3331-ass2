/**
 * Created by matthew on 8/10/16.
 * the Searching node, use for searching only.
 * it represents the current searching state in OOP.
 */
public class G_SearchingNode implements Comparable<G_SearchingNode> {

    private G_SearchingNode last;
    private float cost; // the cumulative cost.
    private G_Node node; // the reference of original node.

    /**
     * @param previous the previous node
     * @param cost     the cost to get here from previous node
     */
    public G_SearchingNode(G_Node node, G_SearchingNode previous, float cost) {
        this.last = previous;
        if (previous != null)
            this.cost = previous.cost + cost;

        this.node = node;
    }


    public void getPath(StringBuilder builder) {
        if (last != null) {
            last.getPath(builder);
        }
        builder.append(node.getId());
    }

    public String getSearchingString() {
        StringBuilder builder = new StringBuilder();
        getPath(builder);
        return "least-cost path to node " + node.getId() + ": " + builder.toString() + " and the cost is " + cost;
    }

    public G_Node getNode() {
        return node;
    }

    public G_Node getDirectNode() {
        if (last != null) {
            if (last.last == null) {
                return last.node;
            }
        } else {
            System.err.println("What is this fking error ?, how could that be possible. I cannot accept this!");
            return node;
        }
        return last.getDirectNode();
    }

    public boolean hasBeen(G_Node node) {
        if (last == null)
            return this.node.equals(node);
        return last.hasBeen(node) || this.node.equals(node);
    }

    @Override
    public int compareTo(G_SearchingNode o) {
        return (int)(cost*10) - (int)(o.cost *10);
    }
}
