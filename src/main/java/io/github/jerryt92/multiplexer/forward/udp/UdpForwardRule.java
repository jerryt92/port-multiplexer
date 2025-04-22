package io.github.jerryt92.multiplexer.forward.udp;

import io.github.jerryt92.multiplexer.conf.ConfigReader;
import io.github.jerryt92.multiplexer.entity.ForwardTarget;
import io.github.jerryt92.multiplexer.protocol.udp.UdpProtocolDetection;
import io.github.jerryt92.multiplexer.protocol.udp.UdpProtocolType;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @Date: 2025/4/17
 * @Author: jerryt92
 */
public class UdpForwardRule {
    private static final Logger log = LogManager.getLogger(UdpForwardRule.class);

    public ForwardTarget getRoute(ChannelHandlerContext ctx, DatagramPacket msg) {
        ConfigReader.UdpForwardConfig forwardConfig = ConfigReader.INSTANCE.getAppConfig().getUdpForwardConfig();
        try {
            // Get route from cache
            ForwardTarget route = UdpChannelCache.getChannelRouteCache().get(ctx.channel());
            if (route != null) {
                return route;
            }
            // 识别第一个数据包协议，获取对应的路由策略
            // Detect first packet protocol to get corresponding routing strategy
            UdpProtocolType protocol = UdpProtocolDetection.detectProtocol(msg);
            String address;
            int port;
            if (forwardConfig.getEnableProtocols().contains(protocol)) {
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
            UdpChannelCache.getChannelRouteCache().put(ctx.channel(), route);
            log.debug("Src address: {}", ctx.channel().remoteAddress());
            log.debug("Dst address: {}", ctx.channel().localAddress());
            log.debug("Protocol: {}", protocol);
            return route;
        } catch (Exception e) {
            log.error("", e);
            return null;
        }
    }
}
