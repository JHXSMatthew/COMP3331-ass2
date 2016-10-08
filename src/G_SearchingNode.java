/**
 * Created by matthew on 8/10/16.
 * the Searching node, use for searching only.
 * it represents the current searching state in OOP.
 */
public class G_SearchingNode {

    private G_SearchingNode last;
    private int cost; // the cumulative cost.
    private G_Node node; // the reference of original node.

    /**
     *
     * @param previous the previous node
     * @param cost the cost to get here from previous node
     */
    public G_SearchingNode(G_Node node, G_SearchingNode previous, int cost){
        this.last = previous;
        this.cost = previous.cost + cost;
        this.node = node;
    }


    public void getPath(StringBuilder builder){
        if(last != null){
            last.getPath(builder);
        }
        builder.append(node.getId());
    }

    public String getSearchingString(){
        StringBuilder builder = new StringBuilder();
        getPath(builder);
        return "least-cost path to node " + node.getId() + ": " + builder.toString() + " and the cost is " + cost;
    }
}
