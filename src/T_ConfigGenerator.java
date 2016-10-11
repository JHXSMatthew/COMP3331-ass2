import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * Created by Matthew on 11/10/2016.
 * a config generator. useful for testing.
 * it will generate test files in data/%testID%
 */
public class T_ConfigGenerator {


    /**
     * argv[0] = node amount per test
     * argv[1] = test amount
     * @param argv
     */
    public static void main(String argv[]){
        int nodeCount = Integer.parseInt(argv[0]);
        int repeat = 1;
        if(argv.length > 1){
            repeat = Integer.parseInt(argv[1]);
        }
        new T_ConfigGenerator(nodeCount,repeat);
    }

    private Random r;

    private static char STARTING_CHAR = 'A';
    private static int MAX_COST = 100;
    private static int BASE_PORT = 2000;
    private static float MAX_CHANCE = 0.5f;
    private G_Graph graph ;


    public T_ConfigGenerator(int nodeCount, int repeat){
        r = new Random();
        graph = new G_Graph();
        System.out.println("Node Count=" + nodeCount);

        for(int i = 0 ; i < nodeCount ; i ++){
            graph.add(Character.toString((char)(STARTING_CHAR + i)));
        }

        for(int i = 0 ; i < repeat ; i ++){
            generate(i);
        }
    }


    private void generate(int i){
        List<G_Node> nodes = graph.getAllNodes();
        for(G_Node from : nodes){
            for(G_Node to : nodes){
                if(from == to){
                    continue;
                }
                if(r.nextFloat() < MAX_CHANCE/(Math.abs(from.getId().charAt(0) - to.getId().charAt(0)))){
                    graph.connect(from,to,(((int)(r.nextFloat()*10))/10F + r.nextInt(MAX_COST)) + 1);
                }
            }
        }

        for(G_Node from : nodes){
            for(G_Node to : nodes){
                try {
                    getShortestPath(from, to);
                }catch (Exception e){
                    graph.connect(from,to,r.nextFloat() + r.nextInt(MAX_COST) + 1);
                    System.err.print("direct connect nodes to prevent partial graph");
                }
            }
        }

        try {
            for(G_Node node : nodes){
                File f = new File("data" + File.separator  + i );
                f.mkdirs();
                f = new File(f, "config" + node.getId() + ".txt");
                f.createNewFile();


                BufferedWriter writer = new BufferedWriter(new FileWriter(f));
                writer.write(String.valueOf(nodes.size()));
                writer.write(System.lineSeparator());

                List<G_Edge> connected = graph.getAllConnections(node);
                for(G_Edge edge : connected){
                    writer.write(edge.getTheOtherNode(node).getId() + " " + edge.getCost() + " " + (edge.getTheOtherNode(node).getId().charAt(0) - STARTING_CHAR + BASE_PORT) );
                    writer.write(System.lineSeparator());
                }
                writer.close();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * the dijkstra algorithm for shortest path.
     * using min-heap (priority queue) optimization.
     *
     * @param starting the starting node
     * @param end      the destination node
     * @return the final searching state.
     */
    public G_SearchingNode getShortestPath(G_Node starting, G_Node end) throws Exception {
        PriorityQueue<G_SearchingNode> queue = new PriorityQueue<G_SearchingNode>();
        G_SearchingNode node = new G_SearchingNode(starting, null, 0);
        queue.add(node);
        while (!queue.isEmpty()) {
            G_SearchingNode current = queue.poll();
            if (current.getNode() == end) {
                return current;
            }
            List<G_Edge> connections = graph.getAllConnections(current.getNode());
            for (G_Edge edge : connections) {
                if (!current.hasBeen(edge.getTheOtherNode(current.getNode()))) {
                    G_SearchingNode next = new G_SearchingNode(
                            edge.getTheOtherNode(current.getNode()),
                            current,
                            edge.getCost());
                    queue.add(next);
                }
            }

        }
        throw new Exception("May have no connections! from " + starting.getId() + " to " + end.getId());

    }


}
