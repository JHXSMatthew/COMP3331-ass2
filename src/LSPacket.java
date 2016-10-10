
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Created by matthew on 8/10/16.
 * The link-state packet.
 * It's very nice to not deal with packet lost :).
 *
 */
public class LSPacket {


    /*
      The LS packet design

                          ------------------ Packet header ------------------ 26 bytes

                  0-15  bytes --- long   ---  the age
                  16    byte  --- String ---  advertising router id  ---
                  17    byte  ---  |R|R|R|R|R|R|R|k
                       K : keep alive, true if it is a heartbeat packet
                       R : Reserved
                  18-21 bytes --- int    ---  Seq number , that's the "version" of this packet.
                  22-25 bytes --- int    ---  the actual data segment length, HEADER EXCLUDED

                          ------------------ data seg ------------------
                  {
                    26    byte  --- String  ---  id of Neighbour
                    27-30 bytes --- int     ---  the cost from advertising router to the router above
                  }* total bytes 5 bytes
                  ( n segments of this type of data, where n <= 0) , data seg maybe empty if

     */


    //attributes of this packet, included in packet header
    private String advertisingRouter;
    private boolean[] flags = new boolean[8];
    private long age = 0;
    private int seq = -1;


    //data segment
    private HashMap<G_Node,Integer> connections = new HashMap<G_Node,Integer>();
    private byte[] data  = null;

    // dynamic wrapper stuff
    private boolean expired = true;

    //static constants
    private final static int HEADER_LENGTH = 26;


    /**
     *
     * construct basic packet information.
     * not fully un-packed the data seg as long as the (method unpack) in called.
     * @param packet the receiving data
     */
    public LSPacket(byte[] packet){
        age =  (long) PacketUtils.get4BytesInt(packet,0) << 32*3
                | PacketUtils.get4BytesInt(packet,4) << 32*2
                | PacketUtils.get4BytesInt(packet,8) << 32
                | PacketUtils.get4BytesInt(packet,12);
        expired = System.currentTimeMillis() <= age;

        advertisingRouter = Byte.toString(packet[16]);
        //bits to booleans, load flags
        for (int i = 0; i < 8; i++)
            flags[i] = (packet[17] & (0b00000001 << i)) != 0;

        seq = PacketUtils.get4BytesInt(packet,18);

        data = Arrays.copyOfRange(packet,HEADER_LENGTH,PacketUtils.get4BytesInt(packet,22));

    }

    public LSPacket(String advertisingRouter ,boolean isHeartbeat,int seq,long ages, G_Edge... connections){
        this.seq = seq;
        this.age = System.currentTimeMillis() + ages;
        this.flags[0] = isHeartbeat;
        this.advertisingRouter = advertisingRouter;
        if(!isHeartbeat) {
            for (G_Edge edge : connections) {
                addConnectionData(edge);
            }
        }
    }

    /**
     *  to unpack the packet
     *  @precondition !expired, !empty , seq may > current cache seq
     */
    public void unpack(G_Graph graph){
        if(data.length %5 != 0){
            System.err.println("packet error ?");
        }
        for(int i = 0 ; i < data.length ; i+=5){
            connections.put(graph.getNode(Byte.toString(data[0]),true),PacketUtils.get4BytesInt(data,i+1));
            if(Lsr.DEBUG){
                System.err.println("packet " + graph.getNode(Byte.toString(data[0]),false) + " cost: " + PacketUtils.get4BytesInt(data,i+1) );
            }
        }

    }

    /**
     *  packet the readable G_Node hash map to bytes array
     *  should be called before toBytes but I did check there anyway.
     *  direct buffer was used, hope no memory leak anyway.
     */
    public void pack(){
        ByteBuffer buffer = ByteBuffer.allocate(connections.size() * 5) ;
        for(G_Node node : connections.keySet()){
            buffer.put((byte)node.getId().charAt(0));
            PacketUtils.fill4BytesToBuffer(connections.get(node),buffer);
        }
        buffer.flip();
        data = buffer.array();
        buffer.clear();
    }


    /**
     *
     * @return the advertaising router
     */
    public String getAdvertisingRouter(){
        return advertisingRouter;
    }

    /**
     *
     * @return is heartbeat packet
     */
    public boolean isHeartbeat(){
        return flags[0];
    }

    /**
     *
     * @param f true if heartbeat
     */
    public void setHeartbeat(boolean f){
        flags[0] = f;
    }

    public int getLength(){
        return connections.size() * 5;
    }

    public int getSeq(){
        return seq;
    }

    public int getCost(G_Node node){
        return connections.containsKey(node) ? connections.get(node) : -1;
    }

    public Set<G_Node> getConnectedNodes(){
        return connections.keySet();
    }


    /**
     *
     * @param neighbour the neighbour of the advertising router
     * @param cost the cost
     */
    public void addConnectionData(G_Node neighbour, int cost){
        if(connections == null){
            connections = new HashMap<G_Node,Integer>();
        }
        connections.put(neighbour,cost);
    }

    /**
     *  the method to use to add for current LSR .
     *  advertising nodes must be one node of this edge
     * @param edge the edge
     */

    public void addConnectionData(G_Edge edge){
        G_Node theOne = edge.getNodes()[0].getId().equals(advertisingRouter)
                ? edge.getNodes()[1]
                : edge.getNodes()[0];
        addConnectionData(theOne,edge.getCost());
    }

    public boolean isExpired(){
        return expired;
    }



    /**
     *
     * @return the bytes of this packet
     */
    public byte[] toBytes(){

        if(data == null && !isHeartbeat())
            pack();

        int length =  HEADER_LENGTH;
        try{
            length += data.length;
        }catch(NullPointerException e){

        }

        byte[] packet = new byte[length];
        //long to bytes array, so bad to do so.
        PacketUtils.fill4BytesFromInt( (int) age>>12,packet,0);
        PacketUtils.fill4BytesFromInt( (int) age>>8,packet,4);
        PacketUtils.fill4BytesFromInt( (int) age>>4,packet,8);
        PacketUtils.fill4BytesFromInt( (int) age>>0,packet,12);


        // id part
        packet[16] = (byte) advertisingRouter.charAt(0);
        byte flagsRep = 0;
        for (int i = 0; i < 8; i++) {
            if (!flags[i]) {
                continue;
            }
            flagsRep = (byte) (flagsRep | (0b00000001 << i));
        }
        packet[17] = flagsRep;
        PacketUtils.fill4BytesFromInt(seq,packet,18);
        PacketUtils.fill4BytesFromInt(data.length,packet,22);
        if(!isHeartbeat()) {
            System.arraycopy(data, 0, packet, HEADER_LENGTH - 1, data.length);
        }
        return packet;
    }

}
