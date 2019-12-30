package V1;

import java.util.Arrays;

public class CgProtocal {

    public byte[] encode(Packet packet) {

        String message = packet.getMessage();
        int term = packet.getTerm();
        int index = packet.getIndex();
        byte type = packet.getType();
        int port = packet.getPort();
        String host = packet.getHost();
        int hostLength = host.length();

        byte[] header = new byte[8];
        int contentLength = message.length();
        header[0] = (byte) ((contentLength >> 8) & 0xff);
        header[1] = (byte) (contentLength & 0xff);
        header[2] = type;

        byte[] termBytes = intToByTe(term);
        byte[] indexBytes = intToByTe(index);
        byte[] portBytes = intToByTe(port);

        byte[] content = message.getBytes();
        byte[] hostBytes = host.getBytes();

        byte[] onePacket = new byte[20 + contentLength + hostLength];
        System.arraycopy(header, 0, onePacket, 0, 8);
        System.arraycopy(termBytes, 0, onePacket, 8, 4);
        System.arraycopy(indexBytes, 0, onePacket, 12, 4);
        System.arraycopy(portBytes, 0, onePacket, 16, 4);
        System.arraycopy(content, 0, onePacket, 20, contentLength);
        System.arraycopy(hostBytes, 0, onePacket, 20 + contentLength, hostLength);

        return onePacket;
    }

    public Packet decode(byte[] messageBytes) {
        byte[] header = Arrays.copyOfRange(messageBytes, 0, 8);
        byte[] termBytes = Arrays.copyOfRange(messageBytes, 8, 12);
        byte[] indexBytes = Arrays.copyOfRange(messageBytes, 12, 16);
        byte[] portBytes = Arrays.copyOfRange(messageBytes, 16, 20);
        int contentLength = (int)((header[0] << 8) & 0xff) + (int)(header[1] & 0xff);
        byte[] content = Arrays.copyOfRange(messageBytes, 20, 20 + contentLength);
        byte[] host = Arrays.copyOfRange(messageBytes, 20 + contentLength, messageBytes.length);

        int term = byteToInt(termBytes);
        int index = byteToInt(indexBytes);
        int port = byteToInt(portBytes);
        String message = new String(content);
        String hostString = new String(host);
        byte type = header[2];

        Packet packet = new Packet();
        packet.setMessage(message);
        packet.setTerm(term);
        packet.setIndex(index);
        packet.setType(type);
        packet.setPort(port);
        packet.setHost(hostString);

        return packet;
    }

    public int byteToInt(byte[] length) {
        int i0 = (int) ((length[0] & 0xff) << 24);
        int i1 = (int) ((length[1] & 0xff) << 16);
        int i2 = (int) ((length[2] & 0xff) << 8);
        int i3 = (int) (length[3] & 0xff);

        return (i0 + i1 + i2 + i3);
    }

    public byte[] intToByTe(int length) {
        byte[] newLength;
        newLength = new byte[4];
        newLength[0] = (byte) ((length >> 24) & 0xff);
        newLength[1] = (byte) ((length >> 16) & 0xff);
        newLength[2] = (byte) ((length >> 8) & 0xff);
        newLength[3] = (byte) (length & 0xff);

        return newLength;
    }
}
