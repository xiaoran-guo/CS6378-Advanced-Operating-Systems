import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class Server {
    private Integer id;
    private VectorClock vectorClock;
    private ServerStatus serverStatus;
    private HostConfig hostConfig;
    private ConcurrentSkipListMap<Integer, StoredContent> contentStorage;
    private ConcurrentSkipListMap<Integer, TreeMap<VectorClock, StoredContent>> buffer;
    private TreeMap<Integer, Socket> socketTreeMap;

    Server(HostConfig hostConfig) {
        this.id = hostConfig.getId();
        this.vectorClock = new VectorClock(hostConfig.getNumOfServer());
        this.contentStorage = new ConcurrentSkipListMap<>();
        this.buffer = new ConcurrentSkipListMap<>();
        this.serverStatus = ServerStatus.normal;
        this.socketTreeMap = new TreeMap<>();
        this.hostConfig = hostConfig;
    }

    public ServerStatus getServerStatus() {
        return serverStatus;
    }

    public ConcurrentSkipListMap<Integer, StoredContent> getContentStorage() {
        return contentStorage;
    }

    private synchronized void ticktock() {
        vectorClock.ticktock(id);
    }

    private synchronized void ticktock(VectorClock vectorClock, Integer fromId) {
        this.vectorClock.ticktock(id, vectorClock, fromId);
    }

    void start() {
        new Thread(new serverListenerThread(hostConfig.getPort())).start();
        new Thread(new interactiveServerThread()).start();
    }

    private class serverListenerThread implements Runnable {
        private ServerSocket serverSocket;

        private serverListenerThread(Integer port) {
            try {
                this.serverSocket = new ServerSocket(port);
                System.out.println("Start Listening...");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    new Thread(new ServerMessageHandlerThread(socket)).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class ServerMessageHandlerThread implements Runnable {
        private Socket socket;

        private ServerMessageHandlerThread(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            ObjectInputStream ois;
            ObjectOutputStream oos;

            try {
                ois = new ObjectInputStream(socket.getInputStream());
                oos = new ObjectOutputStream(socket.getOutputStream());

                boolean terminate = false;
                while (!terminate) {
                    try {
                        Message receivedMsg = (Message)ois.readObject();
                        if (getServerStatus() != ServerStatus.fail) {
                            ticktock(receivedMsg.getTimestamp(), receivedMsg.getFrom());
                        }
                        if ((receivedMsg != null ? receivedMsg.getMessageType() : null) == MessageType.serverPutEnd) {
                            terminate = true;
                        } else {
                            Message replyMsg = generateReplyMsg(receivedMsg);
                            ticktock();
                            oos.writeObject(replyMsg);
                        }
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                        terminate = true;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized Message generateReplyMsg(Message receivedMsg) {
        if (serverStatus == ServerStatus.fail) {
            return new Message(MessageType.serverUnavailable, null, id, vectorClock);
        }
        switch (receivedMsg.getMessageType()) {
            case clientGet: return generateReplyMsgClientGet(receivedMsg);
            case clientPut: return generateReplyMsgClientPut(receivedMsg);
            case serverPutReq: return generateReplyMsgServerPutReq(receivedMsg);
            case serverPut: return generateReplyMsgServerPut(receivedMsg);
            default: System.out.println("Undefined message type."); return null;
        }
    }

    private Message generateReplyMsgClientGet(Message receivedMsg) {
        if (receivedMsg.getStoredContent() != null) {
            return new Message(MessageType.clientGetAck, getContentStorage().get(receivedMsg.getStoredContent().getObjectId()), id, vectorClock);
        } else {
            return null;
        }
    }

    private Message generateReplyMsgClientPut(Message receivedMsg) {
        boolean primaryACK = false, secodaryACK = false, tertiaryACK = false;

        if (receivedMsg.getStoredContent() != null) {
            Integer primaryHash = ReadConfig.getInstance().hashToServer(receivedMsg.getStoredContent().getObjectId(), 0);
            Integer secondaryHash = ReadConfig.getInstance().hashToServer(receivedMsg.getStoredContent().getObjectId(), 1);
            Integer tertiaryHash = ReadConfig.getInstance().hashToServer(receivedMsg.getStoredContent().getObjectId(), 2);

            if (primaryHash.equals(id) || secondaryHash.equals(id) || tertiaryHash.equals(id)) {

                receivedMsg.getStoredContent().setVectorClock(this.vectorClock.getTimestamp());
                buffContent(receivedMsg.getStoredContent());

                Message serverPutReqMsg = new Message(MessageType.serverPutReq, receivedMsg.getStoredContent(), id);
                if (!Objects.equals(primaryHash, id)) {
                    primaryACK = writeToOtherServer(serverPutReqMsg, primaryHash);
                }
                if (!Objects.equals(secondaryHash, id)) {
                    secodaryACK = writeToOtherServer(serverPutReqMsg, secondaryHash);
                }
                if (!Objects.equals(tertiaryHash, id)) {
                    tertiaryACK = writeToOtherServer(serverPutReqMsg, tertiaryHash);
                }

                if (primaryACK || secodaryACK || tertiaryACK) {
                    Message serverPutMsg = new Message(MessageType.serverPut, receivedMsg.getStoredContent(), id);
                    if (!Objects.equals(primaryHash, id)) {
                        primaryACK = writeToOtherServer(serverPutMsg, primaryHash);
                    }
                    if (!Objects.equals(secondaryHash, id)) {
                        secodaryACK = writeToOtherServer(serverPutMsg, secondaryHash);
                    }
                    if (!Objects.equals(tertiaryHash, id)) {
                        tertiaryACK = writeToOtherServer(serverPutMsg, tertiaryHash);
                    }
                    write(receivedMsg);
                }
            } else {
                System.out.print("Wrong server to store.");
            }
        }
        if (primaryACK || secodaryACK || tertiaryACK) {
            return new Message(MessageType.clientPutAck, receivedMsg.getStoredContent(), id);
        } else {
            return new Message(MessageType.clientPutFail, receivedMsg.getStoredContent(), id);
        }
    }

    private Message generateReplyMsgServerPutReq(Message receivedMsg) {
        if (receivedMsg.getStoredContent() == null) {
            return new Message(MessageType.serverPutFail, null, id, vectorClock);
        } else {
            buffContent(receivedMsg.getStoredContent());
            return new Message(MessageType.serverPutReqAck, null, id, vectorClock);
        }
    }

    private Message generateReplyMsgServerPut(Message receivedMsg) {
        write(receivedMsg);
        return new Message(MessageType.serverPutAck, null, id, vectorClock);
    }

    private boolean writeToOtherServer(Message message, Integer id) {
        ticktock();
        boolean res = false;

        Socket socket;
        ObjectInputStream ois;
        ObjectOutputStream oos;
        try {
            socket = new Socket(ReadConfig.getInstance().getServerNodes().get(id).getHostName(),
                    ReadConfig.getInstance().getServerNodes().get(id).getPort());
            ois = new ObjectInputStream(socket.getInputStream());
            oos = new ObjectOutputStream(socket.getOutputStream());

            oos.writeObject(message);

            Message replyMsg = (Message) ois.readObject();
            if (replyMsg.getMessageType() != MessageType.serverUnavailable)
                ticktock(replyMsg.getTimestamp(), replyMsg.getFrom());
            if (message.getMessageType() == MessageType.serverPutReq && replyMsg.getMessageType() == MessageType.serverPutReqAck) {
                res = true;
            } else if (message.getMessageType() == MessageType.serverPut && replyMsg.getMessageType() == MessageType.serverPutAck) {
                res = true;
            }

            ticktock();
            oos.writeObject(new Message(MessageType.serverPutEnd, null, id, vectorClock));
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
        return res;
    }

    private synchronized void buffContent(StoredContent storedContent) {
        TreeMap<VectorClock, StoredContent> buffer = this.buffer.get(storedContent.getObjectId());

        if (buffer == null) {
            buffer = new TreeMap<>();
            this.buffer.put(storedContent.getObjectId(), buffer);
        }
        buffer.put(storedContent.getVectorClock(), storedContent);
    }

    private synchronized void write(Message receivedMsg) {
        if (buffer.get(receivedMsg.getStoredContent().getObjectId()) == null) {
            return;
        }
        for (Map.Entry<VectorClock, StoredContent> bufferEntry : buffer.get(receivedMsg.getStoredContent().getObjectId()).entrySet()) {
            if (bufferEntry.getValue().getVectorClock().compareTo(receivedMsg.getStoredContent().getVectorClock()) <= 0) {
                record(receivedMsg.getFrom(), receivedMsg.getStoredContent().getObjectId(),
                        contentStorage.get(receivedMsg.getStoredContent().getObjectId()) != null ?
                                contentStorage.get(receivedMsg.getStoredContent().getObjectId()).getObjectValue() : null,
                        receivedMsg.getStoredContent().getObjectValue());
                contentStorage.put(receivedMsg.getStoredContent().getObjectId(), bufferEntry.getValue());
                buffer.get(receivedMsg.getStoredContent().getObjectId()).remove(bufferEntry.getKey());
            }
        }
        if (buffer.get(receivedMsg.getStoredContent().getObjectId()).size() == 0) {
            buffer.remove(receivedMsg.getStoredContent().getObjectId());
        }
    }

    private void record(Integer clientId, Integer fid, Integer oldValue, Integer newValue){
        Date date = new Date();

        String home = System.getProperty("user.home");

        File f = new File(System.getProperty("user.home") + "/data" + this.id + ".txt");
        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        StringBuilder record = new StringBuilder("Content ");
        record.append(fid).append(" was modified from ").append(oldValue != null ? oldValue : "null")
                .append("   \tto ").append(newValue).append("   \tby node ").append(clientId).append("\tat ")
                .append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(date.getTime())).append("\n");
        try {
            Files.write(Paths.get(home + "/data" + this.id + ".txt"), (record.toString()).getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class interactiveServerThread implements Runnable {

        @Override
        public void run() {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
            String line;
            boolean terminate = false;

            while (!terminate) {
                try {
                    line = bufferedReader.readLine();
                    if (line.equals("")) {
                        continue;
                    }
                    String[] cmd = line.split(" ");
                    if (cmd[0].equals("offline")) {
                        serverStatus = ServerStatus.fail;
                    }
                    if (cmd[0].equals("online")) {
                        serverStatus = ServerStatus.normal;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public String toString() {
        return "Server{" +
                "id=" + id +
                ", vectorClock=" + vectorClock +
                ", serverStatus=" + serverStatus +
                ", hostConfig=" + hostConfig +
                ", contentStorage=" + contentStorage +
                ", buffer=" + buffer +
                ", socketTreeMap=" + socketTreeMap +
                '}';
    }
}
