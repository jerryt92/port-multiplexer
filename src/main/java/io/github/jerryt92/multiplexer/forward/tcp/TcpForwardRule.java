package io.github.jerryt92.multiplexer.forward.tcp;

import io.github.jerryt92.multiplexer.protocol.tcp.TcpProtocolType;
import io.github.jerryt92.multiplexer.conf.ConfigReader;
import io.github.jerryt92.multiplexer.entity.ForwardTarget;
import io.github.jerryt92.multiplexer.protocol.tcp.TcpProtocolDetection;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @Date: 2025/4/17
 * @Author: jerryt92
 */
public class TcpForwardRule {
    private static final Logger log = LogManager.getLogger(TcpForwardRule.class);

    public ForwardTarget getRoute(ChannelHandlerContext ctx, Object msg) {
        ConfigReader.TcpForwardConfig forwardConfig = ConfigReader.INSTANCE.getAppConfig().getTcpForwardConfig();
        try {
            ByteBuf msgByteBuf = (ByteBuf) msg;
            // Get route from cache
            ForwardTarget route = TcpChannelCache.getChannelRouteCache().get(ctx.channel());
            if (route != null) {
                return route;
            }
            // 识别第一个数据包协议，获取对应的路由策略
            // Detect first packet protocol to get corresponding routing strategy
            TcpProtocolType protocol = TcpProtocolDetection.detectProtocol(msgByteBuf);
            String address;
            int port;
            if (forwardConfig.getAllowedProtocols().contains(protocol)) {
                switch (protocol) {
                    case SSL_TLS:
                        address = forwardConfig.getSsl().split(":")[0];
                        port = Integer.parseInt(forwardConfig.getSsl().split(":")[1]);
                        break;
                    case HTTP:
                    case WEBSOCKET:
                        address = forwardConfig.getHttp().split(":")[0];
                        port = Integer.parseInt(forwardConfig.getHttp().split(":")[1]);
                        break;
                    case SSH:
                        address = forwardConfig.getSsh().split(":")[0];
                        port = Integer.parseInt(forwardConfig.getSsh().split(":")[1]);
                        break;
                    case MQTT:
                        address = forwardConfig.getMqtt().split(":")[0];
                        port = Integer.parseInt(forwardConfig.getMqtt().split(":")[1]);
                        break;
                    default:
                        address = forwardConfig.getDefault().split(":")[0];
                        port = Integer.parseInt(forwardConfig.getDefault().split(":")[1]);
                }
                route = new ForwardTarget().setHost(address).setPort(port);
            } else {
                route = new ForwardTarget().setReject(true);
            }
            TcpChannelCache.getChannelRouteCache().put(ctx.channel(), route);
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
