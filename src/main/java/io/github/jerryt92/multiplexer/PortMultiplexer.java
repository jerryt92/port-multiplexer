package io.github.jerryt92.multiplexer;

import io.github.jerryt92.multiplexer.conf.ConfigService;
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

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * @Date: 2025/4/17
 * @Author: jerryt92
 */
public class PortMultiplexer {
    private static final Logger log = LogManager.getLogger(PortMultiplexer.class);
    private static long startTime;
    private static ServerBootstrap tcpBootstrap;
    private static Bootstrap udpBootstrap;
    private static ConfigService.AppConfig appConfig;

    public static void main(String[] args) {
        startTime = System.currentTimeMillis();
        appConfig = ConfigService.INSTANCE.getAppConfig();
        ScheduledExecutorService readConfigCron = new ScheduledThreadPoolExecutor(1);
        readConfigCron.scheduleAtFixedRate(() -> {
            try {
                ConfigService.AppConfig newConfig = ConfigService.INSTANCE.readConfig();
                if (!appConfig.equals(newConfig)) {
                    log.info("config changed");
                    log.info("new config: " + newConfig);
                    if (!appConfig.getBindConfig().equals(newConfig.getBindConfig()) || appConfig.isTcpEnabled() != newConfig.isTcpEnabled() || appConfig.isUdpEnabled() != newConfig.isUdpEnabled()) {
                        if (!appConfig.getBindConfig().getTcpHost().equals(newConfig.getBindConfig().getTcpHost()) || appConfig.getBindConfig().getTcpPort() != newConfig.getBindConfig().getTcpPort() || appConfig.isTcpEnabled() != newConfig.isTcpEnabled()) {
                            log.info("TCP config changed, restarting server...");
                            runTcpServer(newConfig);
                        }
                        if (!appConfig.getBindConfig().getUdpHost().equals(newConfig.getBindConfig().getUdpHost()) || appConfig.getBindConfig().getUdpPort() != newConfig.getBindConfig().getUdpPort() || appConfig.isUdpEnabled() != newConfig.isUdpEnabled()) {
                            log.info("UDP config changed, restarting server...");
                            runUdpServer(newConfig);
                        }
                    }
                    ConfigService.INSTANCE.setAppConfig(newConfig);
                    appConfig = newConfig;
                }
            } catch (Throwable t) {
                log.error("", t);
            }
        }, 0, 30, java.util.concurrent.TimeUnit.SECONDS);
        runTcpServer(appConfig);
        runUdpServer(appConfig);
    }

    private static void runTcpServer(ConfigService.AppConfig appConfig) {
        stopTcpServer();
        if (appConfig.isTcpEnabled()) {
            new Thread(() -> {
                tcpBootstrap = new ServerBootstrap();
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
                                    log.info("PortMultiplexer started at tcp-port {} in {}ms", appConfig.getBindConfig().getTcpPort(), System.currentTimeMillis() - startTime);
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
    }

    private static void stopTcpServer() {
        if (tcpBootstrap != null && tcpBootstrap.group() != null && !tcpBootstrap.group().isShutdown()) {
            tcpBootstrap.group().shutdownGracefully();
        }
    }

    private static void runUdpServer(ConfigService.AppConfig appConfig) {
        stopUdpServer();
        if (appConfig.isUdpEnabled()) {
            new Thread(() -> {
                udpBootstrap = new Bootstrap();
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
                            .bind(appConfig.getBindConfig().getUdpHost(), appConfig.getBindConfig().getUdpPort())
                            .addListener(future -> {
                                if (future.isSuccess()) {
                                    log.info("PortMultiplexer started at udp-port {} in {}ms", appConfig.getBindConfig().getUdpPort(), System.currentTimeMillis() - startTime);
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

    private static void stopUdpServer() {
        if (udpBootstrap != null && udpBootstrap.group() != null && !udpBootstrap.group().isShutdown()) {
            udpBootstrap.group().shutdownGracefully();
        }
    }
}