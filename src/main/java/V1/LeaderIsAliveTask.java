package V1;

import java.util.Date;

public class LeaderIsAliveTask extends MyTask {
    public LeaderIsAliveTask(Node node){
        super(node);
    }

    @Override
    public void run(){

        if(0 == node.getCurrentReceiveHeartBeatTime()){
           node.setCurrentReceiveHeartBeatTime(System.currentTimeMillis());
        }

        if(node.getCurrentReceiveHeartBeatTime() - node.getPreReceiveHeartBeatTime() > 1000){
            System.out.println("leader down\t" + System.currentTimeMillis() + "\t" + node.getCurrentState());
            node.setPreState(node.getCurrentState());
            node.setCurrentState(NodeState.CANDIDATE);
            node.setState(NodeState.CANDIDATE);
            node.setStateIsChanged(node.checkNodeStateIsChanged());
        }
        System.out.println("2LeaderIsAliveTask\t" + new Date().toLocaleString());
        System.out.println("node state:\t" + node.getState() + "\t" + node.isStateIsChanged());
    }
}
