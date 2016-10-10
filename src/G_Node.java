/**
 * Created by matthew on 8/10/16.
 * Rep of router in the network.
 *
 */
public class G_Node {

    private final String id;

    public G_Node(String id){
        this.id = id;
    }

    public String getId(){
        return id;
    }

    @Override
    public boolean equals(Object obj){
        if(obj instanceof G_Node){
            return ((G_Node) obj).id.equals(id);
        }else {
            return false;
        }
    }

    @Override
    public String toString(){
        return getId() + " ";
    }
}
