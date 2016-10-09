import com.sun.org.apache.bcel.internal.generic.GETFIELD;
import com.sun.org.apache.xml.internal.security.algorithms.implementations.IntegrityHmac;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by matthew on 8/10/16.
 * The link-state packet.
 * It's very nice to not deal with packet lost :).
 *
 */
public class LSPacket {


    /*
      packet design
      --- general header ---

      0  byte ---  sender id  ---
      1  byte ---  |R|R|R|R|R|R|R|k
         K : keep alive, true if it is a heartbeat packet
         R : Reserved
      2  ages --- the age of current in seconds

      --- data seg ---
      3 id of NodeFrom
      4-7 int the cost
      repeat....

     */


    private String senderID;
    private boolean[] flags = new boolean[4];
    //private List<G_Edge> connections = new ArrayList<G_Edge>();
    private HashMap<G_Node,Integer> connections = new HashMap<G_Node,Integer>();
    private byte age = 0;

    private static int TTL = 0;

    /**
     *
     * @param data the receiving data
     */
    public LSPacket(byte[] data){
        senderID = Byte.toString(data[0]);
        //bits to booleans, load flags
        for (int i = 0; i < 8; i++)
            flags[i] = (data[1] & (0b00000001 << i)) != 0;
        age = data[2];

        for(int i = 3 ; i < data.length ; i+=5){
            G_Edge edge = new G_Edge(
                    new G_Node(Byte.toString(data[i])),
                    new G_Node(Byte.toString(data[i+1])),
                    PacketUtils.get4BytesInt(data,i+2)
            );
            connections.add(edge);
        }

    }



    /**
     *
     * @param senderID the sender ID
     * @param isHeartbeat is this packet heartbeat
     * @param connections all connections to send (mindist)
     */
    public LSPacket(String senderID , boolean isHeartbeat , G_Edge... connections){
        this.flags[0] = isHeartbeat;
        this.senderID = senderID;
        for(G_Edge edge : connections){
            addConnectionData(edge);
        }
    }

    public String getSenderID(){
        return senderID;
    }

    public boolean isHeartbeat(){
        return flags[0];
    }


    public void addConnectionData(G_Node current, G_Node node2, int cost){
        if(connections == null){
            connections = new ArrayList<G_Edge>();
        }
        G_Edge edge = new G_Edge(node1,node2,cost);
        connections.add(edge);
    }

    public void addConnectionData(G_Edge edge){
        addConnectionData(edge.getNodes()[0],edge.getNodes()[1],edge.getCost());
    }

    /**
     *  //TODO  finish this asap
     * @return the bytes of this packet
     */
    public byte[] toBytes(){
        List<Byte> byteList = new ArrayList<Byte>();
        // id part
        byteList.add((byte)senderID.charAt(0));
        byte flagsRep = 0;
        for (int i = 0; i < 8; i++) {
            if (!flags[i]) {
                continue;
            }
            flagsRep = (byte) (flagsRep | (0b00000001 << i));
        }
        byteList.add(flagsRep);

        byteList.add(age);

        for(G_Edge edge : connections){
            byteList.add(edge.get)
        }

        byte[] returnValue = new byte[byteList.size()];
        for(int i = 0 ; i < byteList.size() ; i ++)
            returnValue[i] = byteList.get(i);
        return returnValue;
    }

}
