package io.github.jerryt92.multiplexer.forward.tcp;

import io.github.jerryt92.multiplexer.protocol.tcp.TcpProtocolType;
import io.github.jerryt92.multiplexer.conf.ConfigService;
import io.github.jerryt92.multiplexer.entity.ForwardTarget;
import io.github.jerryt92.multiplexer.protocol.tcp.TcpProtocolDetection;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @Date: 2025/4/17
 * @Author: jerryt92
 */
public class TcpForwardRule {
    private static final Logger log = LogManager.getLogger(TcpForwardRule.class);

    public ForwardTarget getRoute(ChannelHandlerContext ctx, Object msg) {
        ConfigService.TcpForwardConfig forwardConfig = ConfigService.INSTANCE.getAppConfig().getTcpForwardConfig();
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
            if (forwardConfig.getAllowedProtocols().contains(protocol)) {
                String target = resolveTarget(forwardConfig, protocol);
                ForwardTarget resolved = buildTarget(target);
                route = resolved != null ? resolved : new ForwardTarget().setReject(true);
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

    private String resolveTarget(ConfigService.TcpForwardConfig config, TcpProtocolType protocol) {
        String target = config.getTargets().get(protocol);
        if (StringUtils.isBlank(target) && protocol.getFallbackProtocol() != null) {
            target = config.getTargets().get(protocol.getFallbackProtocol());
        }
        if (StringUtils.isBlank(target)) {
            target = config.getTargets().get(TcpProtocolType.UNKNOWN);
        }
        return target;
    }

    private ForwardTarget buildTarget(String target) {
        if (StringUtils.isBlank(target)) {
            return null;
        }
        String[] parts = target.trim().split(":");
        if (parts.length != 2) {
            return null;
        }
        int port = Integer.parseInt(parts[1]);
        return new ForwardTarget().setHost(parts[0]).setPort(port);
    }
}
