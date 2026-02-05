package io.github.jerryt92.multiplexer.protocol.tcp;

import java.util.HashMap;
import java.util.Map;

public enum TcpProtocolType {
    UNKNOWN("default", null),
    SSL_TLS("ssl", null),
    HTTP("http", null),
    WEBSOCKET("websocket", HTTP),
    MQTT("mqtt", null),
    SSH("ssh", null),
    RDP("rdp", null);

    private static final Map<String, TcpProtocolType> LOOKUP = new HashMap<>();

    static {
        for (TcpProtocolType type : TcpProtocolType.values()) {
            LOOKUP.put(type.configKey, type);
        }
        LOOKUP.put("tls", SSL_TLS);
        LOOKUP.put("websocket", WEBSOCKET);
        LOOKUP.put("ws", WEBSOCKET);
    }

    private final String configKey;
    private final TcpProtocolType fallbackProtocol;

    TcpProtocolType(String configKey, TcpProtocolType fallbackProtocol) {
        this.configKey = configKey;
        this.fallbackProtocol = fallbackProtocol;
    }

    public String getConfigKey() {
        return configKey;
    }

    public TcpProtocolType getFallbackProtocol() {
        return fallbackProtocol;
    }

    public static TcpProtocolType fromConfigKey(String key) {
        if (key == null) {
            return null;
        }
        return LOOKUP.get(key.trim().toLowerCase());
    }
}
