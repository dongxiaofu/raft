package V1;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Worker {
    private int count = 1;
    private int replicatedCount;
    private int term = 0;
    private int logIndex = 0;
    private ArrayList<String> serverList = new ArrayList<String>();
    private ArrayList<Socket> sendSockts = new ArrayList<Socket>();
    private ArrayList<Socket> receiveSockets = new ArrayList<Socket>();
    private int electionTimeout;
    private int heartBeatTimeout = 1000;
    private volatile Queue<String> receivedMessage = new LinkedList<String>();
    private volatile ServerSocket serverSocket;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
        node.setPort(port);
    }

    private int port;

    private volatile Node node;

    private Timer timer = new Timer();
    private CgProtocal cgProtocal = new CgProtocal();

    public Worker() {
        node = new Node();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
//            System.out.println(serverSocket + "\t" + port);
            if (serverSocket != null) {
                createSendSockets();
                new ReceiveWorker().start();

                System.out.println("start changed:\t" + node.isStateIsChanged());
                new TimerManger().start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createSendSockets() {
        int count = serverList.size();

        int i = 0;
        while (true) {
//            System.out.println("继续连接\t" + System.currentTimeMillis());
            if (i == count) {
                break;
            }
            for (String server : serverList) {
                try {
                    boolean isExists = false;
                    String[] serverArray = server.split(":");
                    String ip = serverArray[0];
                    int port = Integer.parseInt(serverArray[1]);
                    for (int k = 0; k < sendSockts.size(); k++) {
                        Socket socket = sendSockts.get(k);
                        if (socket.getPort() == port) {
                            isExists = true;
                            break;
                        }
                    }
                    if (isExists) {
                        continue;
                    }
                    Socket socket2 = new Socket(ip, port);
                    sendSockts.add(socket2);
                    i++;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        node.setSendSockts(sendSockts);
    }

    private class ReceiveWorker extends Thread {
        @Override
        public void run() {
            try {
                while (true) {
                    Socket socket = serverSocket.accept();
                    new ReceivePacket(socket).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ReceivePacket extends Thread {
        Socket socket;

        public ReceivePacket(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            while (true) {
                int messageLength = receiveMessage();
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (messageLength == -1) {
                    try {
                        sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        private int receiveMessage() {
            int messageLength;
            try {
                InputStream inputStream = socket.getInputStream();
                byte[] messageBytes = new byte[1024];
                messageLength = inputStream.read(messageBytes);
                Packet packet = cgProtocal.decode(messageBytes);
                if (0 == node.getPreReceiveHeartBeatTime()) {
                    node.setPreReceiveHeartBeatTime(System.currentTimeMillis());
                }
                node.setCurrentReceiveHeartBeatTime(System.currentTimeMillis());
//                System.out.println("接收信息\t" + System.currentTimeMillis() + "\t" + packet.getType());
                if (packet.getType() == MessageType.VOTE_ACK) {
                    count++;
                    System.out.println("接收投票信息\t" + count + "\t" + "state\t" + node.getState());
                    if (count > 2) {
                        node.setPreState(node.getCurrentState());
                        node.setCurrentState(NodeState.LEADER);
                        node.setState(NodeState.LEADER);
                        node.setStateIsChanged(node.checkNodeStateIsChanged());
                        node.setLeaderInit(true);

                        System.out.println("setStateIsChanged start");
                        System.out.println(node.getCurrentState() + "\t" + node.getPreState() + "\t" + node.isStateIsChanged());
                        System.out.println("setStateIsChanged end");

                        HashMap<String, String> leaderInfo = new HashMap<String, String>();
                        String ip = socket.getLocalAddress().getHostAddress();
                        int port = socket.getPort();
                        leaderInfo.put("ip", ip);
                        leaderInfo.put("port", String.valueOf(port));
                        node.setLeaderInfo(leaderInfo);
                        System.out.println("leader \t" + System.currentTimeMillis());
                    }
                } else if (packet.getType() == MessageType.REPLICATION_ACK) {
                    replicatedCount++;
                    if (replicatedCount > 2) {
                        node.setCanCommit(true);
                    }
                } else if (packet.getType() == MessageType.COMMIT) {
                    //todo commit
                    System.out.println("commit");
                    // commit 之后，发送 COMMIT_ACK，不用timer
                    // 需要先找出Leader
                    Socket socket = node.getLeaderSocket();
                    OutputStream outputStream = socket.getOutputStream();
                    Packet packet1 = new Packet();
                    packet1.setType(MessageType.COMMIT_ACK);
                    packet1.setHost("127.0.0.1");
                    packet1.setPort(node.getPort());
                    packet1.setMessage("");
                    outputStream.write(cgProtocal.encode(packet1));
                } else if (packet.getType() == MessageType.LEADER) {
                    System.out.println("I am leader\t" + System.currentTimeMillis());
                    node.setPreState(node.getCurrentState());
                    node.setCurrentState(NodeState.FOLLOWER);
                    node.setState(NodeState.FOLLOWER);
                    node.setStateIsChanged(node.checkNodeStateIsChanged());
                    node.setPreReceiveHeartBeatTime(node.getCurrentReceiveHeartBeatTime());
                    node.setCurrentReceiveHeartBeatTime(System.currentTimeMillis());
                } else if (packet.getType() == MessageType.REQUEST_VOTE) {
//                    System.out.println("投票0\t" + "isVoted:" + node.isVoted() + "\tstate:" + node.getState());
                    int candidateTerm = packet.getTerm();
                    int nodeTerm = packet.getTerm();
                    if ((candidateTerm >= nodeTerm) && (!node.isVoted())) {
                        // 投该节点一票
                        // 需要找出对应的服务器，此信息是哪个服务器（ip:port)发出的，根据(ip:port)找出对应的socket
                        String ip = socket.getInetAddress().getHostAddress();
                        int port = packet.getPort();
                        Socket targetSocket = null;
                        System.out.println(socket);
                        for (int i = 0; i < sendSockts.size(); i++) {
                            Socket socket = sendSockts.get(i);
                            System.out.println(i + ":\t" + socket);
                            if (port == socket.getPort()) {
                                targetSocket = socket;
                                break;
                            }
                        }

                        if (targetSocket != null) {
                            OutputStream outputStream = targetSocket.getOutputStream();
                            Packet packet1 = new Packet();
                            packet1.setType(MessageType.VOTE_ACK);
                            packet1.setHost("127.0.0.1");
                            packet1.setPort(node.getPort());
                            packet1.setMessage("");
                            outputStream.write(cgProtocal.encode(packet1));

                            node.setTerm(candidateTerm);
                            node.setVoted(true);
                        }
                    }
                } else if (packet.getType() == MessageType.COMMAND) {
                    // 接收到客户端命令
                    // 写入日志
                    System.out.println("log");
                    // 通知其他node复制
                    Packet packet1 = new Packet();
                    packet1.setMessage("SET X 5");
                    packet1.setType(MessageType.COMMAND);
                    packet1.setHost("127.0.0.1");
                    packet1.setPort(node.getPort());
                    node.sendMessage(cgProtocal.encode(packet1));
                } else if (packet.getType() == MessageType.EMPTY_HEART_BEAT) {
                    System.out.println("EMPTY_HEART_BEAT\t" + System.currentTimeMillis());
                    node.setPreReceiveHeartBeatTime(node.getCurrentReceiveHeartBeatTime());
                    node.setCurrentReceiveHeartBeatTime(System.currentTimeMillis());
                }

                return messageLength;
            } catch (Exception e) {
                e.printStackTrace();
                return -1;
            }
        }
    }

    private class TimerManger extends Thread {
        @Override
        public void run() {
            while (true) {
                MyTask myTask = null;
                byte currentState = node.getCurrentState();
                if (currentState == NodeState.CANDIDATE) {
                    myTask = new RequestVoteTask(node);
                } else if (currentState == NodeState.LEADER) {
                    myTask = new HeartBeatTask(node, receivedMessage);
                } else if (currentState == NodeState.FOLLOWER) {
                    myTask = new LeaderIsAliveTask(node);
                }

                if(null != myTask){
                    System.out.println("myTask\t" + currentState);
                    myTask.start();
                    Random random = new Random(50);
                    try{
                        sleep(500 + random.nextInt(50));
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public ArrayList<String> getServerList() {
        return serverList;
    }

    public void setServerList(ArrayList<String> serverList) {
        this.serverList = serverList;
    }
}
