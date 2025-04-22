package io.github.jerryt92.multiplexer.forward.udp;

import io.github.jerryt92.multiplexer.entity.ForwardTarget;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @Date: 2025/4/17
 * @Author: jerryt92
 */
public class UdpRequestHandler extends ChannelInboundHandlerAdapter {
    private static final Logger log = LogManager.getLogger(UdpRequestHandler.class);
    private final UdpForwardRule udpForwardRule;
    private final EventLoopGroup workerGroup;

    public UdpRequestHandler(EventLoopGroup workerGroup) {
        this.workerGroup = workerGroup;
        this.udpForwardRule = new UdpForwardRule();
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {
        try {
            DatagramPacket msgPacket = (DatagramPacket) msg;
            ForwardTarget forwardTarget = udpForwardRule.getRoute(ctx, msgPacket);
            if (forwardTarget == null || forwardTarget.isReject()) {
                ctx.channel().close();
                return;
            }
            if (UdpChannelCache.getChannelClientCache().containsKey(msgPacket.sender())) {
                Channel forwardChannel = UdpChannelCache.getChannelClientCache().get(msgPacket.sender());
                if (forwardChannel != null && forwardChannel.isActive()) {
                    forwardChannel.writeAndFlush(msgPacket.content());
                    return;
                }
                UdpChannelCache.getChannelClientCache().remove(msgPacket.sender());
            }
            Bootstrap b = new Bootstrap();
            b.option(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(16384));
            b.group(workerGroup);
            b.channel(NioDatagramChannel.class);
            b.handler(new UdpResponseHandler(ctx.channel(), msgPacket.sender()));
            ChannelFuture f = b.connect(forwardTarget.getHost(), forwardTarget.getPort()).sync();
            Channel forwardChannel = f.channel();
            UdpChannelCache.getChannelClientCache().put(msgPacket.sender(), forwardChannel);
            forwardChannel.writeAndFlush(msgPacket.content());
        } catch (Exception e) {
            exceptionCaught(ctx, e);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("", cause);
        closeOnFlush(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        UdpChannelCache.getChannelClientCache().remove(ctx.channel());
        UdpChannelCache.getChannelRouteCache().remove(ctx.channel());
        super.channelInactive(ctx);
    }

    /**
     * Closes the specified channel after all queued write requests are flushed.
     */
    static void closeOnFlush(Channel ch) {
        if (ch.isActive()) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
