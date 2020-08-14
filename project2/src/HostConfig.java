public class HostConfig {
    private String hostName;
    private int port;
    private int id;
    private int numOfServer;
    private int numOfClient;
    private int mode;

    public HostConfig(String hostName, int port, int id, int numOfServer, int numOfClient, int mode) {
        this.hostName = hostName;
        this.port = port;
        this.id = id;
        this.numOfServer = numOfServer;
        this.numOfClient = numOfClient;
        this.mode = mode;
    }

    public String getHostName() {
        return hostName;
    }

    public int getPort() {
        return port;
    }

    public int getId() {
        return id;
    }

    public int getNumOfServer() {
        return numOfServer;
    }

    public int getNumOfClient() {
        return numOfClient;
    }

    public int getMode() {
        return mode;
    }

    @Override
    public String toString() {
        return "HostConfig{" +
                "hostName='" + hostName + '\'' +
                ", port=" + port +
                ", id=" + id +
                ", numOfServer=" + numOfServer +
                ", numOfClient=" + numOfClient +
                ", mode=" + mode +
                '}';
    }
}
