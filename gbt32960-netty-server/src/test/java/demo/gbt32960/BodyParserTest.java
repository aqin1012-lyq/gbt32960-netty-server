package demo.gbt32960;

import demo.gbt32960.codec.Gbt32960Protocol;
import demo.gbt32960.model.CommandBody;
import demo.gbt32960.model.Packet32960;
import demo.gbt32960.parser.BodyParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BodyParserTest {

    @Test
    void shouldParseRealtimeBody() {
        byte[] body = Gbt32960Protocol.buildDemoRealtimeBody();
        Packet32960 packet = new Packet32960(0x02, 0xFE, "LJ8ABC12345678901", 0x01, body.length, body, 0);
        BodyParser parser = new BodyParser();
        CommandBody commandBody = parser.parse(packet);
        String json = commandBody.toPrettyJson();
        assertTrue(json.contains("realtime_report"));
        assertTrue(json.contains("vehicle_data"));
        assertTrue(json.contains("alarm_data"));
        assertTrue(json.contains("compatibility-skeleton"));
        assertTrue(json.contains("thermalRunawayActive"));
    }

    @Test
    void shouldParseLoginBodyWithMetadata() {
        byte[] body = Gbt32960Protocol.buildDemoLoginBody();
        Packet32960 packet = new Packet32960(0x01, 0xFE, "LJ8ABC12345678901", 0x01, body.length, body, 0);
        BodyParser parser = new BodyParser();
        String json = parser.parse(packet).toPrettyJson();
        assertTrue(json.contains("vehicle_login"));
        assertTrue(json.contains("protocolFamily"));
        assertTrue(json.contains("placeholder"));
    }
}
