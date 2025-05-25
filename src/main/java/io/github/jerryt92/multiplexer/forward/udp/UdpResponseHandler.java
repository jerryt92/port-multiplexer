package io.github.jerryt92.multiplexer.forward.udp;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class UdpResponseHandler extends ChannelInboundHandlerAdapter {
    private static final Logger log = LogManager.getLogger(UdpResponseHandler.class);
    public static final Long CHANNEL_IDLE_TIMEOUT_MINUTE = 10L;
    private final Channel inboundChannel;
    private final InetSocketAddress srcSocketAddress;
    private Long closeTimestamp;

    public UdpResponseHandler(Channel inboundChannel, InetSocketAddress srcSocketAddress) {
        this.inboundChannel = inboundChannel;
        this.srcSocketAddress = srcSocketAddress;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        // 10分钟之后自动关闭
        this.closeTimestamp = System.currentTimeMillis() + CHANNEL_IDLE_TIMEOUT_MINUTE * 60 * 1000;
        new Thread(() -> {
            while (System.currentTimeMillis() < closeTimestamp) {
                // 10min
                try {
                    new CountDownLatch(1).await(10, TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                    log.error("", e);
                }
            }
            ctx.channel().close();
        }).start();
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) {
        this.closeTimestamp = System.currentTimeMillis() + CHANNEL_IDLE_TIMEOUT_MINUTE * 60 * 1000;
        DatagramPacket responsePacket = (DatagramPacket) msg;
        // 使用 DatagramPacket 指定目标地址
        DatagramPacket outboundPacket = new DatagramPacket(responsePacket.content().retain(), srcSocketAddress);
        inboundChannel.writeAndFlush(outboundPacket);
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
        UdpChannelCache.getChannelClientCache().remove(srcSocketAddress);
        UdpChannelCache.getChannelRouteCache().remove(srcSocketAddress);
        super.channelInactive(ctx);
    }
}
