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

/**
 * 当前版本是“可运行 + 可维护的骨架解析器”。
 *
 * <p>设计原则：
 * <ul>
 *   <li>把总包解析和数据单元解析分开</li>
 *   <li>对已知示例报文做稳定解析</li>
 *   <li>对文档未给出的 2025 正式字段，不胡乱硬编码</li>
 *   <li>在输出中显式标记 placeholder / compatibility 信息，避免误用为正式标准字段版</li>
 * </ul>
 */
public class BodyParser {

    private static final int CMD_VEHICLE_LOGIN = 0x01;
    private static final int CMD_REALTIME_REPORT = 0x02;
    private static final int CMD_REISSUE_REPORT = 0x03;
    private static final int CMD_VEHICLE_LOGOUT = 0x04;

    private static final int TYPE_VEHICLE_DATA = 0x01;
    private static final int TYPE_DRIVE_MOTOR_DATA = 0x02;
    private static final int TYPE_FUEL_CELL_DATA = 0x03;
    private static final int TYPE_ENGINE_DATA = 0x04;
    private static final int TYPE_LOCATION_DATA = 0x05;
    private static final int TYPE_EXTREME_DATA = 0x06;
    private static final int TYPE_ALARM_DATA = 0x07;

    public CommandBody parse(Packet32960 packet) {
        return switch (packet.cmd()) {
            case CMD_VEHICLE_LOGIN -> parseLogin(packet);
            case CMD_REALTIME_REPORT -> parseRealtime(packet);
            case CMD_REISSUE_REPORT -> parseReissue(packet);
            case CMD_VEHICLE_LOGOUT -> parseLogout(packet);
            default -> new UnknownCommandBody(packet.cmd(), packet.dataUnit());
        };
    }

    private CommandBody parseLogin(Packet32960 packet) {
        ByteReader br = new ByteReader(packet.dataUnit());
        Map<String, Object> fields = baseCommandFields(packet, "vehicle_login", false);
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
        Map<String, Object> fields = baseCommandFields(packet, "vehicle_logout", false);
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
        return new InfoItemsCommandBody("realtime_report", packet, parseInfoItems(packet.dataUnit()));
    }

