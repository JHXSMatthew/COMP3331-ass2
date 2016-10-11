import java.io.*;
import java.nio.IntBuffer;
import java.util.*;

/**
 * Created by MatthewYu on 11/10/2016.
 * LICENSE: GPL3
 *
 * The tester, run tests automatically and compare your output to mine..
 * It does not guarantee your code is totally correct.
 * make sure you do your own tests
 *
 * //TODO: Fail node tests.
 * //TODO: multi-threading tests
 *
 */
public class Tester {

    /**
     *  no arguments - all tests
     *  one arguments - particular test
     *  two arguments - interval between argv[0] ~ argv[1]
     *  more than two arguments - all stated tests
     *
     * I ASSUMED THE PORT STARTING FROM 2000 AND ID STARTING FROM A
     * modify  STARTING_COMMAND if you are using python or C
     * modify  COUNT  if you feel it's time consuming.
     * @param argv arguments
     */

 //================================== YOU MIGHT CHANGE THOSE ===========================================
    // in seconds,  that should be ~1.2 times your ROUTE_UPDATE_INTERVAL (i.e. you assume your code can calculate
    //the whole graph within COUNT seconds.
    // if you are not sure, leave it as 35 - 40.
    private static int COUNT = 35;

    //using variable %node%  %port% %config% for three arguments. you may change this line
    private static String STARTING_COMMAND ="java Lsr %node% %port% %config%";
//======================================================================================================

    private static char STARTING_CHAR = 'A';
    private static int BASE_PORT = 2000;

    private Timer timer = new Timer();
    private TimerTask task = null;
    private boolean failAny = false;

    public static void main(String argv[]){
        File f = new File("data");
        if(!f.exists()){
            System.err.println("Data folder has not been found");
            System.exit(-1);
        }
        File[] allTests = f.listFiles();
        if(allTests.length == 0){
            System.err.println("Empty data folder");
            System.exit(-1);
        }

        Tester tester;
        if(argv.length == 0){
            tester = new Tester(0,allTests.length -1);
        }else if(argv.length == 1){
            int theOne = Integer.parseInt(argv[0]);
            tester = new Tester(theOne,theOne);
        }else if(argv.length == 2){
            tester = new Tester(Integer.parseInt(argv[0]),Integer.parseInt(argv[1]));
        }else {
            IntBuffer buffer = IntBuffer.allocate(argv.length);
            for(String s : argv){
                buffer.put(Integer.parseInt(s));
            }
            buffer.flip();
            int[] array = buffer.array();
            buffer.clear();
            tester = new Tester(array);
        }
        tester.start();
    }


    private int[] tests = null;
    private List<ProcessWrapper> processes = null;
    private int curr = 0;

    public Tester(int starting, int end){
        if(end < starting){
            int temp = starting;
            starting = end;
            end = temp;
        }
        end ++;
        tests = new int[end - starting];
        for(int i = 0 ; i < tests.length ; i ++){
            tests[i] = starting + i;
        }
        System.out.println("Test starting from "+ starting  +" to " + end);
        System.out.println("The output for each test is in data/%test%/out folder!");
        System.out.println("by Matthew Yu");
        System.out.println("Note: this does not guarantee your code is prefect. you should do you own tests anyway.");
        System.out.println();
    }

    public Tester(int ... tests){
        this.tests = tests;
    }

