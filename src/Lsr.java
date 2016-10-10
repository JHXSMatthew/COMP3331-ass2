import com.sun.javafx.geom.Edge;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by matthew on 8/10/16.
 * The main router class.
 */
public class Lsr {

    public static final boolean DEBUG = true;

    public static void main(String[] args){
        Scanner scanner = new Scanner(args[2]);
        List<Neighbour> neighbours = Collections.synchronizedList(new ArrayList<Neighbour>());
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

    private int seq = 0;
    private DatagramSocket socket;
    private List<Neighbour> neighbours;
    private boolean updateRouter = true;
    private Timer timer;

    //the graph that stores all information related to current network topology
    private G_Graph graph;
    private ConcurrentHashMap<G_Node,G_Edge> forwardTable  = null;
    private ConcurrentHashMap<G_Node,LSPacket> packetCache = null;

    //some static constants , in ms
    private final static int UPDATE_INTERVAL = 1000;
    private final static int ROUTE_UPDATE_INTERVAL = 30000;
    private final static int TTL_KEEPALIVE = 200;
    private final static int TTL_LSP = 2000;

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
        forwardTable = new ConcurrentHashMap<G_Node,G_Edge>();
        packetCache = new ConcurrentHashMap<G_Node, LSPacket>();
        graph.add(id);
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(updateRouter){
                    updateRouter();
                }
            }
        },0,ROUTE_UPDATE_INTERVAL);

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                sendPacket();
                Iterator<Neighbour> neighbourIterator = neighbours.iterator();
                while(neighbourIterator.hasNext()){
                    Neighbour neighbour = neighbourIterator.next();
                    if(neighbour.isDead()){
                        neighbourIterator.remove();
                        System.err.println("Neighbour"+ neighbour.getId() + "dead, RIP");
                    }
                }
            }
        },0,UPDATE_INTERVAL);
    }

    private void sendPacket(){

    }

    /**
     *  Starting listening , will block the thread.
     *  won't stop until Ctrl+c
     */
    private void listen(){
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
            if(lsPacket.isExpired()){
                continue;
            }
            Neighbour neighbour = getNeighbourByPort(packet.getPort());
            if(lsPacket.isHeartbeat()){
                onHeartbeatsReceived(lsPacket,neighbour);
            }else{
                onLSPacketReceived(lsPacket,neighbour);
            }
        }

    }

    /**
     *  simulate the listener-Event coding pattern
     *  called when heartbeat packets received.
     * @param packet the heartbeat packet
     * @param sender the sender
     */
    private void onHeartbeatsReceived(LSPacket packet,Neighbour sender){
        //TODO: finish this after basic broadcast working
        //oh, I am dead
        sender.heartbeat();
    }

    /**
     * simulate the listener-Event coding pattern
     * called when LS packet received
     * @param packet the packet
     * @param sender the sender
     */
    private void onLSPacketReceived(LSPacket packet,Neighbour sender){
        G_Node advertiser = graph.getNode(packet.getAdvertisingRouter(),true);

        //update cache
        if(packetCache.containsKey(advertiser)) {
            LSPacket cachedPacket = packetCache.get(advertiser);
            if (cachedPacket.getSeq() < packet.getSeq()) {
                packetCache.put(advertiser, packet);
            } else {
                //old packet received, do nothing.
                return;
            }
        }

        //update graph database
        //fill edges and update costs if necessary
        List<G_Edge> allC_E = graph.getAllConnections(advertiser);
        for(G_Node node : packet.getConnectedNodes()){
            boolean edgeThere = false;
            for(G_Edge edge : allC_E){
                if(edge.isEdge(node,advertiser)){
                    edgeThere = true;
                    int ackCost = packet.getCost(node);
                    if(edge.getCost() != ackCost){
                        edge.setCost(ackCost);
                        invalid();
                    }
                }
            }
            if(!edgeThere) {
                graph.connect(node, advertiser, packet.getCost(node));
                invalid();
            }
        }
        //remove all disconnected nodes.
        List<String> pendingRemove = new ArrayList<String>();
        for(G_Edge edge : allC_E){
            if(!packet.getConnectedNodes().contains(edge.getTheOtherNode(advertiser)))
                pendingRemove.add(edge.getTheOtherNode(advertiser).getId());
        }
        for(String s : pendingRemove){
            graph.remove(s);
            invalid();
        }

        //then forward this packet to my neighbour as a gift, hope they love it :)
        try {
            forwardPacket(packet,sender);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *  invalid current Forward table ,
     *  it should be called when: cost changed | node fail etc...
     *  shortest paths need to be re-calculated.
     */
    public void invalid(){
        this.updateRouter = true;
    }


    /**
     *  forward LS packet to other neighbours.
     * @param packet the packet
     * @param duplicated the sender of this packet (one of my neighbour)
     * @throws IOException exception if there is something wrong with UDP stuff
     */
    public void forwardPacket(LSPacket packet,Neighbour duplicated) throws IOException {
        byte[] bytes = packet.toBytes();
        DatagramPacket send = new DatagramPacket(bytes,bytes.length);
        for(Neighbour neighbour : neighbours){
            if(neighbour.equals(duplicated))
                continue;
            send.setAddress(InetAddress.getLocalHost());
            send.setPort(neighbour.getPort());
            socket.send(send);
        }
    }


    /**
     *
     * @param port the neighbour port
     * @return the neighbour
     */
    public Neighbour getNeighbourByPort(int port){
        for(Neighbour neighbour : neighbours) {
            if(neighbour.getPort() == port)
                return neighbour;
        }
        System.err.println("CROSS ROUTER MESSAGE, HOW COULD IT BE POSSIBLE. YOU LIED TO ME!!!");
        return null;
    }

    public void updateRouter(){
        List<G_SearchingNode> searchingNodes = new ArrayList<G_SearchingNode>();
        for(G_Node node : graph.getAllNodes()){
            if(node.getId().equals(id)){
                continue;
            }

            try {
                G_SearchingNode searchingNode =  getShortestPath(graph.getNode(id,false),node);
                searchingNodes.add(searchingNode);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //remove the old forward "table"
        forwardTable.clear();
        for(G_SearchingNode searchingNode : searchingNodes){
            System.out.println(searchingNode.getSearchingString());
            forwardTable.put(searchingNode.getNode(),graph.getEdge(graph.getNode(id,false),searchingNode.getDirectNode()));
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
