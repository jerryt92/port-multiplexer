package io.github.jerryt92.multiplexer.forward.tcp;

import io.github.jerryt92.multiplexer.entity.ForwardTarget;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @Date: 2025/4/17
 * @Author: jerryt92
 */
public class TcpRequestHandler extends ChannelInboundHandlerAdapter {
    private static final Logger log = LogManager.getLogger(TcpRequestHandler.class);
    private final TcpForwardRule tcpForwardRule;
    private final EventLoopGroup workerGroup;

    public TcpRequestHandler(EventLoopGroup workerGroup) {
        this.workerGroup = workerGroup;
        this.tcpForwardRule = new TcpForwardRule();
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {
        try {
            ForwardTarget forwardTarget = tcpForwardRule.getRoute(ctx, msg);
            if (forwardTarget == null || forwardTarget.isReject()) {
                ctx.channel().close();
                return;
            }
            if (TcpChannelCache.getChannelClientCache().containsKey(ctx.channel())) {
                Channel channel = TcpChannelCache.getChannelClientCache().get(ctx.channel());
                if (channel != null && channel.isActive()) {
                    channel.writeAndFlush(msg);
                    return;
                }
                if (channel != null && !channel.isActive()) {
                    // 显式关闭连接
                    channel.close();
                }
                TcpChannelCache.getChannelClientCache().remove(ctx.channel());
            }
            Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            b.handler(new TcpResponseHandler(ctx.channel()));
            ChannelFuture f = b.connect(forwardTarget.getHost(), forwardTarget.getPort()).sync();
            Channel channel = f.channel();
            TcpChannelCache.getChannelClientCache().put(ctx.channel(), channel);
            channel.writeAndFlush(msg);
        } catch (Exception e) {
            exceptionCaught(ctx, e);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("", cause);
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        TcpChannelCache.getChannelClientCache().remove(ctx.channel());
        TcpChannelCache.getChannelRouteCache().remove(ctx.channel());
        super.channelInactive(ctx);
    }
}
