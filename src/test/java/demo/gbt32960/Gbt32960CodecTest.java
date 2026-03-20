package demo.gbt32960;

import demo.gbt32960.codec.Gbt32960FrameDecoder;
import demo.gbt32960.codec.Gbt32960FrameEncoder;
import demo.gbt32960.codec.Gbt32960Protocol;
import demo.gbt32960.model.Packet32960;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderException;
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
        ByteBuf encoded = encoder.readOutbound();
        byte[] bytes = new byte[encoded.readableBytes()];
        encoded.readBytes(bytes);
        encoded.release();

        assertFalse(channel.writeInbound(Unpooled.wrappedBuffer(bytes, 0, 10)));
        assertNull(channel.readInbound());
        assertTrue(channel.writeInbound(Unpooled.wrappedBuffer(bytes, 10, bytes.length - 10)));
        Packet32960 decoded = channel.readInbound();
        assertNotNull(decoded);
        assertEquals("LJ8ABC12345678901", decoded.vin());
        channel.finishAndReleaseAll();
        encoder.finishAndReleaseAll();
    }

    @Test
    void shouldRejectBadChecksum() {
        EmbeddedChannel encoder = new EmbeddedChannel(new Gbt32960FrameEncoder());
        byte[] body = Gbt32960Protocol.buildDemoLoginBody();
        Packet32960 packet = new Packet32960(0x01, 0xFE, "LJ8ABC12345678901", 0x01, body.length, body, 0);
        assertTrue(encoder.writeOutbound(packet));
        ByteBuf encoded = encoder.readOutbound();
        byte[] bytes = new byte[encoded.readableBytes()];
        encoded.readBytes(bytes);
        encoded.release();
        bytes[bytes.length - 1] ^= 0x01;

        EmbeddedChannel decoder = new EmbeddedChannel(new Gbt32960FrameDecoder());
        DecoderException ex = assertThrows(DecoderException.class, () -> decoder.writeInbound(Unpooled.wrappedBuffer(bytes)));
        assertTrue(ex.getCause() instanceof IllegalArgumentException);
        assertTrue(ex.getCause().getMessage().contains("bcc mismatch"));
        decoder.finishAndReleaseAll();
        encoder.finishAndReleaseAll();
    }

    @Test
    void shouldDecodeTwoConcatenatedFrames() {
        EmbeddedChannel encoder = new EmbeddedChannel(new Gbt32960FrameEncoder());
        byte[] loginBody = Gbt32960Protocol.buildDemoLoginBody();
        byte[] realtimeBody = Gbt32960Protocol.buildDemoRealtimeBody();
        assertTrue(encoder.writeOutbound(new Packet32960(0x01, 0xFE, "LJ8ABC12345678901", 0x01, loginBody.length, loginBody, 0)));
        assertTrue(encoder.writeOutbound(new Packet32960(0x02, 0xFE, "LJ8ABC12345678901", 0x01, realtimeBody.length, realtimeBody, 0)));

        ByteBuf first = encoder.readOutbound();
        ByteBuf second = encoder.readOutbound();
        ByteBuf combined = Unpooled.buffer(first.readableBytes() + second.readableBytes());
        combined.writeBytes(first);
        combined.writeBytes(second);
        first.release();
        second.release();

        EmbeddedChannel decoder = new EmbeddedChannel(new Gbt32960FrameDecoder());
        assertTrue(decoder.writeInbound(combined));
        Packet32960 p1 = decoder.readInbound();
        Packet32960 p2 = decoder.readInbound();
        assertNotNull(p1);
        assertNotNull(p2);
        assertEquals(0x01, p1.cmd());
        assertEquals(0x02, p2.cmd());
        decoder.finishAndReleaseAll();
        encoder.finishAndReleaseAll();
    }
}
