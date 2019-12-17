package V1;


import java.util.Timer;

public class Node {
    private byte state;
    private volatile boolean flag = false;
    private byte messageType;
    private int term = 0;
    private int index = 0;

    private Timer timer = new Timer();

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

    public synchronized void startTimerManager(boolean isChanged) throws InterruptedException {
        if (flag) {
            wait();
        } else {
            if (isChanged) {
                // 关键点
                flag = true;
                System.out.println("唤醒 NodeStateWatchDog");
                notify();
            }
        }
    }

    public synchronized void startNodeStateWatchDog(boolean isChanged) throws InterruptedException {
        if (!flag) {
            wait();
        } else {
            if(isChanged){
                flag = false;
                System.out.println("唤醒 timerManager");
                notify();
            }

        }
    }
}
