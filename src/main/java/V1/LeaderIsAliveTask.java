package V1;

import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.locks.LockSupport;

public class LeaderIsAliveTask extends MyTask {
    private Worker.CreateSendSockts createSendSockts;


    public LeaderIsAliveTask(Node node, Worker.CreateSendSockts createSendSockts){
        super(node);
        this.createSendSockts = createSendSockts;
    }

    @Override
    public void start(){

        if(0 == node.getCurrentReceiveHeartBeatTime()){
           node.setCurrentReceiveHeartBeatTime(System.currentTimeMillis());
        }
        System.out.println("leader live\t" + node.getCurrentReceiveHeartBeatTime() + "\t" + node.getPreReceiveHeartBeatTime());
        if(node.getCurrentReceiveHeartBeatTime() - node.getPreReceiveHeartBeatTime() > 2000){
            System.out.println("leader down\t" + System.currentTimeMillis() + "\t" + node.getCurrentState());
            node.setPreState(node.getCurrentState());
            node.setCurrentState(NodeState.CANDIDATE);
            node.setState(NodeState.CANDIDATE);
            node.setStateIsChanged(node.checkNodeStateIsChanged());
            // 想了很长时间才想到这一行代码
            node.setVoted(false);
            node.setVoteCounter(0);

            node.setSendSockts(new ArrayList<Socket>());
            System.out.println("=======CreateSendSockts restart===========");
            LockSupport.unpark(createSendSockts);
        }
        System.out.println("2LeaderIsAliveTask\t" + new Date().toLocaleString());
        System.out.println("node state:\t" + node.getState() + "\t" + node.isStateIsChanged());
    }
}
