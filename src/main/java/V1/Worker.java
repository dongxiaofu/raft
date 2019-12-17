package V1;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;

public class Worker {
    private byte state = NodeState.FOLLOWER;
    private int count;
    private int replicatedCount;
    private boolean canCommit = false;
    private int term = 0;
    private int logIndex = 0;
    private ArrayList<String> serverList = new ArrayList<String>();
    private ArrayList<Socket> sendSockts = new ArrayList<Socket>();
    private ArrayList<Socket> receiveSockets = new ArrayList<Socket>();
    private int electionTimeout;
    private int heartBeatTimeout = 1000;
    private Node node;

    private CgProtocal cgProtocal = new CgProtocal();
    private Timer timer = new Timer();

    public Worker() {
        node = new Node();
        node.setState(state);
    }


    public void start() {
        createSendSockets();
        new ReceiveWorker().start();

        new NodeStateWatchDog().start();
        new TimerManger().start();

    }

    public void sendRequestVote() throws IOException {
        Packet packet = new Packet();
        byte[] vote = cgProtocal.encode(packet);
        sendMessage(vote);
    }

    public void sendAck() throws IOException {
        Packet packet = new Packet();
        byte[] ack = cgProtocal.encode(packet);
        sendMessage(ack);
    }

    public void sendHeartBeat() throws IOException {
        Packet packet = new Packet();
        byte[] heartBeat = cgProtocal.encode(packet);
        sendMessage(heartBeat);
    }


    private void sendMessage(byte[] vote) throws IOException {
        for (Socket socket : sendSockts) {
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(vote);
        }
    }


    private void createSendSockets() {
        for (String server : serverList) {
            String[] serverArray = server.split(":");
            String ip = serverArray[0];
            int port = Integer.parseInt(serverArray[1]);
            try {
                sendSockts.add(new Socket(ip, port));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class ReceiveWorker extends Thread {
        @Override
        public void run() {
            ServerSocket serverSocket;
            try {
                serverSocket = new ServerSocket(5000);
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
                if (messageLength == -1) {
                    try {
                        sleep(300);
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
                byte[] messageBytes = {};
                messageLength = inputStream.read(messageBytes);
                Packet packet = cgProtocal.decode(messageBytes);
                if (packet.getType() == MessageType.VOTE_ACK) {
                    count++;
                    if (count > 2) {
                        state = NodeState.LEADER;
                    }
                } else if (packet.getType() == MessageType.REPLICATION_ACK) {
                    replicatedCount++;
                    if (replicatedCount > 2) {
                        canCommit = true;
                    }
                }

                return messageLength;
            } catch (Exception e) {
                e.printStackTrace();
                return -1;
            }
        }
    }

    private class NodeStateWatchDog extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    node.startNodeStateWatchDog();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class TimerManger extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    node.startTimerManager();
                    startTimer();
                    if (Worker.this.checkNodeStateIsChanged()) {
                        node.setFlag(true);
                        stopTimer();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private void startTimer() {
            MyTask myTask = new HeartBeatTask();
            if (Worker.this.state == NodeState.CANDIDATE) {
                myTask = new RequestVoteTask();
            } else if (Worker.this.state == NodeState.LEADER) {
                myTask = new HeartBeatTask();
            }
            Worker.this.timer.schedule(myTask, new Date(), 2000);
        }

        private void stopTimer() {
            timer.cancel();
        }
    }

    protected boolean checkNodeStateIsChanged() {
        return true;
    }
}
