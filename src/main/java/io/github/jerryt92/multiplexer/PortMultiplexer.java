package io.github.jerryt92.multiplexer;

import io.github.jerryt92.multiplexer.conf.ConfigService;
import io.github.jerryt92.multiplexer.forward.tcp.TcpRequestHandler;
import io.github.jerryt92.multiplexer.forward.udp.UdpRequestHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * @Date: 2025/4/17
 * @Author: jerryt92
 */
public class PortMultiplexer {
    private static final Logger log = LogManager.getLogger(PortMultiplexer.class);
    private static long startTime;
    private static Thread tcpThread;
    private static ServerBootstrap tcpBootstrap;
    private static EventLoopGroup tcpBossGroup;
    private static EventLoopGroup tcpWorkerGroup;
    private static Thread udpThread;
    private static Bootstrap udpBootstrap;
    private static EventLoopGroup udpWorkerGroup;
    private static ConfigService.AppConfig appConfig;
    private static final ConcurrentHashMap<Thread, Thread> serverThreads = new ConcurrentHashMap<>();
    /**
     * 最大缓冲区大小1024MB
     */
    private static final int maxBufferSize = 1024 * 1024 * 1024;

    public static void main(String[] args) {
        startTime = System.currentTimeMillis();
        appConfig = ConfigService.INSTANCE.getAppConfig();
        ScheduledExecutorService readConfigCron = new ScheduledThreadPoolExecutor(1);
        readConfigCron.scheduleAtFixedRate(PortMultiplexer::daemonThreadRunnable, 0, 3, java.util.concurrent.TimeUnit.SECONDS);
        runTcpServer(appConfig);
        runUdpServer(appConfig);
    }

    private static void runTcpServer(ConfigService.AppConfig appConfig) {
        stopTcpServer();
        if (appConfig.isTcpEnabled()) {
            tcpThread = new Thread(() -> {
                while (!Thread.interrupted() && serverThreads.containsKey(Thread.currentThread())) {
                    tcpBootstrap = new ServerBootstrap();
                    tcpBossGroup = new NioEventLoopGroup(1);
                    tcpWorkerGroup = new NioEventLoopGroup();
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
                                .option(ChannelOption.SO_BACKLOG, 16384)
                                // 设置连接超时时间
                                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                                // 设置自动分配缓冲区
                                .option(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator(100 * 1024 * 1024, 100 * 1024 * 1024, maxBufferSize))
                                // 子通道（客户端连接）也设置相同参数
                                .childOption(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator(100 * 1024 * 1024, 100 * 1024 * 1024, maxBufferSize))
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
                    }
                }
            });
            serverThreads.put(tcpThread, tcpThread);
            tcpThread.setDaemon(true);
            tcpThread.start();
        }
    }

    private static void stopTcpServer() {
        if (tcpThread != null) {
            serverThreads.remove(tcpThread);
        }
        if (tcpBossGroup != null && !tcpBossGroup.isShutdown()) {
            tcpBossGroup.shutdownGracefully();
        }
        if (tcpWorkerGroup != null && !tcpWorkerGroup.isShutdown()) {
            tcpWorkerGroup.shutdownGracefully();
        }
    }

    private static void runUdpServer(ConfigService.AppConfig appConfig) {
        stopUdpServer();
        if (appConfig.isUdpEnabled()) {
            udpThread = new Thread(() -> {
                while (!Thread.interrupted() && serverThreads.containsKey(Thread.currentThread())) {
                    udpBootstrap = new Bootstrap();
                    udpWorkerGroup = new NioEventLoopGroup(10);
                    try {
                        ChannelHandler udpChannelHandler = new ChannelInitializer<DatagramChannel>() {
                            @Override
                            protected void initChannel(DatagramChannel datagramChannel) {
                                datagramChannel.pipeline().addLast(new UdpRequestHandler(udpWorkerGroup));
                            }
                        };
                        udpBootstrap.group(udpWorkerGroup)
                                .option(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator(100 * 1024 * 1024, 100 * 1024 * 1024, maxBufferSize))
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
                    }
                }
            });
            serverThreads.put(udpThread, udpThread);
            udpThread.setDaemon(true);
            udpThread.start();
        }
    }

    private static void stopUdpServer() {
        if (udpThread != null) {
            serverThreads.remove(udpThread);
        }
        if (udpWorkerGroup != null && !udpWorkerGroup.isShutdown()) {
            udpWorkerGroup.shutdownGracefully();
        }
    }

    // 守护线程
    private static void daemonThreadRunnable() {
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
    }
}