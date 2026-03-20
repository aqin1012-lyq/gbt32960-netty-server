package demo.gbt32960.model;

import demo.gbt32960.util.HexUtil;
import demo.gbt32960.util.JsonUtil;

import java.util.LinkedHashMap;
import java.util.Map;

public record Packet32960(
        int cmd,
        int ack,
        String vin,
        int encryptFlag,
        int dataLen,
        byte[] dataUnit,
        int checksum
) {
    public String toPrettyJson() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("cmd", cmd);
        map.put("ack", ack);
        map.put("vin", vin);
        map.put("encryptFlag", encryptFlag);
        map.put("dataLen", dataLen);
        map.put("checksum", checksum);
        map.put("dataUnitHex", HexUtil.toHex(dataUnit));
        return JsonUtil.toPrettyJson(map);
    }
}
