public class Main {
    public static void main(String[] args) {
        int id = Integer.parseInt(args[0]);
        ReadConfig clientServerConfig = ReadConfig.getInstance();
        if (clientServerConfig.getServerNodes().containsKey(id) || clientServerConfig.getClientNodes().containsKey(id)) {
            int mode = clientServerConfig.getServerNodes().containsKey(id) ? 0 : clientServerConfig.getClientNodes().get(id).getMode();
            switch (mode) {
                case 0:
                    new Server(clientServerConfig.getServerNodes().get(id).start());
                    break;
                case 1:
                    new Client(id).manuStart();
                    break;
                case 2:
                    new Client(id).autoStart();
                    break;
                default:
                    break;
            }
        } else {
            System.out.println("Please input a valid number from 0 to 11.");
        }
    }
}
