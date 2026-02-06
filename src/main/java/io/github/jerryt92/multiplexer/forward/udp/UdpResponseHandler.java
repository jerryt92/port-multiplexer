package io.github.jerryt92.multiplexer.forward.udp;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class UdpResponseHandler extends ChannelInboundHandlerAdapter {
    private static final Logger log = LogManager.getLogger(UdpResponseHandler.class);
    public static final Long CHANNEL_IDLE_TIMEOUT_MINUTE = 10L;
    private final Channel inboundChannel;
    private final InetSocketAddress srcSocketAddress;
    private ScheduledFuture<?> closeFuture;

    public UdpResponseHandler(Channel inboundChannel, InetSocketAddress srcSocketAddress) {
        this.inboundChannel = inboundChannel;
        this.srcSocketAddress = srcSocketAddress;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        // schedule automatic close after idle timeout
        resetIdleTimer(ctx);
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {
        // refresh idle timer on each response
        resetIdleTimer(ctx);
        DatagramPacket responsePacket = (DatagramPacket) msg;
        // 使用 DatagramPacket 指定目标地址
        DatagramPacket outboundPacket = new DatagramPacket(responsePacket.content().retain(), srcSocketAddress);
        inboundChannel.writeAndFlush(outboundPacket);
    }

    private void resetIdleTimer(ChannelHandlerContext ctx) {
        if (closeFuture != null && !closeFuture.isDone()) {
            closeFuture.cancel(false);
        }
        closeFuture = ctx.executor().schedule(() -> {
            if (ctx.channel().isOpen()) {
                ctx.channel().close();
            }
        }, CHANNEL_IDLE_TIMEOUT_MINUTE, TimeUnit.MINUTES);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("", cause);
        if (inboundChannel.isActive()) {
            inboundChannel.close();
        }
    }

    // 关闭连接
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (closeFuture != null && !closeFuture.isDone()) {
            closeFuture.cancel(false);
        }
        UdpChannelCache.getChannelClientCache().remove(srcSocketAddress);
        UdpChannelCache.getChannelRouteCache().remove(srcSocketAddress);
        super.channelInactive(ctx);
    }
}
