package demo.gbt32960.util;

import java.util.List;
import java.util.Map;

public final class JsonUtil {

    private JsonUtil() {
    }

    public static String toPrettyJson(Object obj) {
        StringBuilder sb = new StringBuilder();
        write(obj, sb, 0);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static void write(Object obj, StringBuilder sb, int indent) {
        if (obj == null) {
            sb.append("null");
            return;
        }
        if (obj instanceof String s) {
            sb.append('"').append(escape(s)).append('"');
            return;
        }
        if (obj instanceof Number || obj instanceof Boolean) {
            sb.append(obj);
            return;
        }
        if (obj instanceof byte[] bytes) {
            sb.append('"').append(HexUtil.toHex(bytes)).append('"');
            return;
        }
        if (obj instanceof Map<?, ?> rawMap) {
            Map<String, Object> map = (Map<String, Object>) rawMap;
            sb.append("{\n");
            int i = 0;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                indent(sb, indent + 2);
                sb.append('"').append(escape(entry.getKey())).append("\": ");
                write(entry.getValue(), sb, indent + 2);
                if (i < map.size() - 1) {
                    sb.append(',');
                }
                sb.append('\n');
                i++;
            }
            indent(sb, indent);
            sb.append('}');
            return;
        }
        if (obj instanceof List<?> rawList) {
            List<Object> list = (List<Object>) rawList;
            sb.append("[\n");
            for (int i = 0; i < list.size(); i++) {
                indent(sb, indent + 2);
                write(list.get(i), sb, indent + 2);
                if (i < list.size() - 1) {
                    sb.append(',');
                }
                sb.append('\n');
            }
            indent(sb, indent);
            sb.append(']');
            return;
        }
        sb.append('"').append(escape(String.valueOf(obj))).append('"');
    }

    private static void indent(StringBuilder sb, int n) {
        for (int i = 0; i < n; i++) {
            sb.append(' ');
        }
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
