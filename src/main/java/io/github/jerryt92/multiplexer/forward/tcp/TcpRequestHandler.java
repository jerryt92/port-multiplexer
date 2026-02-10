package io.github.jerryt92.multiplexer.forward.tcp;

import io.github.jerryt92.multiplexer.entity.ForwardTarget;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.ReferenceCountUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @Date: 2025/4/17
 * @Author: jerryt92
 */
public class TcpRequestHandler extends ChannelInboundHandlerAdapter {
    private static final Logger log = LogManager.getLogger(TcpRequestHandler.class);
    private static final TcpForwardRule TCP_FORWARD_RULE = new TcpForwardRule();
    private final EventLoopGroup workerGroup;

    public TcpRequestHandler(EventLoopGroup workerGroup) {
        this.workerGroup = workerGroup;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {
        try {
            ForwardTarget forwardTarget = TCP_FORWARD_RULE.getRoute(ctx, msg);
            if (forwardTarget == null || forwardTarget.isReject()) {
                ctx.channel().close();
                return;
            }
            TcpChannelCache.OutboundState existing = TcpChannelCache.getChannelClientCache().get(ctx.channel());
            if (existing != null) {
                if (existing.getOutbound() != null && existing.getOutbound().isActive()) {
                    existing.getOutbound().writeAndFlush(msg);
                    return;
                }
                if (existing.getConnectFuture() != null && !existing.getConnectFuture().isDone()) {
                    existing.getPending().add(ReferenceCountUtil.retain(msg));
                    return;
                }
                cleanupState(existing);
                TcpChannelCache.getChannelClientCache().remove(ctx.channel());
            }
            Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            b.handler(new TcpResponseHandler(ctx.channel()));
            TcpChannelCache.OutboundState state = new TcpChannelCache.OutboundState();
            state.getPending().add(ReferenceCountUtil.retain(msg));
            TcpChannelCache.getChannelClientCache().put(ctx.channel(), state);
            ctx.channel().config().setAutoRead(false);
            ChannelFuture f = b.connect(forwardTarget.getHost(), forwardTarget.getPort());
            state.setConnectFuture(f);
            f.addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    Channel channel = future.channel();
                    state.setOutbound(channel);
                    Object pendingMsg;
                    while ((pendingMsg = state.getPending().poll()) != null) {
                        channel.write(pendingMsg);
                    }
                    channel.flush();
                    ctx.channel().config().setAutoRead(true);
                    ctx.read();
                } else {
                    cleanupState(state);
                    TcpChannelCache.getChannelClientCache().remove(ctx.channel());
                    exceptionCaught(ctx, future.cause());
                }
            });
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
        TcpChannelCache.OutboundState state = TcpChannelCache.getChannelClientCache().remove(ctx.channel());
        if (state != null) {
            cleanupState(state);
        }
        super.channelInactive(ctx);
    }

    private void cleanupState(TcpChannelCache.OutboundState state) {
        if (state.getOutbound() != null) {
            state.getOutbound().close();
        }
        Object pendingMsg;
        while ((pendingMsg = state.getPending().poll()) != null) {
            ReferenceCountUtil.release(pendingMsg);
        }
    }
}
