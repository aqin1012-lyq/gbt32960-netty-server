package demo.gbt32960.model;

import java.util.Map;

public interface InfoItem {
    int type();
    Map<String, Object> toMap();
}
