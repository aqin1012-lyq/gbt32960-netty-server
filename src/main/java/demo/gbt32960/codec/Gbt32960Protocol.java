package demo.gbt32960.codec;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;

public final class Gbt32960Protocol {

    public static final byte HEADER = 0x23;
    public static final int MIN_FRAME_LEN = 25;

    private Gbt32960Protocol() {
    }

    public static int calcBcc(int cmd, int ack, byte[] vinBytes, int encryptFlag, int dataLen, byte[] encryptedData) {
        int x = 0;
        x ^= (cmd & 0xFF);
        x ^= (ack & 0xFF);
        for (byte b : vinBytes) {
            x ^= (b & 0xFF);
        }
        x ^= (encryptFlag & 0xFF);
        x ^= ((dataLen >> 8) & 0xFF);
        x ^= (dataLen & 0xFF);
        for (byte b : encryptedData) {
            x ^= (b & 0xFF);
        }
        return x & 0xFF;
    }

    public static byte[] decrypt(int encryptFlag, byte[] data) {
        return data;
    }

    public static byte[] encrypt(int encryptFlag, byte[] data) {
        return data;
    }

    public static byte[] buildDemoLoginBody() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(nowAsBcd6());
            writeU16(out, 1);
            out.write("89860012345678901234".getBytes());
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static byte[] buildDemoRealtimeBody() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(0x01);
            out.write(nowAsBcd6());
            out.write(0x01);
            out.write(0x01);
            out.write(0x01);
            writeU16(out, 635);
            writeU32(out, 123456L);
            writeU16(out, 3650);
            writeU16(out, 1234);
            out.write(71);
            writeU16(out, 2500);

            out.write(0x07);
            out.write(0x02);
            out.write(0x01);
            writeU32(out, 0x00000011L);
            out.write(0x01);
            writeU32(out, 0x12345678L);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static byte[] nowAsBcd6() {
        LocalDateTime now = LocalDateTime.now();
        int year = now.getYear() % 100;
        return new byte[]{
                toBcd(year),
                toBcd(now.getMonthValue()),
                toBcd(now.getDayOfMonth()),
                toBcd(now.getHour()),
                toBcd(now.getMinute()),
                toBcd(now.getSecond())
        };
    }

    private static byte toBcd(int value) {
        return (byte) (((value / 10) << 4) | (value % 10));
    }

    public static void writeU16(ByteArrayOutputStream out, int v) {
        out.write((v >> 8) & 0xFF);
        out.write(v & 0xFF);
    }

    public static void writeU32(ByteArrayOutputStream out, long v) {
        out.write((int) ((v >> 24) & 0xFF));
        out.write((int) ((v >> 16) & 0xFF));
        out.write((int) ((v >> 8) & 0xFF));
        out.write((int) (v & 0xFF));
    }
}
