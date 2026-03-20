package demo.gbt32960.codec;

import demo.gbt32960.model.Packet32960;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class Gbt32960FrameDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        for (;;) {
            if (in.readableBytes() < Gbt32960Protocol.MIN_FRAME_LEN) {
                return;
            }

            in.markReaderIndex();
            byte h1 = in.readByte();
            byte h2 = in.readByte();
            if (h1 != Gbt32960Protocol.HEADER || h2 != Gbt32960Protocol.HEADER) {
                in.resetReaderIndex();
                in.readByte();
                continue;
            }

            if (in.readableBytes() < 22) {
                in.resetReaderIndex();
                return;
            }

            int cmd = in.readUnsignedByte();
            int ack = in.readUnsignedByte();
            byte[] vinBytes = new byte[17];
            in.readBytes(vinBytes);
            String vin = new String(vinBytes, StandardCharsets.US_ASCII);
            int encryptFlag = in.readUnsignedByte();
            int dataLen = in.readUnsignedShort();

            if (in.readableBytes() < dataLen + 1) {
                in.resetReaderIndex();
                return;
            }

            byte[] encryptedData = new byte[dataLen];
            in.readBytes(encryptedData);
            int checksum = in.readUnsignedByte();

            int calc = Gbt32960Protocol.calcBcc(cmd, ack, vinBytes, encryptFlag, dataLen, encryptedData);
            if (calc != checksum) {
                throw new IllegalArgumentException("bcc mismatch, calc=" + calc + ", actual=" + checksum);
            }

            byte[] plain = Gbt32960Protocol.decrypt(encryptFlag, encryptedData);
            out.add(new Packet32960(cmd, ack, vin, encryptFlag, dataLen, plain, checksum));
        }
    }
}
