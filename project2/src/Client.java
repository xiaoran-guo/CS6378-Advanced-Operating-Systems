import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Client {
    int id;
    private HashMap<Integer, Socket> connections;
    private HashMap<Integer, ObjectInputStream> ois;
    private HashMap<Integer, ObjectOutputStream> oos;

    public Client(int id) {
        this.id = id;
        this.connections = new HashMap<>();
        this.ois = new HashMap<>();
        this.oos = new HashMap<>();
    }

    void manuStart() {
        System.out.println("Manual client start.");
        establishConnection();

        String getOrPut;
        int serverIdx, objectId, objectValue;

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
        String line;

        while (true) {
            try {
                line = bufferedReader.readLine();
                if ("terminate".equals(line)) {
                    break;
                } else if (!"".equals(line)) {
                    String[] cmd = line.split("//s+");
                    serverIdx = Integer.parseInt(cmd[0].trim());
                    getOrPut = cmd[1].trim();
                    objectId = Integer.parseInt(cmd[2].trim());
                    objectValue = (cmd.length == 4) ? Integer.parseInt(cmd[3].trim()) : null;

                    if (!sendReceiveMsg(serverIdx, getOrPut, objectId, objectValue)) {
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    synchronized void autoStart() {
        System.out.println("Automated Client start.");
        establishConnection();

        String getOrPut;
        int serverIdx, objectId, objectValue;

        Random r = new Random();

        for (int i = 0; i < 30; i++) {
            try {
                wait(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            serverIdx = r.nextInt(3);
            getOrPut = "put";
            objectId = r.nextInt(7);
            objectValue = r.nextInt(1000);

            if (!sendReceiveMsg(serverIdx, getOrPut, objectId, objectValue)) {
                break;
            }
        }
    }

    private boolean sendReceiveMsg(int serverIdx, String getOrPut, int objectId, int objectValue) {
        int serverId = ReadConfig.getInstance().hashToServer(objectId, serverIdx);
        if ((!"put".equals(getOrPut)) && (!"get".equals(getOrPut))) {
            return false;
        }
        if ("put".equals(getOrPut)) {
            try {
                oos.get(serverId).writeObject(new Message(MessageType.clientPut, new StoredContent(objectId, objectValue), id));
                Message receivedMsg = (Message)ois.get(serverId).readObject();
                System.out.println("Set content " + objectId + " as " + objectValue + " . Result: " + ((receivedMsg.getMessageType() == MessageType.clientPutAck) ? "Success" : "Fail") );
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        } else if ("get".equals(getOrPut)) {
            try {
                this.oos.get(serverId).writeObject(new Message(MessageType.clientGet, new StoredContent(objectId), id));
                Message receivedMsg = (Message) ois.get(serverId).readObject();
                if (receivedMsg.getMessageType() == MessageType.serverUnavailable) {
                    System.out.println("Server Unavailable.");
                } else if (receivedMsg.getStoredContent() == null) {
                    System.out.println("Content Not exist.");
                } else {
                    System.out.println(receivedMsg.getStoredContent().getObjectValue());
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private void establishConnection() {
        System.out.println("Establish connection to");
        for (Map.Entry<Integer, HostConfig> e : ReadConfig.getInstance().getServerNodes().entrySet()) {
            System.out.println("\tServer " + e.getKey() + " " + e.getValue().getHostName() + ":" + e.getValue().getPort() + "...");
            try {
                Socket socket = new Socket(e.getValue().getHostName(), e.getValue().getPort());
                connections.put(e.getValue().getId(), socket);
                ois.put(e.getValue().getId(), new ObjectInputStream(socket.getInputStream()));
                oos.put(e.getValue().getId(), new ObjectOutputStream(socket.getOutputStream()));
                System.out.println("\tDone.");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }

        }
    }
}
