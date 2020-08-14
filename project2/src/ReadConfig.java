import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ReadConfig {
    private static ReadConfig clientServerConfig = new ReadConfig();
    private HashMap<Integer, HostConfig> serverNodes;
    private HashMap<Integer, HostConfig> clientNodes;

    public static ReadConfig getInstance(){
        return clientServerConfig;
    }

    private ReadConfig() {
        this.serverNodes = new HashMap<>();
        this.clientNodes = new HashMap<>();
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader("config.cfg"));
            try {
                String line = bufferedReader.readLine();
                String[] numSplit = line.split("//s+");
                int numOfServer = Integer.parseInt(numSplit[0]);
                int numOfClient = Integer.parseInt(numSplit[1]);

                while ((line = bufferedReader.readLine()) != null) {
                    String[] split = line.split("//s+");
                    int id = Integer.parseInt(split[0].trim());
                    String hostName = split[1].trim();
                    int port = Integer.parseInt(split[2].trim());
                    int mode = Integer.parseInt(split[3].trim());

                    if(mode == 0) {
                        serverNodes.put(id, new HostConfig(hostName, port, id, numOfServer, numOfClient, mode));
                    } else {
                        clientNodes.put(id, new HostConfig(hostName, port, id, numOfServer, numOfClient, mode));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public HashMap<Integer, HostConfig> getServerNodes() {
        return serverNodes;
    }

    public HashMap<Integer, HostConfig> getClientNodes() {
        return clientNodes;
    }

    public int hashToServer(int objectId, int num) {
        return (objectId + num) % serverNodes.size();
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ReadConfig[serverNodes = {");

        for(Map.Entry<Integer, HostConfig> e : serverNodes.entrySet()) {
            sb.append(e);
        }
        sb.append("}\nclientNodes = {");
        for(Map.Entry<Integer, HostConfig> e : clientNodes.entrySet()) {
            sb.append(e);
        }
        sb.append("}]");

        return sb.toString();
    }
}
