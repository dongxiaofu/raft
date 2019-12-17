package V1;

public class RequestVoteTask extends MyTask {

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName());
    }
}
