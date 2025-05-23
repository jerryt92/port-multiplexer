package io.github.jerryt92.multiplexer.forward.udp;

import io.github.jerryt92.multiplexer.conf.ConfigService;
import io.github.jerryt92.multiplexer.entity.ForwardTarget;
import io.github.jerryt92.multiplexer.protocol.udp.UdpProtocolDetection;
import io.github.jerryt92.multiplexer.protocol.udp.UdpProtocolType;
import io.netty.channel.socket.DatagramPacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @Date: 2025/4/17
 * @Author: jerryt92
 */
public class UdpForwardRule {
    private static final Logger log = LogManager.getLogger(UdpForwardRule.class);

    public ForwardTarget getRoute(DatagramPacket msg) {
        ConfigService.UdpForwardConfig forwardConfig = ConfigService.INSTANCE.getAppConfig().getUdpForwardConfig();
        try {
            // Get route from cache
            ForwardTarget route = UdpChannelCache.getChannelRouteCache().get(msg.sender());
            if (route != null) {
                return route;
            }
            // 识别第一个数据包协议，获取对应的路由策略
            // Detect first packet protocol to get corresponding routing strategy
            UdpProtocolType protocol = UdpProtocolDetection.detectProtocol(msg);
            String address;
            int port;
            if (forwardConfig.getAllowedProtocols().contains(protocol)) {
                switch (protocol) {
                    case SNMP:
                        address = forwardConfig.getSnmp().split(":")[0];
                        port = Integer.parseInt(forwardConfig.getSnmp().split(":")[1]);
                        break;
                    default:
                        address = forwardConfig.getDefault().split(":")[0];
                        port = Integer.parseInt(forwardConfig.getDefault().split(":")[1]);
                }
                route = new ForwardTarget().setHost(address).setPort(port);
            } else {
                route = new ForwardTarget().setReject(true);
            }
            UdpChannelCache.getChannelRouteCache().put(msg.sender(), route);
            log.debug("Src address: {}", msg.sender());
            log.debug("Protocol: {}", protocol);
            return route;
        } catch (Exception e) {
            log.error("", e);
            return null;
        }
    }
}
