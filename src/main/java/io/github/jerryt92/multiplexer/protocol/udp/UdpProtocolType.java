package io.github.jerryt92.multiplexer.protocol.udp;

import java.util.HashMap;
import java.util.Map;

public enum UdpProtocolType {
    UNKNOWN("default"),
    SNMP("snmp");

    private static final Map<String, UdpProtocolType> LOOKUP = new HashMap<>();

    static {
        for (UdpProtocolType type : UdpProtocolType.values()) {
            LOOKUP.put(type.configKey, type);
        }
    }

    private final String configKey;

    UdpProtocolType(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigKey() {
        return configKey;
    }

    public static UdpProtocolType fromConfigKey(String key) {
        if (key == null) {
            return null;
        }
        return LOOKUP.get(key.trim().toLowerCase());
    }
}
