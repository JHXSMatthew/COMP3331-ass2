import java.util.*;

/**
 * Created by matthew on 8/10/16.
 * The undirected graph for this assignment.
 */
public class G_Graph {

    private final List<G_Node> nodes;
    private Set<G_Edge> edges;

    public G_Graph() {
        nodes = Collections.synchronizedList(new ArrayList<G_Node>());
        edges = Collections.synchronizedSet(new HashSet<G_Edge>());
    }

    /**
     * @param nodeA one node
     * @param nodeB the other node
     * @param cost  the cost to reach each other
     * @return the connected Nodes
     */
    public G_Edge connect(G_Node nodeA, G_Node nodeB, float cost) {
        G_Edge edge = new G_Edge(nodeA, nodeB, cost);

        if (isEdgeThere(edge))
            return null;

        edges.add(edge);
        return edge;
    }

    public List<G_Node> getAllNodes() {
        synchronized (this.nodes) {
            List<G_Node> copy = new ArrayList<G_Node>(); //copy to a new array prevent the concurrentModification exception.
            copy.addAll(this.nodes);
            return copy;
        }
    }

    /**
     * @param edge the edge to check
     * @return true if edge in the graph
     */
    public boolean isEdgeThere(G_Edge edge) {
        for (G_Edge e : edges) {
            if (e.isEdge(edge.getNodes())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param id the node to add
     * @return the G_Node object
     */
    public G_Node add(String id) {
        G_Node node = new G_Node(id);
        this.nodes.add(node);
        return node;
    }

    /**
     * @param node1 one node
     * @param node2 the other
     * @return the edge
     */
    public G_Edge getEdge(G_Node node1, G_Node node2) {
        List<G_Edge> edges = getAllConnections(node1);
        for (G_Edge edge : edges) {
            if (edge.isEdge(node2)) {
                return edge;
            }
        }
        return null;
    }

    /**
     * @param id the id of removal
     * @return the removal node
     */
    public synchronized G_Node remove(String id) {
        Iterator<G_Node> iterator = nodes.iterator();
        G_Node node = null;
        while (iterator.hasNext()) {
            G_Node n = iterator.next();
            if (n.getId().equals(id)) {
                node = n;
                iterator.remove();
                break;
            }
        }
        if (node == null) {
            System.err.println("Node not exist!");
            return null;
        }
        boolean print = false;
        List<G_Edge> allConnected = getAllConnections(node);
        if (allConnected != null && !allConnected.isEmpty()) {
            for (G_Edge edge : allConnected) {
                print = true;
                edges.remove(edge);
            }
        }
        if (print) {
            System.err.println("Neighbour " + id + " is Down");
        }
        return node;
    }


    public List<G_Edge> getAllConnections(G_Node node) {
        List<G_Edge> returnValue = new ArrayList<G_Edge>();
        for (G_Edge edge : edges) {
            if (edge.isEdge(node)) {
                returnValue.add(edge);
            }
        }
        return returnValue;
    }

    public List<G_Edge> getAllConnections(String id) {
        G_Node node = getNode(id, false);
        return getAllConnections(node);
    }

    public G_Node getNode(String id, boolean createNew) {
        for (G_Node node : this.nodes) {
            if (node.getId().equals(id)) {
                return node;
            }
        }

        return createNew ? add(id) : null;
    }

    public void print() {
        if (!Lsr.DEBUG) {
            return;
        }
        System.err.println("Graph detail");
        System.err.println("Nodes:" + nodes.size());
        System.err.print("    ");
        for (G_Node n : nodes) {
            System.err.print(n.toString());
        }
        System.err.println();
        System.err.println("Edges: " + edges.size());
        System.err.print("    ");
        for (G_Edge n : edges) {
            System.err.print(n.toString());
        }
        System.err.println();
    }


}
