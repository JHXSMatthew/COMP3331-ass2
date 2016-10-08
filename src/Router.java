import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;

/**
 * Created by matthew on 8/10/16.
 * The main router class.
 */
public class Router {

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

        Router r = new Router(args[0],Integer.parseInt(args[1]),neighbours);

    }


    private String id;
    private int port;


    private List<Neighbour> neighbours;

    //the graph that record all information related to current network topology
    private G_Graph graph;


    public Router(String id, int port, List<Neighbour> neighbourList){
        this.neighbours = neighbourList;
        this.id = id;
        this.port = port;
        graph = new G_Graph();

    }


    public void forwardPacket(){

    }

    public void update(){
        
    }


}
