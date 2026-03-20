package demo.gbt32960.codec;

import demo.gbt32960.model.Packet32960;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.charset.StandardCharsets;

public class Gbt32960FrameEncoder extends MessageToByteEncoder<Packet32960> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Packet32960 msg, ByteBuf out) {
        byte[] vinBytes = msg.vin().getBytes(StandardCharsets.US_ASCII);
        if (vinBytes.length != 17) {
            throw new IllegalArgumentException("VIN must be exactly 17 ASCII chars");
        }
        byte[] encryptedData = Gbt32960Protocol.encrypt(msg.encryptFlag(), msg.dataUnit());
        int dataLen = encryptedData.length;

        out.writeByte(Gbt32960Protocol.HEADER);
        out.writeByte(Gbt32960Protocol.HEADER);
        out.writeByte(msg.cmd());
        out.writeByte(msg.ack());
        out.writeBytes(vinBytes);
        out.writeByte(msg.encryptFlag());
        out.writeShort(dataLen);
        out.writeBytes(encryptedData);
        out.writeByte(Gbt32960Protocol.calcBcc(msg.cmd(), msg.ack(), vinBytes, msg.encryptFlag(), dataLen, encryptedData));
    }
}
