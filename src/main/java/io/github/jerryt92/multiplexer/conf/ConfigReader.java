package io.github.jerryt92.multiplexer.conf;

import io.github.jerryt92.multiplexer.PortMultiplexer;
import io.github.jerryt92.multiplexer.protocol.tcp.TcpProtocolType;
import io.github.jerryt92.multiplexer.protocol.udp.UdpProtocolType;
import lombok.Data;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Map;

public class ConfigReader {
    private static final Logger log = LogManager.getLogger(ConfigReader.class);
    public static final ConfigReader INSTANCE = new ConfigReader();
    @Getter
    private final AppConfig appConfig = new AppConfig();

    private ConfigReader() {
        ClassLoader classLoader = PortMultiplexer.class.getClassLoader();
        Yaml yaml = new Yaml();
        try (InputStream inputStream = classLoader.getResourceAsStream("conf.yaml")) {
            Map<String, Object> obj = yaml.load(inputStream);
            BindConfig bindConfig = new BindConfig();
            appConfig.setBindConfig(bindConfig);
            // TCP
            Map<String, Object> tcpConfigMap = (Map<String, Object>) obj.get("tcp");
            Map<String, Object> tcpBind = (Map<String, Object>) tcpConfigMap.get("bind");
            Map<String, Object> tcpForward = (Map<String, Object>) tcpConfigMap.get("forward");
            appConfig.setTcpEnabled((Boolean) tcpConfigMap.get("enabled"));
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
            appConfig.setTcpForwardConfig(tcpForwardConfig);
            // UDP
            Map<String, Object> udpConfigMap = (Map<String, Object>) obj.get("udp");
            Map<String, Object> udpBind = (Map<String, Object>) udpConfigMap.get("bind");
            Map<String, Object> udpForward = (Map<String, Object>) udpConfigMap.get("forward");
            appConfig.setUdpEnabled((Boolean) udpConfigMap.get("enabled"));
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
            appConfig.setUdpForwardConfig(udpForwardConfig);
        } catch (Exception e) {
            log.error("Failed to read configuration file", e);
            throw new RuntimeException("Failed to read configuration file", e);
        }
    }

    @Data
    public static class AppConfig {
        private BindConfig bindConfig;
        private boolean tcpEnabled = false;
        private TcpForwardConfig tcpForwardConfig;
        private boolean udpEnabled = false;
        private UdpForwardConfig udpForwardConfig;
    }

    @Data
    public static class BindConfig {
        private InetAddress tcpHost;
        private int tcpPort;
        private InetAddress udpHost;
        private int udpPort;
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
    }
}