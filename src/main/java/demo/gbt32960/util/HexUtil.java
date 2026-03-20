package demo.gbt32960.util;

public final class HexUtil {

    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private HexUtil() {
    }

    public static String toHex(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[i * 2] = HEX[v >>> 4];
            out[i * 2 + 1] = HEX[v & 0x0F];
        }
        return new String(out);
    }

    public static byte[] fromHex(String s) {
        String normalized = s.replaceAll("\\s+", "");
        if ((normalized.length() & 1) != 0) {
            throw new IllegalArgumentException("hex length must be even");
        }
        byte[] out = new byte[normalized.length() / 2];
        for (int i = 0; i < out.length; i++) {
            int hi = Character.digit(normalized.charAt(i * 2), 16);
            int lo = Character.digit(normalized.charAt(i * 2 + 1), 16);
            if (hi < 0 || lo < 0) {
                throw new IllegalArgumentException("bad hex string");
            }
            out[i] = (byte) ((hi << 4) | lo);
        }
        return out;
    }
}
