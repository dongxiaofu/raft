package V1;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

public class Worker {
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
    private static volatile CreateSendSockts createSendSockts;
    private static volatile TimerManger timerManger;
    private int serverCount = 0;
    private Lock lock = new ReentrantLock();

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
        node.setPort(port);
    }

    private int port;

    private volatile Node node;

    private CgProtocal cgProtocal = new CgProtocal();

    public Worker() {
        node = new Node();
    }

    public void start() {
        try {
            createSendSockts = new CreateSendSockts();
            ReceiveWorker receiveWorker = new ReceiveWorker(lock);
            createSendSockts.start();
            receiveWorker.start();
//            createSendSockts.join();
//            receiveWorker.join();
            timerManger = new TimerManger(createSendSockts);
            timerManger.start();
        } catch (Exception e) {
//            e.printStackTrace();
        }
    }

    private class ReceiveWorker extends Thread {
        private Lock lock;

        public ReceiveWorker(Lock lock) {
            this.lock = lock;
        }

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(port);
                while (true) {
                    Socket socket = serverSocket.accept();
                    new ReceivePacket(socket, lock).start();
                }
            } catch (IOException e) {
//                e.printStackTrace();
            }
        }
    }

    private class ReceivePacket extends Thread {
        Socket socket;
        private Lock lock;


        public ReceivePacket(Socket socket, Lock lock) {
            this.socket = socket;
            this.lock = lock;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    receiveMessage();
                } catch (Exception e) {
//                    e.printStackTrace();
                    break;
                }

                try {
                    sleep(1000);
                } catch (InterruptedException e) {
//                    e.printStackTrace();
                }
            }
        }

        private void receiveMessage() throws Exception {
            InputStream inputStream = socket.getInputStream();
            byte[] messageBytes = new byte[1024];
            inputStream.read(messageBytes);
            Packet packet = cgProtocal.decode(messageBytes);
            if (0 == node.getPreReceiveHeartBeatTime()) {
                node.setPreReceiveHeartBeatTime(System.currentTimeMillis());
            }
            node.setCurrentReceiveHeartBeatTime(System.currentTimeMillis());
//                System.out.println("接收信息\t" + System.currentTimeMillis() + "\t" + packet.getType());
            if (packet.getType() == MessageType.VOTE_ACK) {
                node.setVoteCounter(node.getVoteCounter() + 1);
                System.out.println("接收投票信息\t" + node.getVoteCounter() + "\t" + "state\t" + node.getState());
                if (node.getVoteCounter() >= 2) {
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
                if (node.getState() != NodeState.LEADER) {
                    node.setPreState(node.getCurrentState());
                    node.setCurrentState(NodeState.FOLLOWER);
                    node.setState(NodeState.FOLLOWER);
                    node.setStateIsChanged(node.checkNodeStateIsChanged());
                    node.setPreReceiveHeartBeatTime(node.getCurrentReceiveHeartBeatTime());
                    node.setCurrentReceiveHeartBeatTime(System.currentTimeMillis());
                }
                System.out.println("I am leader\t" + System.currentTimeMillis() + "\t" + node.getCurrentState() + "\t" + node.getState());
            } else if (packet.getType() == MessageType.REQUEST_VOTE) {
                lock.lock();
                try {
                    int candidateTerm = packet.getTerm();
                    int nodeTerm = node.getTerm();
                    System.out.println("投票0\t" + "isVoted:" + node.isVoted() + "\tstate:" + node.getState() + "\t" + candidateTerm + "\t" + nodeTerm);
                    if ((candidateTerm >= nodeTerm) && (!node.isVoted())) {
                        // 投该节点一票
                        // 需要找出对应的服务器，此信息是哪个服务器（ip:port)发出的，根据(ip:port)找出对应的socket
                        String ip = socket.getInetAddress().getHostAddress();
                        int port = packet.getPort();
                        Socket targetSocket = null;
                        System.out.println(socket);
                        ArrayList<Socket> sendSockets = node.getSendSockts();
                        for (int i = 0; i < sendSockets.size(); i++) {
                            Socket socket = sendSockets.get(i);
                            System.out.println(i + ":\t" + socket);
                            if (port == socket.getPort()) {
                                targetSocket = socket;
                                break;
                            }
                        }
                        System.out.println(targetSocket + "\t" + "targetSocket0");
                        if (targetSocket != null) {
                            System.out.println(targetSocket + "\t" + "targetSocket1");
                            OutputStream outputStream = targetSocket.getOutputStream();
                            Packet packet1 = new Packet();
                            packet1.setType(MessageType.VOTE_ACK);
                            packet1.setHost("127.0.0.1");
                            packet1.setPort(node.getPort());
                            packet1.setMessage("");
                            outputStream.write(cgProtocal.encode(packet1));

                            node.setTerm(candidateTerm);
                            node.setVoted(true);
//                                lock.unlock();
                        }
                    }
                } finally {
                    lock.unlock();
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
                System.out.println("EMPTY_HEART_BEAT\t" + System.currentTimeMillis() + "\t" + node.getState());
                node.setPreReceiveHeartBeatTime(node.getCurrentReceiveHeartBeatTime());
                node.setCurrentReceiveHeartBeatTime(System.currentTimeMillis());
            }
        }
    }

    private class TimerManger extends Thread {
        private CreateSendSockts createSendSockts;

        public TimerManger(CreateSendSockts createSendSockts) {
            this.createSendSockts = createSendSockts;
        }

        @Override
        public void run() {
            while (true) {
                if (node.getSendSockts() != null && node.getSendSockts().size() != serverList.size()) {
                    System.out.println("服务器未准备好，sleep");
                    LockSupport.park();
                }
                System.out.println("=============start timer=============");
                MyTask myTask = null;
                byte currentState = node.getCurrentState();
                if (currentState == NodeState.CANDIDATE) {
                    myTask = new RequestVoteTask(node);
                } else if (currentState == NodeState.LEADER) {
                    myTask = new HeartBeatTask(node, receivedMessage);
                } else if (currentState == NodeState.FOLLOWER) {
                    myTask = new LeaderIsAliveTask(node, createSendSockts);
                }

                if (null != myTask) {
                    System.out.println("myTask\t" + currentState);
                    try {
                        Random random = new Random(50);
                        sleep(500 + random.nextInt(50));
                        myTask.start();
                    } catch (Exception e) {
                        LockSupport.unpark(createSendSockts);
//                        e.printStackTrace();
                    }
                }
            }
        }
    }

    protected class CreateSendSockts extends Thread {
        @Override
        public void run() {
            int count = serverList.size();
            while (true) {
                if (serverCount == count) {
                    node.setSendSockts(sendSockts);
                    node.setVoted(false);
                    node.setVoteCounter(0);
                    sendSockts = new ArrayList<Socket>();
                    serverCount = 0;
                    LockSupport.unpark(timerManger);
                    System.out.println("=======CreateSendSockts sleep===========");
                    LockSupport.park();
                    System.out.println("=======CreateSendSockts sleep0000===========");
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
                        System.out.println("socket2\t" + socket2);
                        sendSockts.add(socket2);
                        serverCount++;
                    } catch (Exception e) {
//                        e.printStackTrace();
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
