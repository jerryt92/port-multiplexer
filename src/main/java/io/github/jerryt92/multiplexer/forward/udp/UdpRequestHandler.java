package io.github.jerryt92.multiplexer.forward.udp;

import io.github.jerryt92.multiplexer.entity.ForwardTarget;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.ReferenceCountUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;

/**
 * @Date: 2025/4/17
 * @Author: jerryt92
 */
public class UdpRequestHandler extends ChannelInboundHandlerAdapter {
    private static final Logger log = LogManager.getLogger(UdpRequestHandler.class);
    private static final UdpForwardRule UDP_FORWARD_RULE = new UdpForwardRule();
    private final EventLoopGroup workerGroup;

    public UdpRequestHandler(EventLoopGroup workerGroup) {
        this.workerGroup = workerGroup;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {
        try {
            DatagramPacket msgPacket = (DatagramPacket) msg;
            ForwardTarget forwardTarget = UDP_FORWARD_RULE.getRoute(msgPacket);
            if (forwardTarget == null || forwardTarget.isReject()) {
                // Drop invalid or rejected packets without closing the listening channel
                return;
            }
            final InetSocketAddress sender = msgPacket.sender();
            Channel forwardChannel = UdpChannelCache.getChannelClientCache().get(sender);
            if (forwardChannel != null && forwardChannel.isActive()) {
                // Reuse existing outbound channel
                forwardChannel.writeAndFlush(msgPacket.content().retain());
                return;
            }
            if (forwardChannel != null) {
                UdpChannelCache.getChannelClientCache().remove(sender);
            }
            Bootstrap b = new Bootstrap();
            b.option(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(16384));
            b.group(workerGroup);
            b.channel(NioDatagramChannel.class);
            b.handler(new UdpResponseHandler(ctx.channel(), sender));
            final ByteBuf payload = msgPacket.content().retain();
            ChannelFuture f = b.connect(forwardTarget.getHost(), forwardTarget.getPort());
            f.addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    Channel newChannel = future.channel();
                    UdpChannelCache.getChannelClientCache().put(sender, newChannel);
                    newChannel.writeAndFlush(payload);
                } else {
                    ReferenceCountUtil.release(payload);
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
}
