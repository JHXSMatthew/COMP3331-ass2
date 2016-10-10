import java.util.Arrays;

/**
 * Created by matthew on 8/10/16.
 * The edge in this case, actually , it should be called "link" anyway.
 */
public class G_Edge {

    private G_Node[] connected = new G_Node[2];
    private int cost;

    public G_Edge(G_Node nodea, G_Node nodeb,int cost){
        connected[0] = nodea;
        connected[1] = nodeb;
        this.cost = cost;
    }

    /**
     *
     * @return the array holding, original.
     */
    public G_Node[] getNodes(){
        return connected;
    }

    public G_Node getTheOtherNode(G_Node node){
        if(connected[0] == node){
            return connected[1];
        }
        return connected[0];
    }

    public int getCost(){
        return cost;
    }

    public void setCost(int c){
        this.cost = c;
    }

    public boolean isEdge(G_Node... node ){
        if(node.length > 2){
            return false;
        }
        for(G_Node temp : node){
            boolean isIn = false;
            for(G_Node in : connected)
                if(in == temp)
                    isIn = true;

            if(isIn == false)
                return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object obj){
        if(obj instanceof G_Edge){
            return ((G_Edge) obj).isEdge(connected) && cost == ((G_Edge) obj).cost;
        }else{
            return false;
        }
    }

    @Override
    public String toString(){
        return "[" + getNodes()[0].getId()  + "<->" + getNodes()[1].getId() + "] " ;
    }
}
