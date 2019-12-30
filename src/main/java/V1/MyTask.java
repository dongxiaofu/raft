package V1;

import java.util.Queue;
import java.util.TimerTask;

public class MyTask  extends TimerTask {
    protected Node node;
    protected Queue<String> receivedMessage;

    public MyTask(Node node) {
        this.node = node;
    }

    @Override
    public void run() {

    }
}
