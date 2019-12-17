package V1;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class CgProtocalTest {

    private CgProtocal cgProtocal = new CgProtocal();

    @Ignore
    @org.junit.Test
    public void encode() {
        byte[] expectedMessageBytes = {};
        Packet packet = new Packet();
        packet.setMessage("SET 5");
        packet.setTerm(2);
        packet.setIndex(50);
        assertArrayEquals(expectedMessageBytes, cgProtocal.encode(packet));
    }

    @org.junit.Test
    public void decode() {
        String expectedMessage = "SET X 5;ADD X 7";
        Packet packet = new Packet();
        packet.setMessage(expectedMessage);
        packet.setTerm(2);
        packet.setIndex(50);
        byte[] messageBytes = cgProtocal.encode(packet);
        Packet packet2 = cgProtocal.decode(messageBytes);

        Packet packet1 = new Packet();
        packet1.setMessage(expectedMessage);
        packet1.setTerm(2);
        packet1.setIndex(50);

        assertEquals(packet1.getMessage(), packet2.getMessage());
        assertEquals(packet1.getTerm(), packet2.getTerm());
        assertEquals(packet1.getIndex(), packet2.getIndex());
    }

    @Test
    public void byteToInt() {
        byte[] length = {0,0,0,127};
        assertEquals(127, cgProtocal.byteToInt(length));

        byte[] length1 = {0,0,0,(byte)10000000};
        assertEquals(128, cgProtocal.byteToInt(length1));

//        (byte)10000010 是 -118。这是为什么？
//        byte[] length2 = {0,0,0,(byte)10000010};
        byte[] length2 = {0,0,0,-126};
//        byte[] length2 = cgProtocal.intToByTe(130);
        for(int i = 0; i < length2.length; i++){
            System.out.println(length2[i]);
        }
        assertEquals(130, cgProtocal.byteToInt(length2));
    }

    @Test
    public void intToByTe() {
        byte c = -106;
        System.out.println(c & 0xff);
        System.out.println((int)(c & 0xff));
        byte[] length = {0,0,0,(byte)(-106)};
        byte[] newLength = cgProtocal.intToByTe(150);
        for(int i = 0; i < newLength.length; i++){
            System.out.println(newLength[i]);
        }
        assertArrayEquals(cgProtocal.intToByTe(150), length);
    }

//    @org.junit.Test
//    public void getLength() {
//        int b = 130 & 0xff;
//        byte a = (byte)(130 & 0xff);
//        int c = (byte)(100);
//        int d = (int)(10000010);
////        byte[] expectedLength = {0,0,0,(byte)10000001};
////        assertArrayEquals(expectedLength, cgProtocal.getLength(129));
////
////        byte[] expectedLength1 = {0,0,0,(byte)10000000};
////        assertArrayEquals(expectedLength1, cgProtocal.getLength(128));
//
////        byte[] expectedLength2 = {0,0,0,(byte)10000010};  为什么是-118？
////        byte[] expectedLength2 = {(byte)11111111,(byte)11111111,(byte)11111111,(byte)10000010};
//        byte[] expectedLength2 = {0,0,0,(byte)10000010};
//    }
}