    private CommandBody parseReissue(Packet32960 packet) {
        return new InfoItemsCommandBody("reissue_report", packet, parseInfoItems(packet.dataUnit()));
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
            case TYPE_VEHICLE_DATA -> parseVehicleData(br);
            case TYPE_DRIVE_MOTOR_DATA -> parseDriveMotorData(br);
            case TYPE_FUEL_CELL_DATA -> parseFuelCellData(br);
            case TYPE_ENGINE_DATA -> parseEngineData(br);
            case TYPE_LOCATION_DATA -> parseLocationData(br);
            case TYPE_EXTREME_DATA -> parseExtremeData(br);
            case TYPE_ALARM_DATA -> parseAlarmData(br);
            default -> throw new IllegalArgumentException("unknown info type=" + type);
        };
    }

    private InfoItem parseVehicleData(ByteReader br) {
        Map<String, Object> fields = newInfoFields("vehicle_data", TYPE_VEHICLE_DATA);
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
        return new SimpleInfoItem(TYPE_VEHICLE_DATA, "vehicle_data", fields);
    }

    private InfoItem parseDriveMotorData(ByteReader br) {
        Map<String, Object> fields = newInfoFields("drive_motor_data", TYPE_DRIVE_MOTOR_DATA);
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
        return new SimpleInfoItem(TYPE_DRIVE_MOTOR_DATA, "drive_motor_data", fields);
    }

    private InfoItem parseFuelCellData(ByteReader br) {
        Map<String, Object> fields = newInfoFields("fuel_cell_data", TYPE_FUEL_CELL_DATA);
        fields.put("stackVoltageRaw", br.readU16());
        fields.put("stackCurrentRaw", br.readU16());
        fields.put("fuelConsumptionRaw", br.readU16());
        if (br.remaining() > 0) {
            fields.put("rawTailHex", HexUtil.toHex(br.readRemaining()));
        }
        return new SimpleInfoItem(TYPE_FUEL_CELL_DATA, "fuel_cell_data", fields);
    }

    private InfoItem parseEngineData(ByteReader br) {
        Map<String, Object> fields = newInfoFields("engine_data", TYPE_ENGINE_DATA);
        fields.put("status", br.readU8());
        fields.put("crankshaftSpeed", br.readU16());
        fields.put("fuelConsumptionRate", br.readU16());
        return new SimpleInfoItem(TYPE_ENGINE_DATA, "engine_data", fields);
    }

    private InfoItem parseLocationData(ByteReader br) {
        Map<String, Object> fields = newInfoFields("location_data", TYPE_LOCATION_DATA);
        fields.put("positionStatus", br.readU8());
        long longitudeRaw = br.readU32();
        long latitudeRaw = br.readU32();
        fields.put("longitudeRaw", longitudeRaw);
        fields.put("longitude", longitudeRaw / 1_000_000.0);
        fields.put("latitudeRaw", latitudeRaw);
        fields.put("latitude", latitudeRaw / 1_000_000.0);
        return new SimpleInfoItem(TYPE_LOCATION_DATA, "location_data", fields);
    }

    private InfoItem parseExtremeData(ByteReader br) {
        Map<String, Object> fields = newInfoFields("extreme_data", TYPE_EXTREME_DATA);
        fields.put("maxVoltageSubsystemNo", br.readU8());
        fields.put("maxVoltageCellNo", br.readU8());
        fields.put("maxVoltageRaw", br.readU16());
        fields.put("minVoltageSubsystemNo", br.readU8());
        fields.put("minVoltageCellNo", br.readU8());
        fields.put("minVoltageRaw", br.readU16());
        fields.put("parserNote", "Extreme data is still a compatibility placeholder; refine against GB/T 32960.3-2025 official field table.");
        return new SimpleInfoItem(TYPE_EXTREME_DATA, "extreme_data", fields);
    }

    private InfoItem parseAlarmData(ByteReader br) {
        Map<String, Object> fields = newInfoFields("alarm_data", TYPE_ALARM_DATA);
        int alarmLevel = br.readU8();
        int thermalRunawayLevel = br.readU8();
        long systemAlarmBits = br.readU32();
        int faultCount = br.readU8();

        fields.put("alarmLevel", alarmLevel);
        fields.put("thermalRunawayLevel", thermalRunawayLevel);
        fields.put("systemAlarmBits", systemAlarmBits);
        fields.put("faultCount", faultCount);
        fields.put("thermalRunawayActive", thermalRunawayLevel > 0);

        List<Object> faults = new ArrayList<>();
        for (int i = 0; i < faultCount; i++) {
            faults.add(br.readU32());
        }
        fields.put("faultCodes", faults);

        Map<String, Object> alarmSummary = new LinkedHashMap<>();
        alarmSummary.put("severity", alarmSeverityName(alarmLevel));
        alarmSummary.put("thermalRunawaySeverity", alarmSeverityName(thermalRunawayLevel));
        alarmSummary.put("systemAlarmBitsHex", String.format("0x%08X", systemAlarmBits));
        fields.put("alarmSummary", alarmSummary);

        if (br.remaining() > 0) {
            fields.put("rawTailHex", HexUtil.toHex(br.readRemaining()));
        }
        return new SimpleInfoItem(TYPE_ALARM_DATA, "alarm_data", fields);
    }

    private Map<String, Object> baseCommandFields(Packet32960 packet, String commandName, boolean placeholder) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("protocolFamily", "GB/T 32960");
        fields.put("parserMode", "compatibility-skeleton");
        fields.put("commandName", commandName);
        fields.put("vin", packet.vin());
        fields.put("encryptFlag", packet.encryptFlag());
        fields.put("dataLen", packet.dataLen());
        fields.put("placeholder", placeholder);
        return fields;
    }

    private Map<String, Object> newInfoFields(String infoName, int type) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("infoName", infoName);
        fields.put("infoType", type);
        fields.put("parserMode", "compatibility-skeleton");
        return fields;
    }

    private String alarmSeverityName(int level) {
        return switch (level) {
            case 0 -> "none-or-normal";
            case 1 -> "level-1";
            case 2 -> "level-2";
            case 3 -> "level-3";
            default -> "reserved-or-unknown";
        };
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
        private final Packet32960 packet;
        private final List<InfoItem> items;

        private InfoItemsCommandBody(String name, Packet32960 packet, List<InfoItem> items) {
            this.name = name;
            this.packet = packet;
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
            map.put("protocolFamily", "GB/T 32960");
            map.put("parserMode", "compatibility-skeleton");
            map.put("vin", packet.vin());
            map.put("dataLen", packet.dataLen());
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
            map.put("parserMode", "compatibility-skeleton");
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
