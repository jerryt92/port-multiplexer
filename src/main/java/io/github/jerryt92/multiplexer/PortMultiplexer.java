package io.github.jerryt92.multiplexer;

import io.github.jerryt92.multiplexer.conf.ConfigReader;
import io.github.jerryt92.multiplexer.forward.tcp.TcpRequestHandler;
import io.github.jerryt92.multiplexer.forward.udp.UdpRequestHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @Date: 2025/4/17
 * @Author: jerryt92
 */
public class PortMultiplexer {
    private static final Logger log = LogManager.getLogger(PortMultiplexer.class);

    public static void main(String[] args) {
        ConfigReader.AppConfig appConfig = ConfigReader.INSTANCE.getAppConfig();
        runTcpServer(appConfig);
        runUdpServer(appConfig);
    }

    private static void runTcpServer(ConfigReader.AppConfig appConfig) {
        new Thread(() -> {
            ServerBootstrap tcpBootstrap = new ServerBootstrap();
            EventLoopGroup tcpBossGroup = new NioEventLoopGroup(1);
            EventLoopGroup tcpWorkerGroup = new NioEventLoopGroup();
            try {
                ChannelHandler tcpChannelHandler = new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel socketChannel) {
                        socketChannel.pipeline().addLast(new TcpRequestHandler(tcpWorkerGroup));
                    }
                };
                tcpBootstrap.group(tcpBossGroup, tcpWorkerGroup)
                        .channel(NioServerSocketChannel.class)
                        .childHandler(tcpChannelHandler)
                        // 设置并发连接数
                        .option(ChannelOption.SO_BACKLOG, 1024)
                        .bind(appConfig.getBindConfig().getTcpHost(), appConfig.getBindConfig().getTcpPort())
                        .addListener(future -> {
                            if (future.isSuccess()) {
                                log.info("PortMultiplexer started at tcp-port {}", appConfig.getBindConfig().getTcpPort());
                            } else {
                                log.error("Failed to start PortMultiplexer", future.cause());
                            }
                        })
                        .sync()
                        .channel()
                        .closeFuture()
                        .sync();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                tcpWorkerGroup.shutdownGracefully();
                tcpBossGroup.shutdownGracefully();
            }
        }).start();
    }

    private static void runUdpServer(ConfigReader.AppConfig appConfig) {
        new Thread(() -> {
            Bootstrap udpBootstrap = new Bootstrap();
            EventLoopGroup udpWorkerGroup = new NioEventLoopGroup(10);
            try {
                ChannelHandler udpChannelHandler = new ChannelInitializer<DatagramChannel>() {
                    @Override
                    protected void initChannel(DatagramChannel datagramChannel) {
                        datagramChannel.pipeline().addLast(new UdpRequestHandler(udpWorkerGroup));
                    }
                };
                udpBootstrap.group(udpWorkerGroup)
                        .option(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(16384))
                        .channel(NioDatagramChannel.class)
                        .handler(udpChannelHandler)
                        // 设置并发连接数
                        .bind(appConfig.getBindConfig().getUdpHost(), appConfig.getBindConfig().getUdpPort())
                        .addListener(future -> {
                            if (future.isSuccess()) {
                                log.info("PortMultiplexer started at udp-port {}", appConfig.getBindConfig().getUdpPort());
                            } else {
                                log.error("Failed to start PortMultiplexer", future.cause());
                            }
                        })
                        .channel()
                        .closeFuture()
                        .sync();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                udpWorkerGroup.shutdownGracefully();
            }
        }).start();
    }
}