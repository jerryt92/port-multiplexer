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
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

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
            tcpForwardConfig.setDefault((String) tcpForward.get("default"));
            tcpForwardConfig.setSsl((String) tcpForward.get("ssl"));
            tcpForwardConfig.setHttp((String) tcpForward.get("http"));
            tcpForwardConfig.setSsh((String) tcpForward.get("ssh"));
            tcpForwardConfig.setMqtt((String) tcpForward.get("mqtt"));
            tcpForwardConfig.setAllowedProtocols(new HashSet<>());
            for (String protocol : ((String) tcpForward.get("allowed")).split(",")) {
                protocol = protocol.trim();
                switch (protocol) {
                    case "ssl":
                        tcpForwardConfig.getAllowedProtocols().add(TcpProtocolType.SSL_TLS);
                        break;
                    case "http":
                        tcpForwardConfig.getAllowedProtocols().add(TcpProtocolType.HTTP);
                        break;
                    case "mqtt":
                        tcpForwardConfig.getAllowedProtocols().add(TcpProtocolType.MQTT);
                        break;
                    case "ssh":
                        tcpForwardConfig.getAllowedProtocols().add(TcpProtocolType.SSH);
                        break;
                    case "default":
                        tcpForwardConfig.getAllowedProtocols().add(TcpProtocolType.UNKNOWN);
                        break;
                }
            }
            config.setTcpForwardConfig(tcpForwardConfig);
            // UDP
            Map<String, Object> udpConfigMap = (Map<String, Object>) obj.get("udp");
            Map<String, Object> udpBind = (Map<String, Object>) udpConfigMap.get("bind");
            Map<String, Object> udpForward = (Map<String, Object>) udpConfigMap.get("forward");
            config.setUdpEnabled((Boolean) udpConfigMap.get("enabled"));
            bindConfig.setUdpHost(InetAddress.getByName(StringUtils.defaultIfBlank((String) udpBind.get("host"), "0.0.0.0")));
            bindConfig.setUdpPort((Integer) udpBind.get("port"));
            UdpForwardConfig udpForwardConfig = new UdpForwardConfig();
            udpForwardConfig.setSnmp((String) udpForward.get("snmp"));
            udpForwardConfig.setDefault((String) udpForward.get("default"));
            udpForwardConfig.setAllowedProtocols(new HashSet<>());
            for (String protocol : ((String) udpForward.get("allowed")).split(",")) {
                protocol = protocol.trim();
                switch (protocol) {
                    case "snmp":
                        udpForwardConfig.getAllowedProtocols().add(UdpProtocolType.SNMP);
                        break;
                    case "default":
                        udpForwardConfig.getAllowedProtocols().add(UdpProtocolType.UNKNOWN);
                        break;
                }
            }
            config.setUdpForwardConfig(udpForwardConfig);
        } catch (Exception e) {
            log.error("Failed to read configuration file", e);
            throw new RuntimeException("Failed to read configuration file", e);
        }
        return config;
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
        private String defaultAddress;
        private String ssl;
        private String http;
        private String ssh;
        private String mqtt;

        public String getDefault() {
            return defaultAddress;
        }

        public void setDefault(String defaultAddress) {
            this.defaultAddress = defaultAddress;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            TcpForwardConfig that = (TcpForwardConfig) o;
            return Objects.equals(allowedProtocols, that.allowedProtocols) && Objects.equals(defaultAddress, that.defaultAddress) && Objects.equals(ssl, that.ssl) && Objects.equals(http, that.http) && Objects.equals(ssh, that.ssh) && Objects.equals(mqtt, that.mqtt);
        }
    }

    @Data
    public static class UdpForwardConfig {
        private HashSet<UdpProtocolType> allowedProtocols;
        private String defaultAddress;
        private String snmp;

        public String getDefault() {
            return defaultAddress;
        }

        public void setDefault(String defaultAddress) {
            this.defaultAddress = defaultAddress;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            UdpForwardConfig that = (UdpForwardConfig) o;
            return Objects.equals(allowedProtocols, that.allowedProtocols) && Objects.equals(defaultAddress, that.defaultAddress) && Objects.equals(snmp, that.snmp);
        }
    }
}