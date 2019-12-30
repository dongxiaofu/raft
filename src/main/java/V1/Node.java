package V1;


import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class Node {
    private byte state = NodeState.FOLLOWER;
    private volatile boolean flag = false;
    private byte messageType;
    private int term = 0;
    private int index = 0;
    private volatile boolean stateIsChanged;
    private volatile byte preState = NodeState.FOLLOWER;
    private volatile byte currentState = NodeState.FOLLOWER;
    private long preReceiveHeartBeatTime = 0;
    private long currentReceiveHeartBeatTime = 0;
    private ArrayList<Socket> sendSockts;
    private boolean canCommit = false;
    private boolean leaderInit = false;
    private boolean voted = false;
    private HashMap<String, String> leaderInfo;
    private int port;

    private CgProtocal cgProtocal = new CgProtocal();

    public long getPreReceiveHeartBeatTime() {
        return preReceiveHeartBeatTime;
    }

    public void setPreReceiveHeartBeatTime(long preReceiveHeartBeatTime) {
        this.preReceiveHeartBeatTime = preReceiveHeartBeatTime;
    }

    public long getCurrentReceiveHeartBeatTime() {
        return currentReceiveHeartBeatTime;
    }

    public void setCurrentReceiveHeartBeatTime(long currentReceiveHeartBeatTime) {
        this.currentReceiveHeartBeatTime = currentReceiveHeartBeatTime;
    }

    public byte getPreState() {
        return preState;
    }

    public void setPreState(byte preState) {
        this.preState = preState;
    }

    public byte getCurrentState() {
        return currentState;
    }

    public void setCurrentState(byte currentState) {
        this.currentState = currentState;
    }

    public boolean isFlag() {
        return flag;
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
    }

    public byte getState() {
        return state;
    }

    public byte getMessageType() {
        return messageType;
    }

    public void setMessageType(byte messageType) {
        this.messageType = messageType;
    }

    public void setState(byte state) {
        this.state = state;
    }

    public int getTerm() {
        return term;
    }

    public void setTerm(int term) {
        this.term = term;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public boolean isStateIsChanged() {
        return stateIsChanged;
    }

    public void setStateIsChanged(boolean stateIsChanged) {
        this.stateIsChanged = stateIsChanged;
    }

    public ArrayList<Socket> getSendSockts() {
        return sendSockts;
    }

    public void setSendSockts(ArrayList<Socket> sendSockts) {
        this.sendSockts = sendSockts;
    }

    public boolean isCanCommit() {
        return canCommit;
    }

    public void setCanCommit(boolean canCommit) {
        this.canCommit = canCommit;
    }

    public boolean isLeaderInit() {
        return leaderInit;
    }

    public void setLeaderInit(boolean leaderInit) {
        this.leaderInit = leaderInit;
    }

    public boolean isVoted() {
        return voted;
    }

    public void setVoted(boolean voted) {
        this.voted = voted;
    }

    public HashMap<String, String> getLeaderInfo() {
        return leaderInfo;
    }

    public void setLeaderInfo(HashMap<String, String> leaderInfo) {
        this.leaderInfo = leaderInfo;
    }

    public synchronized void startTimerManager() throws InterruptedException {
        if (flag) {
            wait();
        } else {
            System.out.println("=======node startTimerManager\t" + stateIsChanged + "\t" + state);
            if (stateIsChanged) {
                // 关键点
                flag = true;
//                System.out.println("唤醒 NodeStateWatchDog");
                notify();
                stateIsChanged = !stateIsChanged;
            }
        }
    }

    public synchronized void startNodeStateWatchDog() throws InterruptedException {
        if (!flag) {
            wait();
        } else {
            System.out.println("node startNodeStateWatchDog\t" + stateIsChanged + "\t" + state);
            if(stateIsChanged || state == NodeState.FOLLOWER){
                flag = false;
                System.out.println("唤醒 timerManager");
                notify();
                stateIsChanged = false;
            }
        }
    }

    public void sendRequestVote(Packet packet) throws IOException {
        byte[] vote = cgProtocal.encode(packet);
        sendMessage(vote);
    }

    public void sendAck() throws IOException {
        Packet packet = new Packet();
        byte[] ack = cgProtocal.encode(packet);
        sendMessage(ack);
    }

    public void sendHeartBeat(Packet packet) throws IOException {
        byte[] heartBeat = cgProtocal.encode(packet);
        sendMessage(heartBeat);
    }


    public void sendMessage(byte[] vote) throws IOException {
        for (Socket socket : sendSockts) {
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(vote);
        }
    }

    public Socket getLeaderSocket(){
        for(int i = 0; i < sendSockts.size(); i++){
            Socket socket = sendSockts.get(i);
            String ip = socket.getInetAddress().getHostAddress();
            int port = socket.getPort();
            if(ip.equals(leaderInfo.get("ip")) && (port == Integer.parseInt(leaderInfo.get("port")))){
                return socket;
            }
        }

        return null;
    }

    public boolean checkNodeStateIsChanged() {
        return getCurrentState() != getPreState();
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
