import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by Matthew on 11/10/2016.
 */
public class ConfigGenerator {


    public static void main(String argv[]){
        int nodeCount = Integer.parseInt(argv[0]);
        int repeat = 1;
        if(argv.length > 1){
            repeat = Integer.parseInt(argv[1]);
        }
        new ConfigGenerator(nodeCount,repeat);
    }

    private Random r;

    private static char STARTING_CHAR = 'A';
    private static int MAX_COST = 7;
    private static int BASE_PORT = 2000;
    private G_Graph graph ;


    public ConfigGenerator(int nodeCount, int repeat){
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
                if(r.nextBoolean()){
                    graph.connect(from,to,(float)(((int)(r.nextFloat() * 10))/10) + r.nextInt(MAX_COST));
                }
            }
        }

        try {
            for(G_Node node : nodes){
                File f = new File("out" + File.pathSeparator  + i + File.pathSeparator + "config" + node.getId() + ".txt");
                f.mkdirs();
                f.createNewFile();


                BufferedWriter writer = new BufferedWriter(new FileWriter(f));
                writer.write(i);
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


}
