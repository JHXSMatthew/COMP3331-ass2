import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.*;

/**
 * Created by matthew on 8/10/16.
 * The main router class.
 */
public class Lsr {

    public static void main(String[] args){
        Scanner scanner = new Scanner(args[2]);
        List<Neighbour> neighbours = new ArrayList<Neighbour>();
        int num = -2;
        int curr = 0;
        while(scanner.hasNextLine()){
            String readLine = scanner.nextLine();
            if(num == -2){
                num = Integer.parseInt(readLine);
            }else{
                curr ++;
                StringTokenizer tokenizer = new StringTokenizer(readLine," ");
                neighbours.add(new Neighbour(tokenizer.nextToken()
                        , Integer.parseInt(tokenizer.nextToken())
                        ,Integer.parseInt(tokenizer.nextToken())));
                if(curr > num)
                    break;
            }
        }

        Lsr r = new Lsr(args[0],Integer.parseInt(args[1]),neighbours);

    }


    private final String id;
    private final int port;

    private DatagramSocket socket;
    private List<Neighbour> neighbours;

    //the graph that stores all information related to current network topology
    private G_Graph graph;
    private HashMap<G_Node,G_Edge> forwardTable  = null;

    //some static constants , in ms
    private final static int UPDATE_INTERVAL = 1000;
    private final static int ROUTE_UPDATE_INTERVAL = 30000;

    /**
     *
     * @param id the current router id
     * @param port the port current router listen on
     * @param neighbourList the neighbours list
     */
    public Lsr(String id, int port, List<Neighbour> neighbourList){
        this.neighbours = neighbourList;
        this.id = id;
        this.port = port;
        graph = new G_Graph();
        forwardTable = new HashMap<G_Node,G_Edge>();
        graph.add(id);

    }

    /**
     *  Starting listening , will block the thread.
     *  won't stop until Ctrl+c
     */
    private void  listen(){
        if(socket == null){
            try {
                socket = new DatagramSocket(port);
            } catch (SocketException e) {
                System.err.println("Listen Socket Exception!");
                e.printStackTrace();
            }
        }

        while(true){
            DatagramPacket packet = new DatagramPacket(new byte[1024],1024);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                System.out.println("packet receive exception!");
                e.printStackTrace();
            }
            LSPacket lsPacket = new LSPacket(packet.getData());
            if(lsPacket.isHeartbeat()){
                onHeartbeats(lsPacket);
            }else{
                onLSPacketReceived(lsPacket);
            }
        }

    }

    private void onHeartbeats(LSPacket packet){

    }

    private void onLSPacketReceived(LSPacket packet){

    }


    public void forwardPacket(LSPacket packet) throws IOException {
        for(Neighbour neighbour : neighbours){
            byte[] bytes = packet.toBytes();
            DatagramPacket send = new DatagramPacket(bytes,bytes.length);
            socket.send(send);
        }
    }


    public void update(){
        List<G_SearchingNode> searchingNodes = new ArrayList<G_SearchingNode>();
        for(G_Node node : graph.getAllNodes()){
            if(node.getId().equals(id){
                continue;
            }

            try {
                G_SearchingNode searchingNode =  getShortestPath(graph.getNode(id),node);
                searchingNodes.add(searchingNode);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //remove the old forward "table"
        forwardTable.clear();
        for(G_SearchingNode searchingNode : searchingNodes){
            System.out.println(searchingNode.getSearchingString());
            forwardTable.put(searchingNode.getNode(),graph.getEdge(graph.getNode(id),searchingNode.getDirectNode()));
        }
        
    }

    /**
     *
     *  the dijkstra algorithm for shortest path.
     *  using min-heap (priority queue) optimization.
     * @param starting the starting node
     * @param end the destination node
     * @return the final searching state.
     */
    public G_SearchingNode getShortestPath(G_Node starting, G_Node end) throws Exception {
        PriorityQueue<G_SearchingNode> queue = new PriorityQueue();
        G_SearchingNode node = new G_SearchingNode(starting,null,0);
        queue.add(node);
        while(!queue.isEmpty()){
            G_SearchingNode current = queue.poll();
            if(current.getNode() == end){
                return current;
            }
            List<G_Edge> connections = graph.getAllConnections(current.getNode());
            for(G_Edge edge : connections){
                G_SearchingNode next = new G_SearchingNode(
                        edge.getTheOtherNode(current.getNode()),
                        current,
                        edge.getCost());
                queue.add(next);
            }

        }
        throw new Exception("May have no connections!");

    }


}
