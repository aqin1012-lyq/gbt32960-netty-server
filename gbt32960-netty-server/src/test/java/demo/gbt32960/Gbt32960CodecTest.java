package demo.gbt32960;

import demo.gbt32960.codec.Gbt32960FrameDecoder;
import demo.gbt32960.codec.Gbt32960FrameEncoder;
import demo.gbt32960.codec.Gbt32960Protocol;
import demo.gbt32960.model.Packet32960;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Gbt32960CodecTest {

    @Test
    void shouldEncodeAndDecodePacket() {
        EmbeddedChannel channel = new EmbeddedChannel(new Gbt32960FrameEncoder(), new Gbt32960FrameDecoder());
        byte[] body = Gbt32960Protocol.buildDemoRealtimeBody();
        Packet32960 packet = new Packet32960(0x02, 0xFE, "LJ8ABC12345678901", 0x01, body.length, body, 0);

        assertTrue(channel.writeOutbound(packet));
        Object encoded = channel.readOutbound();
        assertNotNull(encoded);
        assertTrue(channel.writeInbound(encoded));

        Packet32960 decoded = channel.readInbound();
        assertNotNull(decoded);
        assertEquals(packet.cmd(), decoded.cmd());
        assertEquals(packet.ack(), decoded.ack());
        assertEquals(packet.vin(), decoded.vin());
        assertArrayEquals(packet.dataUnit(), decoded.dataUnit());
        channel.finishAndReleaseAll();
    }

    @Test
    void shouldHandleHalfPacket() {
        EmbeddedChannel channel = new EmbeddedChannel(new Gbt32960FrameDecoder());
        EmbeddedChannel encoder = new EmbeddedChannel(new Gbt32960FrameEncoder());
        byte[] body = Gbt32960Protocol.buildDemoLoginBody();
        Packet32960 packet = new Packet32960(0x01, 0xFE, "LJ8ABC12345678901", 0x01, body.length, body, 0);
        assertTrue(encoder.writeOutbound(packet));
        Object obj = encoder.readOutbound();
        byte[] bytes = new byte[((io.netty.buffer.ByteBuf) obj).readableBytes()];
        ((io.netty.buffer.ByteBuf) obj).readBytes(bytes);
        ((io.netty.buffer.ByteBuf) obj).release();

        assertFalse(channel.writeInbound(Unpooled.wrappedBuffer(bytes, 0, 10)));
        assertNull(channel.readInbound());
        assertTrue(channel.writeInbound(Unpooled.wrappedBuffer(bytes, 10, bytes.length - 10)));
        Packet32960 decoded = channel.readInbound();
        assertNotNull(decoded);
        assertEquals("LJ8ABC12345678901", decoded.vin());
        channel.finishAndReleaseAll();
        encoder.finishAndReleaseAll();
    }
}
