
import java.util.*;

/**
 * Created by matthew on 8/10/16.
 * The undirected graph for this assignment.
 */
public class G_Graph {

    private List<G_Node> nodes;
    private Set<G_Edge> edges;


    public G_Graph(){
        nodes = new ArrayList<>();
        edges = new HashSet<>();
    }

    public G_Edge connect(G_Node nodeA, G_Node nodeB, int cost){
        G_Edge edge = new G_Edge(nodeA,nodeB,cost);

        if(isEdgeThere(edge))
            return null;

        edges.add(edge);
        return edge;

    }

    public boolean isEdgeThere(G_Edge edge){
        for(G_Edge e : edges){
            if(e.isEdge(edge.getNodes())){
                return true;
            }
        }
        return false;
    }

    public G_Node add(String id) {
        G_Node node = new G_Node(id);
        this.nodes.add(node);
        return node;
    }

    public G_Node remove(String id){
        Iterator<G_Node> iterator = nodes.iterator();
        G_Node node = null;
        while(iterator.hasNext()){
            G_Node n = iterator.next();
            if(n.getId().equals(id)){
                node = n;
                break;
            }
        }
        if(node == null){
            System.err.println("Node not exist!");
            return null;
        }
        List<G_Edge> allConnected = getAllConnections(node);
        if(allConnected != null && !allConnected.isEmpty()) {
            for(G_Edge edge : allConnected){
                edges.remove(edge);
            }
        }
        return node;
    }


    public List<G_Edge> getAllConnections(G_Node node){
        List<G_Edge> returnValue = new ArrayList<G_Edge>();
        for(G_Edge edge : edges){
            if(edge.isEdge(node)){
                returnValue.add(edge);
            }
        }
        return returnValue;
    }

    public List<G_Edge> getAllConnections(String id){
        G_Node node = getNode(id);
        return getAllConnections(node);
    }

    public G_Node getNode(String id){
        for(G_Node node : this.nodes){
            if(node.getId().equals(id)){
                return node;
            }
        }
        return null;
    }


}