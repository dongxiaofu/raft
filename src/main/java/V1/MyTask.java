package V1;

import java.util.Queue;

abstract public class MyTask {
    protected Node node;
    protected Queue<String> receivedMessage;

    public MyTask(Node node) {
        this.node = node;
    }

    abstract void start();
}