    public void start(){
        processes = new ArrayList<ProcessWrapper>();
        System.out.println(curr);
        System.out.println("--- Current Testing case " + tests[curr] +" ---");
        File f = new File("data" + File.separator + tests[curr]);
        File[] listFiles = f.listFiles();
        for(File config : listFiles){
            if(config.isDirectory()){
                continue;
            }
            String nodeId = config.getName().replace("config","").replace(".txt","");
            int port = nodeId.charAt(0) - STARTING_CHAR + BASE_PORT;
            String commandToCall = STARTING_COMMAND.replace("%node%",nodeId).replace("%port%",String.valueOf(port)).replace("%config%",config.getPath());
            try {
                processes.add(new ProcessWrapper(commandToCall,nodeId));
                System.out.println("Starting Node " + nodeId + " on port " + port);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Set up finished for test case " + tests[curr]);
        System.out.println("Waiting for 40 seconds.... ");

        if(task != null ){
            try{
                task.cancel();
            }catch (Exception e){

            }
        }
        timer.purge();
        task = new TimerTask() {
            int count = 0;
            @Override
            public void run() {
                count ++;
                if(count < COUNT){
                    if(count % 5 ==0 && count != 0){
                        System.out.print("|");
                    }
                }else{
                    System.out.println();
                    callBack();
                }
            }
        };

        timer.schedule(task,0,1000);

    }

    private void callBack(){
        File testFolder = new File("data" + File.separator + tests[curr]);
        File mine = new File(testFolder, "mine");
        File f = new File(testFolder,"out");
        if(f.exists())
            f.delete();

        f.mkdirs();
        for(ProcessWrapper wrapper : processes){
            try {
                File out = new File(f,wrapper.getNode());
                if(!out.exists()){
                    out.createNewFile();
                }

                String answer = wrapper.readStd(processes.size());
                BufferedWriter writer = new BufferedWriter(new FileWriter(out));
                writer.write(answer);
                writer.close();
                wrapper.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(mine.exists()) {
            if (compare(mine, f)) {
                System.out.println("PASS!");
                System.out.println();
            } else {
                Scanner scanner = new Scanner(System.in);
                scanner.nextLine();
            }
        }else{
            f.renameTo(mine);
        }

        curr++;
        if(curr >= tests.length){
            System.out.println("All test finished!");
            if(!failAny){
                System.out.println("All tests passed ,You are awesome!");
            }
            System.exit(1);
        }
        start();
    }


    private boolean compare(File mineFolder, File outFolder){

        File[] files = mineFolder.listFiles();
        for(File temp : files){
            try {
                List<String> answer = getStringFromFile(temp);
                List<String> allOut = getStringFromFile(new File(outFolder,temp.getName()));
                HashMap<String,Float> costMap = new HashMap<String,Float>();
                for(String s : answer)
                    costMap.put(s.replaceAll("least-cost path to node ","").replaceAll(": .*",""),Float.parseFloat(s.replaceAll(".*is ","").replaceAll("is ","")));

                for(String s : allOut){
                    String id = s.replaceAll("least-cost path to node ","").replaceAll(": .*","");
                    float cost = Float.parseFloat(s.replaceAll(".*is ","").replaceAll("is ",""));
                    if(costMap.get(id) != cost){
                        System.out.println("unmatched output found, please check !");
                        System.out.println(" -- Test No." + tests[curr]);
                        System.out.println(" -- Node Id." + temp.getName());

                        System.out.println("↓↓↓↓↓↓ Yours ↓↓↓↓↓↓");
                        System.out.println(getStringFromList(allOut));
                        System.out.println("↓↓↓↓↓↓ Mine ↓↓↓↓↓↓");
                        System.out.println(getStringFromList(answer));
                        System.out.println("Note: you may have the same costs but different paths, that's fine.");
                        System.out.println(" Press enter to continue tests ....");
                        failAny = true;
                        return false;
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    public String getStringFromList(List<String> list){
        StringBuilder builder = new StringBuilder();
        for(String s : list){
            builder.append(s).append(System.lineSeparator());
        }
        return builder.toString();
    }

    public List<String> getStringFromFile(File f) throws IOException {
        List<String> list = new ArrayList<>();
        try(BufferedReader buffer = new BufferedReader(new FileReader(f))) {
            String s ;
            while ((s = buffer.readLine()) != null) {
                list.add(s);
            }
        }
        return list;
    }


    public class ProcessWrapper{

        private Process process = null;
        private BufferedReader reader = null;
        private String node;

        public ProcessWrapper(String command,String node) throws IOException {
            this.node = node;
            process  = Runtime.getRuntime().exec(command);
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        }

        public String readStd(int total) throws IOException {
            StringBuilder builder = new StringBuilder();
            String s = null;
            int i = 0;
            while((s = reader.readLine() ) != null){
                if(s.contains("least-cost")) {
                    builder.append(s)
                            .append(System.lineSeparator());
                    i++;
                }
                if(i >= total -1){
                    break;
                }
            }
            return builder.toString();
        }

        public void close() throws IOException {
            reader.close();
            process.destroy();
        }

        public String getNode(){
            return node;
        }





    }

}
