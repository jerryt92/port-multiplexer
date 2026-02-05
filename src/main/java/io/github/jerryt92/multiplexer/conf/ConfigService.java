package io.github.jerryt92.multiplexer.conf;

import io.github.jerryt92.multiplexer.protocol.tcp.TcpProtocolType;
import io.github.jerryt92.multiplexer.protocol.udp.UdpProtocolType;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class ConfigService {
    private static final Logger log = LogManager.getLogger(ConfigService.class);
    public static final ConfigService INSTANCE = new ConfigService();
    @Getter
    @Setter
    private AppConfig appConfig;

    private ConfigService() {
        appConfig = readConfig();
    }

    public AppConfig readConfig() {
        Yaml yaml = new Yaml();
        AppConfig config = new AppConfig();
        try (InputStream inputStream = ConfigService.class.getClassLoader().getResourceAsStream("conf.yaml")) {
            Map<String, Object> obj = yaml.load(inputStream);
            BindConfig bindConfig = new BindConfig();
            config.setBindConfig(bindConfig);
            // TCP
            Map<String, Object> tcpConfigMap = (Map<String, Object>) obj.get("tcp");
            Map<String, Object> tcpBind = (Map<String, Object>) tcpConfigMap.get("bind");
            Map<String, Object> tcpForward = (Map<String, Object>) tcpConfigMap.get("forward");
            config.setTcpEnabled((Boolean) tcpConfigMap.get("enabled"));
            bindConfig.setTcpHost(InetAddress.getByName(StringUtils.defaultIfBlank((String) tcpBind.get("host"), "0.0.0.0")));
            bindConfig.setTcpPort((Integer) tcpBind.get("port"));
            TcpForwardConfig tcpForwardConfig = new TcpForwardConfig();
            tcpForwardConfig.setAllowedProtocols(parseAllowedProtocols(tcpForward.get("allowed"), TcpProtocolType::fromConfigKey));
            populateTargets(tcpForwardConfig, tcpForward, TcpProtocolType::fromConfigKey);
            config.setTcpForwardConfig(tcpForwardConfig);
            // UDP
            Map<String, Object> udpConfigMap = (Map<String, Object>) obj.get("udp");
            Map<String, Object> udpBind = (Map<String, Object>) udpConfigMap.get("bind");
            Map<String, Object> udpForward = (Map<String, Object>) udpConfigMap.get("forward");
            config.setUdpEnabled((Boolean) udpConfigMap.get("enabled"));
            bindConfig.setUdpHost(InetAddress.getByName(StringUtils.defaultIfBlank((String) udpBind.get("host"), "0.0.0.0")));
            bindConfig.setUdpPort((Integer) udpBind.get("port"));
            UdpForwardConfig udpForwardConfig = new UdpForwardConfig();
            udpForwardConfig.setAllowedProtocols(parseAllowedProtocols(udpForward.get("allowed"), UdpProtocolType::fromConfigKey));
            populateTargets(udpForwardConfig, udpForward, UdpProtocolType::fromConfigKey);
            config.setUdpForwardConfig(udpForwardConfig);
        } catch (Exception e) {
            log.error("Failed to read configuration file", e);
            throw new RuntimeException("Failed to read configuration file", e);
        }
        return config;
    }

    private static <T> HashSet<T> parseAllowedProtocols(Object rawAllowed, Function<String, T> mapper) {
        HashSet<T> result = new HashSet<>();
        if (rawAllowed == null) {
            return result;
        }
        if (rawAllowed instanceof String) {
            for (String token : ((String) rawAllowed).split(",")) {
                addAllowedToken(result, mapper, token);
            }
        } else if (rawAllowed instanceof List) {
            for (Object token : (List<?>) rawAllowed) {
                addAllowedToken(result, mapper, String.valueOf(token));
            }
        } else {
            addAllowedToken(result, mapper, String.valueOf(rawAllowed));
        }
        return result;
    }

    private static <T> void addAllowedToken(HashSet<T> result, Function<String, T> mapper, String token) {
        if (StringUtils.isBlank(token)) {
            return;
        }
        T protocol = mapper.apply(token.trim());
        if (protocol != null) {
            result.add(protocol);
        }
    }

    private static String parseTargetValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof Map) {
            Map<?, ?> mapValue = (Map<?, ?>) value;
            if (mapValue.isEmpty()) {
                return null;
            }
            Object key = mapValue.keySet().iterator().next();
            return String.valueOf(key);
        }
        return String.valueOf(value);
    }

    private static void populateTargets(TcpForwardConfig config, Map<String, Object> forward, Function<String, TcpProtocolType> mapper) {
        config.setTargets(new HashMap<>());
        for (Map.Entry<String, Object> entry : forward.entrySet()) {
            String key = entry.getKey();
            if ("allowed".equalsIgnoreCase(key)) {
                continue;
            }
            TcpProtocolType protocol = mapper.apply(key);
            if (protocol == null) {
                continue;
            }
            String target = parseTargetValue(entry.getValue());
            if (StringUtils.isNotBlank(target)) {
                config.getTargets().put(protocol, target.trim());
            }
        }
    }

    private static void populateTargets(UdpForwardConfig config, Map<String, Object> forward, Function<String, UdpProtocolType> mapper) {
        config.setTargets(new HashMap<>());
        for (Map.Entry<String, Object> entry : forward.entrySet()) {
            String key = entry.getKey();
            if ("allowed".equalsIgnoreCase(key)) {
                continue;
            }
            UdpProtocolType protocol = mapper.apply(key);
            if (protocol == null) {
                continue;
            }
            String target = parseTargetValue(entry.getValue());
            if (StringUtils.isNotBlank(target)) {
                config.getTargets().put(protocol, target.trim());
            }
        }
    }

    @Data
    public static class AppConfig {
        private BindConfig bindConfig;
        private boolean tcpEnabled = false;
        private TcpForwardConfig tcpForwardConfig;
        private boolean udpEnabled = false;
        private UdpForwardConfig udpForwardConfig;

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            AppConfig config = (AppConfig) o;
            return tcpEnabled == config.tcpEnabled && udpEnabled == config.udpEnabled && Objects.equals(bindConfig, config.bindConfig) && Objects.equals(tcpForwardConfig, config.tcpForwardConfig) && Objects.equals(udpForwardConfig, config.udpForwardConfig);
        }
    }

    @Data
    public static class BindConfig {
        private InetAddress tcpHost;
        private int tcpPort;
        private InetAddress udpHost;
        private int udpPort;

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            BindConfig that = (BindConfig) o;
            return tcpPort == that.tcpPort && udpPort == that.udpPort && Objects.equals(tcpHost, that.tcpHost) && Objects.equals(udpHost, that.udpHost);
        }
    }

    @Data
    public static class TcpForwardConfig {
        private HashSet<TcpProtocolType> allowedProtocols;
        private Map<TcpProtocolType, String> targets;

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            TcpForwardConfig that = (TcpForwardConfig) o;
            return Objects.equals(allowedProtocols, that.allowedProtocols) && Objects.equals(targets, that.targets);
        }
    }

    @Data
    public static class UdpForwardConfig {
        private HashSet<UdpProtocolType> allowedProtocols;
        private Map<UdpProtocolType, String> targets;

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            UdpForwardConfig that = (UdpForwardConfig) o;
            return Objects.equals(allowedProtocols, that.allowedProtocols) && Objects.equals(targets, that.targets);
        }
    }
}