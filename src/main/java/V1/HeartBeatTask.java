package V1;

import java.io.IOException;
import java.util.Queue;

public  class HeartBeatTask extends MyTask {
    public HeartBeatTask(Node node, Queue receivedMessage) {
        super(node);
        this.receivedMessage = receivedMessage;
    }

    @Override
    public void start() {
        System.out.println("HeartBeatTask\t" + Thread.currentThread().getName());
        Packet packet = new Packet();
        packet.setTerm(node.getTerm());
        packet.setHost("127.0.0.1");
        packet.setPort(node.getPort());
        packet.setMessage("");
        if(receivedMessage.isEmpty()){
            if(node.isCanCommit()){
                packet.setType(MessageType.COMMIT);
                node.setCanCommit(false);
            }else {
                System.out.println("isLeaderInit\t" + node.isLeaderInit());
                if(node.isLeaderInit()){
                    packet.setType(MessageType.LEADER);
                    node.setLeaderInit(false);
                }else{
                    packet.setType(MessageType.EMPTY_HEART_BEAT);
                }
            }
        }else{
            packet.setType(MessageType.NOTIFY);
            packet.setMessage(receivedMessage.peek());
        }

        try{
            node.sendHeartBeat(packet);
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
