package V1;

public class MessageType {
    public static byte REQUEST_VOTE = 1;
    public static byte EMPTY_HEART_BEAT = 2;
    public static byte VOTE_ACK = 3;
    public static byte COMMIT = 4;
    public static byte NOTIFY = 5;
    public static byte REPLICATION_ACK = 6;
    public static byte COMMIT_ACK = 7;
    public static byte LEADER = 8;
    public static byte COMMAND = 9;
}
