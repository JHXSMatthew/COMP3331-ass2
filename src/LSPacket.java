import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Set;

/**
 * Created by matthew on 8/10/16.
 * The link-state packet.
 * It's very nice to not deal with packet lost :).
 */
public class LSPacket {


    /*
                          ------------------ Packet header ------------------ 26 bytes
                  0    byte  --- String ---  advertising router id  ---
                  1    byte  ---  |R|R|R|R|R|R|R|k
                       K : keep alive, true if it is a heartbeat packet
                       R : Reserved
                  2-5 bytes --- int    ---  Seq number , that's the "version" of this packet.
                  6-9 bytes --- int    ---  the actual data segment length, HEADER EXCLUDED
                          ------------------ data seg ------------------
                  {
                    10    byte  --- String  ---  id of Neighbour
                    11-14 bytes --- float     ---  the cost from advertising router to the router above
                  }*  5 bytes in total
                  ( n segments of this type of data, where n <= 0) , data seg maybe empty if it is a heartbeat packet

     */


    //static constants
    private final static int HEADER_LENGTH = 10;
    //attributes of this packet, included in packet header
    private String advertisingRouter;
    private boolean[] flags = new boolean[8];
    //private long age = 0;
    private int seq = -1;
    //data segment
    private HashMap<G_Node, Float> connections = new HashMap<G_Node, Float>();
    private byte[] data = null;
    // dynamic wrapper stuff
    //private boolean expired = true;


    /**
     * construct basic packet information.
     * not fully un-packed the data seg as long as the (method unpack) in called.
     *
     * @param packet the receiving data
     */
    public LSPacket(byte[] packet) {
        /*age = (long) PacketUtils.get4BytesInt(packet, 0) << 32 * 3
                | PacketUtils.get4BytesInt(packet, 4) << 32 * 2
                | PacketUtils.get4BytesInt(packet, 8) << 32
                | PacketUtils.get4BytesInt(packet, 12);*/
        //expired = System.currentTimeMillis() <= age;

        advertisingRouter = Character.toString((char) packet[0]);
        //bits to booleans, load flags
        for (int i = 0; i < 8; i++)
            flags[i] = (packet[1] & (0b00000001 << i)) != 0;

        seq = PacketUtils.get4BytesInt(packet, 2);
        int length = PacketUtils.get4BytesInt(packet, 6);
        if (!isHeartbeat()) {
            data = new byte[length];
            System.arraycopy(packet,HEADER_LENGTH,data,0,data.length);
            /*for (int i = 0; i < data.length; i++) {
                data[i] = packet[i + HEADER_LENGTH];
            }
            */
        }

    }

    public LSPacket(String advertisingRouter, boolean isHeartbeat, int seq, G_Edge... connections) {
        this.seq = seq;
        //this.age = System.currentTimeMillis() + ages;
        this.flags[0] = isHeartbeat;
        this.advertisingRouter = advertisingRouter;
        if (!isHeartbeat) {
            for (G_Edge edge : connections) {
                addConnectionData(edge);
            }
        }
    }

    /**
     * to unpack the packet
     *
     * precondition !expired, !empty , seq may > current cache seq
     */
    public void unpack(G_Graph graph) {
        if (data.length % 5 != 0) {
            System.err.println("packet error ? DL=" + data.length + " from " + getAdvertisingRouter());
        }
        for (int i = 0; i < data.length; i += 5) {
            String id = Character.toString((char) data[i]);
            connections.put(graph.getNode(id, true), PacketUtils.get4ByteFloat(data, i + 1));
        }

    }

    /**
     * packet the readable G_Node hash map to bytes array
     * should be called before toBytes but I did check there anyway.
     * direct buffer was used, hope no memory leak anyway.
     */
    public void pack() {
        ByteBuffer buffer = ByteBuffer.allocate(connections.size() * 5);
        for (G_Node node : connections.keySet()) {
            buffer.put((byte) node.getId().charAt(0));
            PacketUtils.fill4BytesFloatToBuffer(connections.get(node), buffer);
        }
        buffer.flip();
        data = buffer.array();
        buffer.clear();
    }


    /**
     * @return the advertising router
     */
    public String getAdvertisingRouter() {
        return advertisingRouter;
    }

    /**
     * @return is heartbeat packet
     */
    public boolean isHeartbeat() {
        return flags[0];
    }

    /**
     * @param f true if heartbeat
     */
    public void setHeartbeat(boolean f) {
        flags[0] = f;
    }

    public int getLength() {
        return connections.size() * 5;
    }

    public int getSeq() {
        return seq;
    }

    public float getCost(G_Node node) {
        return connections.containsKey(node) ? connections.get(node) : -1;
    }

    public Set<G_Node> getConnectedNodes() {
        return connections.keySet();
    }

    /*
    public long getAge() {
        return age;
    }
    */

    public String getPrintableName() {
        return "id :" + this.getAdvertisingRouter();
    }

    /**
     * @param neighbour the neighbour of the advertising router
     * @param cost      the cost
     */
    public void addConnectionData(G_Node neighbour, float cost) {
        if (connections == null) {
            connections = new HashMap<G_Node, Float>();
        }
        connections.put(neighbour, cost);
    }

    /**
     * the method to use to add for current LSR .
     * advertising nodes must be one node of this edge
     *
     * @param edge the edge
     */

    public void addConnectionData(G_Edge edge) {
        G_Node theOne = edge.getNodes()[0].getId().equals(advertisingRouter)
                ? edge.getNodes()[1]
                : edge.getNodes()[0];
        addConnectionData(theOne, edge.getCost());
    }

    /*public boolean isExpired() {
        return false;
    }
    */


    /**
     * @return the bytes of this packet
     */
    public byte[] toBytes() {

        if (data == null && !isHeartbeat())
            pack();

        int length = HEADER_LENGTH;

        try {
            length += data.length;
        } catch (NullPointerException e) {
            //what ? empty, yse , it should be empty.

        }

        byte[] packet = new byte[length];
        //long to bytes array, so bad to do so.
    /*
        PacketUtils.fill4BytesFromInt((int) age >> 12, packet, 0);
        PacketUtils.fill4BytesFromInt((int) age >> 8, packet, 4);
        PacketUtils.fill4BytesFromInt((int) age >> 4, packet, 8);
        PacketUtils.fill4BytesFromInt((int) age >> 0, packet, 12);
    */

        // id part
        packet[0] = (byte) advertisingRouter.charAt(0);
        byte flagsRep = 0;
        for (int i = 0; i < 8; i++) {
            if (!flags[i]) {
                continue;
            }
            flagsRep = (byte) (flagsRep | (0b00000001 << i));
        }
        packet[1] = flagsRep;
        PacketUtils.fill4BytesFromInt(seq, packet, 2);
        if (!isHeartbeat()) {
            PacketUtils.fill4BytesFromInt(data.length, packet, 6);
            System.arraycopy(data,0,packet,HEADER_LENGTH,data.length);
            /*
            for (int i = HEADER_LENGTH; i < data.length + HEADER_LENGTH; i++)
                packet[i] = data[i - HEADER_LENGTH];
            */
        } else {
            PacketUtils.fill4BytesFromInt(0, packet, 6);
        }
        return packet;
    }

}
