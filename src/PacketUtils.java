import java.nio.ByteBuffer;

/**
 * Created by Matthew on 5/09/2016
 * some help method for packets.
 */
public class PacketUtils {

    public static int get4BytesInt(byte[] data, int offset) {
        return ((data[offset] & 0xff) << 24) | ((data[offset + 1] & 0xff) << 16) |
                ((data[offset + 2] & 0xff) << 8) | (data[offset + 3] & 0xff);
    }

    public static void fill4BytesFromInt(int i, byte[] data, int offset) {
        data[offset] = (byte) (i >> 24);
        data[offset + 1] = (byte) (i >> 16);
        data[offset + 2] = (byte) (i >> 8);
        data[offset + 3] = (byte) (i >> 0);
    }

    public static void fill4BytesIntToBuffer(int i, ByteBuffer buffer) {
        buffer.put((byte) (i >> 24));
        buffer.put((byte) (i >> 16));
        buffer.put((byte) (i >> 8));
        buffer.put((byte) (i >> 0));
    }


    public static void fill4BytesFloatToBuffer(float i, ByteBuffer buffer) {
        fill4BytesIntToBuffer(Float.floatToIntBits(i), buffer);
    }


    public static float get4ByteFloat(byte[] data, int offset) {
        return Float.intBitsToFloat(get4BytesInt(data, offset));
    }

    public static void fill4BytesFloat(float f, byte[] data, int offset) {
        int floatBytes = Float.floatToIntBits(f);
        fill4BytesFromInt(floatBytes, data, offset);
    }
}