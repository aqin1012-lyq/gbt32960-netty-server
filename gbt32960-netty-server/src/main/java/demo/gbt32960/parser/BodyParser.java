package demo.gbt32960.parser;

import demo.gbt32960.model.CommandBody;
import demo.gbt32960.model.InfoItem;
import demo.gbt32960.model.Packet32960;
import demo.gbt32960.util.HexUtil;
import demo.gbt32960.util.JsonUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BodyParser {

    public CommandBody parse(Packet32960 packet) {
        return switch (packet.cmd()) {
            case 0x01 -> parseLogin(packet);
            case 0x02 -> parseRealtime(packet);
            case 0x03 -> parseReissue(packet);
            case 0x04 -> parseLogout(packet);
            default -> new UnknownCommandBody(packet.cmd(), packet.dataUnit());
        };
    }

    private CommandBody parseLogin(Packet32960 packet) {
        ByteReader br = new ByteReader(packet.dataUnit());
        Map<String, Object> fields = new LinkedHashMap<>();
        if (br.remaining() >= 6) {
            fields.put("collectTimeBcd", HexUtil.toHex(br.readBytes(6)));
        }
        if (br.remaining() >= 2) {
            fields.put("serialNo", br.readU16());
        }
        if (br.remaining() > 0) {
            fields.put("rawTailHex", HexUtil.toHex(br.readRemaining()));
        }
        return new SimpleCommandBody("vehicle_login", fields);
    }

    private CommandBody parseLogout(Packet32960 packet) {
        ByteReader br = new ByteReader(packet.dataUnit());
        Map<String, Object> fields = new LinkedHashMap<>();
        if (br.remaining() >= 6) {
            fields.put("collectTimeBcd", HexUtil.toHex(br.readBytes(6)));
        }
        if (br.remaining() >= 2) {
            fields.put("serialNo", br.readU16());
        }
        if (br.remaining() > 0) {
            fields.put("rawTailHex", HexUtil.toHex(br.readRemaining()));
        }
        return new SimpleCommandBody("vehicle_logout", fields);
    }

    private CommandBody parseRealtime(Packet32960 packet) {
        return new InfoItemsCommandBody("realtime_report", parseInfoItems(packet.dataUnit()));
    }

    private CommandBody parseReissue(Packet32960 packet) {
        return new InfoItemsCommandBody("reissue_report", parseInfoItems(packet.dataUnit()));
    }

    private List<InfoItem> parseInfoItems(byte[] bytes) {
        ByteReader br = new ByteReader(bytes);
        List<InfoItem> items = new ArrayList<>();
        while (br.readable()) {
            int type = br.readU8();
            items.add(parseInfoItem(type, br));
        }
        return items;
    }

    private InfoItem parseInfoItem(int type, ByteReader br) {
        return switch (type) {
            case 0x01 -> parseVehicleData(br);
            case 0x02 -> parseDriveMotorData(br);
            case 0x03 -> parseFuelCellData(br);
            case 0x04 -> parseEngineData(br);
            case 0x05 -> parseLocationData(br);
            case 0x06 -> parseExtremeData(br);
            case 0x07 -> parseAlarmData(br);
            default -> throw new IllegalArgumentException("unknown info type=" + type);
        };
    }

    private InfoItem parseVehicleData(ByteReader br) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("collectTimeBcd", HexUtil.toHex(br.readBytes(6)));
        fields.put("vehicleStatus", br.readU8());
        fields.put("chargeStatus", br.readU8());
        fields.put("runMode", br.readU8());
        int speedRaw = br.readU16();
        long mileageRaw = br.readU32();
        int totalVoltageRaw = br.readU16();
        int totalCurrentRaw = br.readU16();
        int soc = br.readU8();
        int insulation = br.readU16();
        fields.put("speedRaw", speedRaw);
        fields.put("speedKph", speedRaw / 10.0);
        fields.put("mileageRaw", mileageRaw);
        fields.put("mileageKm", mileageRaw / 10.0);
        fields.put("totalVoltageRaw", totalVoltageRaw);
        fields.put("totalVoltageV", totalVoltageRaw / 10.0);
        fields.put("totalCurrentRaw", totalCurrentRaw);
        fields.put("totalCurrentA", (totalCurrentRaw - 1000) / 10.0);
        fields.put("socPct", soc);
        fields.put("insulationOhm", insulation);
        return new SimpleInfoItem(0x01, "vehicle_data", fields);
    }

    private InfoItem parseDriveMotorData(ByteReader br) {
        Map<String, Object> fields = new LinkedHashMap<>();
        int motorCount = br.readU8();
        fields.put("motorCount", motorCount);
        List<Object> motors = new ArrayList<>();
        for (int i = 0; i < motorCount; i++) {
            Map<String, Object> one = new LinkedHashMap<>();
            one.put("seq", br.readU8());
            one.put("status", br.readU8());
            one.put("controllerTemp", br.readU8());
            one.put("speedRaw", br.readU16());
            one.put("torqueRaw", br.readU16());
            one.put("motorTemp", br.readU8());
            one.put("inputVoltageRaw", br.readU16());
            one.put("dcBusCurrentRaw", br.readU16());
            motors.add(one);
        }
        fields.put("motors", motors);
        return new SimpleInfoItem(0x02, "drive_motor_data", fields);
    }

    private InfoItem parseFuelCellData(ByteReader br) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("stackVoltageRaw", br.readU16());
        fields.put("stackCurrentRaw", br.readU16());
        fields.put("fuelConsumptionRaw", br.readU16());
        if (br.remaining() > 0) {
            fields.put("rawTailHex", HexUtil.toHex(br.readRemaining()));
        }
        return new SimpleInfoItem(0x03, "fuel_cell_data", fields);
    }

    private InfoItem parseEngineData(ByteReader br) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("status", br.readU8());
        fields.put("crankshaftSpeed", br.readU16());
        fields.put("fuelConsumptionRate", br.readU16());
        return new SimpleInfoItem(0x04, "engine_data", fields);
    }

    private InfoItem parseLocationData(ByteReader br) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("positionStatus", br.readU8());
        long longitudeRaw = br.readU32();
        long latitudeRaw = br.readU32();
        fields.put("longitudeRaw", longitudeRaw);
        fields.put("longitude", longitudeRaw / 1_000_000.0);
        fields.put("latitudeRaw", latitudeRaw);
        fields.put("latitude", latitudeRaw / 1_000_000.0);
        return new SimpleInfoItem(0x05, "location_data", fields);
    }

    private InfoItem parseExtremeData(ByteReader br) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("maxVoltageSubsystemNo", br.readU8());
        fields.put("maxVoltageCellNo", br.readU8());
        fields.put("maxVoltageRaw", br.readU16());
        fields.put("minVoltageSubsystemNo", br.readU8());
        fields.put("minVoltageCellNo", br.readU8());
        fields.put("minVoltageRaw", br.readU16());
        return new SimpleInfoItem(0x06, "extreme_data", fields);
    }

    private InfoItem parseAlarmData(ByteReader br) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("alarmLevel", br.readU8());
        fields.put("thermalRunawayLevel", br.readU8());
        fields.put("systemAlarmBits", br.readU32());
        int faultCount = br.readU8();
        fields.put("faultCount", faultCount);
        List<Object> faults = new ArrayList<>();
        for (int i = 0; i < faultCount; i++) {
            faults.add(br.readU32());
        }
        fields.put("faultCodes", faults);
        if (br.remaining() > 0) {
            fields.put("rawTailHex", HexUtil.toHex(br.readRemaining()));
        }
        return new SimpleInfoItem(0x07, "alarm_data", fields);
    }

    private static class ByteReader {
        private final byte[] data;
        private int pos;

        private ByteReader(byte[] data) {
            this.data = data == null ? new byte[0] : data;
        }

        private boolean readable() {
            return remaining() > 0;
        }

        private int remaining() {
            return data.length - pos;
        }

        private int readU8() {
            ensure(1);
            return data[pos++] & 0xFF;
        }

        private int readU16() {
            ensure(2);
            int value = ((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF);
            pos += 2;
            return value;
        }

        private long readU32() {
            ensure(4);
            long value = ((long) (data[pos] & 0xFF) << 24)
                    | ((long) (data[pos + 1] & 0xFF) << 16)
                    | ((long) (data[pos + 2] & 0xFF) << 8)
                    | (long) (data[pos + 3] & 0xFF);
            pos += 4;
            return value;
        }

        private byte[] readBytes(int n) {
            ensure(n);
            byte[] out = Arrays.copyOfRange(data, pos, pos + n);
            pos += n;
            return out;
        }

        private byte[] readRemaining() {
            return readBytes(remaining());
        }

        private void ensure(int n) {
            if (remaining() < n) {
                throw new IllegalArgumentException("not enough bytes, need=" + n + ", remaining=" + remaining());
            }
        }
    }

    private static class SimpleCommandBody implements CommandBody {
        private final String name;
        private final Map<String, Object> fields;

        private SimpleCommandBody(String name, Map<String, Object> fields) {
            this.name = name;
            this.fields = fields;
        }

        @Override
        public String toPrettyJson() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("cmdName", name);
            map.put("fields", fields);
            return JsonUtil.toPrettyJson(map);
        }
    }

    private static class InfoItemsCommandBody implements CommandBody {
        private final String name;
        private final List<InfoItem> items;

        private InfoItemsCommandBody(String name, List<InfoItem> items) {
            this.name = name;
            this.items = items;
        }

        @Override
        public String toPrettyJson() {
            Map<String, Object> map = new LinkedHashMap<>();
            List<Object> list = new ArrayList<>();
            for (InfoItem item : items) {
                list.add(item.toMap());
            }
            map.put("cmdName", name);
            map.put("items", list);
            return JsonUtil.toPrettyJson(map);
        }
    }

    private static class UnknownCommandBody implements CommandBody {
        private final int cmd;
        private final byte[] raw;

        private UnknownCommandBody(int cmd, byte[] raw) {
            this.cmd = cmd;
            this.raw = raw;
        }

        @Override
        public String toPrettyJson() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("cmdName", "unknown_" + cmd);
            map.put("rawHex", HexUtil.toHex(raw));
            return JsonUtil.toPrettyJson(map);
        }
    }

    private static class SimpleInfoItem implements InfoItem {
        private final int type;
        private final String name;
        private final Map<String, Object> fields;

        private SimpleInfoItem(int type, String name, Map<String, Object> fields) {
            this.type = type;
            this.name = name;
            this.fields = fields;
        }

        @Override
        public int type() {
            return type;
        }

        @Override
        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("type", type);
            map.put("name", name);
            map.put("fields", fields);
            return map;
        }
    }
}
