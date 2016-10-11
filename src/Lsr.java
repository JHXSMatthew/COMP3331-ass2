import java.io.File;
import java.io.FileNotFoundException;
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

    public static final boolean DEBUG = false;
    //some static constants , in ms
    private final static int KEEP_ALIVE_INTERVAL = 25;
    private final static int UPDATE_INTERVAL = 1000;
    private final static int ROUTE_UPDATE_INTERVAL = 30000;
    /* private final static int TTL_KEEP_ALIVE = 200;
     private final static int TTL_LSP = 2000;
     */
    private final String id;
    private final int port;
    private int seq = 0;
    private DatagramSocket socket;
    private final List<Neighbour> neighbours;
    private boolean updateRouter = true;
    private boolean listen = false;
    private Timer timer;
    //the graph that stores all information related to current network topology
    private G_Graph graph;
    private ConcurrentHashMap<G_Node, G_Edge> forwardTable = null;
    private ConcurrentHashMap<G_Node, LSPacket> packetCache = null;

    /**
     * @param id            the current router id
     * @param port          the port current router listen on
     * @param neighbourList the neighbours list
     */
    public Lsr(String id, int port, final List<Neighbour> neighbourList) {
        this.neighbours = neighbourList;
        this.id = id;
        this.port = port;
        graph = new G_Graph();
        forwardTable = new ConcurrentHashMap<G_Node, G_Edge>();
        packetCache = new ConcurrentHashMap<G_Node, LSPacket>();

        for (Neighbour neighbour : neighbourList) {
            graph.connect(graph.getNode(neighbour.getId(), true), graph.getNode(id, true), neighbour.getCost());
        }
        //graph.print();

        timer = new Timer();

        timer.schedule(new TimerTask() {
            int count = 1;

            @Override
            public void run() {
                if (!listen) {
                    return;
                } else {
                    count++;
                }
                //keep alive stuff
                if (count % KEEP_ALIVE_INTERVAL == 0) {
                    try {
                        sendPacket(true);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                // node failed test , set it to, max delay of detecting node failure is 3x100ms = 300ms
                if (count % 100 == 0) {
                    synchronized (neighbours) {
                        for (Neighbour neighbour : neighbours) {
                            neighbour.isNeighbourAlive();
                        }
                    }
                }
                // per second LS packet sending
                if (count % UPDATE_INTERVAL == 0) {
                    graph.print();

                    try {
                        sendPacket(false);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    synchronized (neighbours) {
                        Iterator<Neighbour> neighbourIterator = neighbours.iterator();
                        while (neighbourIterator.hasNext()) {
                            Neighbour neighbour = neighbourIterator.next();
                            if (neighbour.isDead()) {
                                neighbourIterator.remove();
                                graph.remove(neighbour.getId());
                            }
                        }
                    }
                    if (neighbours.isEmpty()) {
                        System.err.println("Why I am still alive, all my neighbours died. I choose to die");
                        if (DEBUG) {
                            System.exit(-1);
                        }
                    }
                }

                //shortest oath
                if (count == ROUTE_UPDATE_INTERVAL) {
                    if (updateRouter) {
                        updateRouter();
                    }
                    count = 0;
                }
            }
        }, 0, 1);

        System.out.println("Router: " + id + " Listen on: " + port);
        listen();
    }

    public static void main(String[] args) throws FileNotFoundException {
        Scanner scanner = new Scanner(new File(args[2]));
        List<Neighbour> neighbours = Collections.synchronizedList(new ArrayList<Neighbour>());
        int num = -2;
        int curr = 0;
        while (scanner.hasNextLine()) {
            String readLine = scanner.nextLine();
            if (num == -2) {
                num = Integer.parseInt(readLine);
            } else {
                curr++;
                StringTokenizer tokenizer = new StringTokenizer(readLine, " ");
                neighbours.add(new Neighbour(tokenizer.nextToken()
                        , Float.parseFloat(tokenizer.nextToken())
                        , Integer.parseInt(tokenizer.nextToken())));
                if (curr > num)
                    break;
            }
        }

        new Lsr(args[0], Integer.parseInt(args[1]), neighbours);

    }


    /**
     * @param heartbeats if it is a heartbeats packet
     * @throws IOException
     */
    private void sendPacket(boolean heartbeats) throws IOException {
        LSPacket lsPacket ;
        if (heartbeats) {
            lsPacket = new LSPacket(id
                    , true
                    , seq
                    , (G_Edge) null);
        } else {
            List<G_Edge> temp = graph.getAllConnections(id);
            lsPacket = new LSPacket(id
                    , false
                    , seq++
                    , temp.toArray(new G_Edge[temp.size()]));
        }
        forwardPacket(lsPacket, (Neighbour) null);
    }


    /**
     * Starting listening , will block the thread.
     * won't stop until Ctrl+c
     */
    private void listen() {
        if (socket == null) {
            try {
                socket = new DatagramSocket(port);
            } catch (SocketException e) {
                System.err.println("Listen Socket Exception!");
                e.printStackTrace();
            }
        }
        listen = true;

        while (true) {
            DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                System.out.println("packet receive exception!");
                e.printStackTrace();
                System.exit(-1);
            }
            LSPacket lsPacket = new LSPacket(packet.getData());

            Neighbour neighbour = getNeighbourByPort(packet.getPort());
            if (lsPacket.isHeartbeat()) {
                onHeartbeatsReceived(lsPacket, neighbour);
            } else {
                onLSPacketReceived(lsPacket, neighbour);
            }
        }

    }

    /**
     * simulate the listener-Event coding pattern
     * called when heartbeat packets received.
     *
     * @param packet the heartbeat packet
     * @param sender the sender
     */
    private void onHeartbeatsReceived(LSPacket packet, Neighbour sender) {
        if (sender == null) {
            return;
        }
        sender.heartbeat();
    }

    /**
     * simulate the listener-Event coding pattern
     * called when LS packet received
     *
     * @param packet the packet
     * @param sender the sender
     */
    private void onLSPacketReceived(LSPacket packet, Neighbour sender) {
        G_Node advertiser = graph.getNode(packet.getAdvertisingRouter(), true);

        //update cache
        if (packetCache.containsKey(advertiser)) {
            LSPacket cachedPacket = packetCache.get(advertiser);
            if (cachedPacket.getSeq() < packet.getSeq()) {
                packetCache.put(advertiser, packet);
            } else {
                //old packet received, do nothing.
                return;
            }
        } else {
            packetCache.put(advertiser, packet);
        }

        //time to unpack
        packet.unpack(graph);

        //update graph database
        //fill edges and update costs if necessary
        List<G_Edge> allC_E = graph.getAllConnections(advertiser);
        for (G_Node node : packet.getConnectedNodes()) {
            boolean edgeThere = false;
            for (G_Edge edge : allC_E) {
                if (edge.isEdge(node, advertiser)) {
                    edgeThere = true;
                    float ackCost = packet.getCost(node);
                    if (edge.getCost() != ackCost) {
                        edge.setCost(ackCost);
                        invalid();
                    }
                }
            }
            if (!edgeThere) {
                graph.connect(node, advertiser, packet.getCost(node));
                invalid();
            }
        }
        //remove all disconnected nodes.
        List<String> pendingRemove = new ArrayList<String>();
        for (G_Edge edge : allC_E) {
            if (!packet.getConnectedNodes().contains(edge.getTheOtherNode(advertiser))) {
                pendingRemove.add(edge.getTheOtherNode(advertiser).getId());
            }
        }
        for (String s : pendingRemove) {
            graph.remove(s);
            invalid();
        }

        //then forward this packet to my neighbour as a gift, hope they love it :)

        try {
            Neighbour advertisingNeighbour = getNeighbourById(advertiser.getId());
            if (advertisingNeighbour == null) {
                forwardPacket(packet, sender);
            } else {
                forwardPacket(packet, sender, advertisingNeighbour);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * invalid current Forward table ,
     * it should be called when: cost changed | node fail etc...
     * shortest paths need to be re-calculated.
     */
    public void invalid() {
        this.updateRouter = true;
    }


    /**
     * forward LS packet to other neighbours.
     *
     * @param packet     the packet
     * @param duplicated the sender of this packet (one of my neighbour)
     * @throws IOException exception if there is something wrong with UDP stuff
     */
    public void forwardPacket(LSPacket packet, Neighbour... duplicated) throws IOException {
        byte[] bytes = packet.toBytes();
        DatagramPacket send = new DatagramPacket(bytes, bytes.length);
        synchronized (neighbours) {
            for (Neighbour neighbour : neighbours) {
                if (duplicated != null) {
                    boolean skip = false;
                    for (Neighbour temp : duplicated) {
                        if (temp == null) {
                            continue;
                        }
                        if (temp.equals(neighbour)) {
                            skip = true;
                        }
                    }
                    if (skip)
                        continue;
                }
                if (DEBUG && !packet.isHeartbeat()) {
                    StringBuilder builder = new StringBuilder();
                    builder.append("sending packets to ")
                            .append(neighbour.getId())
                            .append(" ")
                            .append(+neighbour.getPort())
                            .append(" heartbeat= ")
                            .append(packet.isHeartbeat())
                            .append(" seq= ").append(packet.getSeq());

                    if (!packet.getAdvertisingRouter().equals(id))
                        builder.append(" forward from ").append(packet.getPrintableName());

                    System.err.println(builder.toString());
                }
                send.setAddress(InetAddress.getByName("127.0.0.1"));
                send.setPort(neighbour.getPort());
                socket.send(send);
            }
        }
    }


    /**
     * update router, run shortest paths
     */
    public void updateRouter() {
        List<G_SearchingNode> searchingNodes = new ArrayList<G_SearchingNode>();
        //graph.print();
        List<G_Node> allNodes = graph.getAllNodes();
        for (G_Node node : allNodes) {
            if (node.getId().equals(id)) {
                continue;
            }
            try {
                G_SearchingNode searchingNode = getShortestPath(graph.getNode(id, false), node);
                searchingNodes.add(searchingNode);
            } catch (Exception e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
                //TODO: check if this is necessary, but it's obvious truth, node cannot reach should be removed in order
                //to reduce the cost of searching.
                graph.remove(node.getId());
            }
        }

        //remove the old forward "table"
        forwardTable.clear();
        System.out.println("Router update ...");
        for (G_SearchingNode searchingNode : searchingNodes) {
            System.out.println(searchingNode.getSearchingString());
            forwardTable.put(searchingNode.getNode(), graph.getEdge(graph.getNode(id, false), searchingNode.getDirectNode()));
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


    /**
     * @param port the neighbour port
     * @return the neighbour
     */
    public Neighbour getNeighbourByPort(int port) {
        synchronized (neighbours) {
            for (Neighbour neighbour : neighbours) {
                if (neighbour.getPort() == port)
                    return neighbour;
            }
        }
        System.err.println("ERROR, possible reason , offline router goes to online again, or configs error!");
        return null;
    }


    /**
     * @param id the id of neighbour trying to get
     * @return the neighbour object
     */
    public Neighbour getNeighbourById(String id) {
        synchronized (neighbours) {
            for (Neighbour neighbour : neighbours) {
                if (neighbour.getId().equals(id))
                    return neighbour;
            }
        }
        return null;
    }


}
