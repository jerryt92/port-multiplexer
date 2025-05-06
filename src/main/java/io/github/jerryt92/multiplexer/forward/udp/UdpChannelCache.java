package io.github.jerryt92.multiplexer.forward.udp;

import io.github.jerryt92.multiplexer.entity.ForwardTarget;
import io.netty.channel.Channel;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Date: 2025/4/17
 * @Author: jerryt92
 */
public class UdpChannelCache {
    private static final UdpChannelCache INSTANCE = new UdpChannelCache();

    private static UdpChannelCache getInstance() {
        return INSTANCE;
    }

    private UdpChannelCache() {
    }

    /**
     * UDP会话通道的缓存
     * UDP-Session's proxy client cache
     */
    private final ConcurrentHashMap<InetSocketAddress, Channel> channelClientCache = new ConcurrentHashMap<>();
    /**
     * UDP会话的转发缓存
     * UDP-Session's forward cache
     */
    private final ConcurrentHashMap<InetSocketAddress, ForwardTarget> channelRouteCache = new ConcurrentHashMap<>();

    public static ConcurrentHashMap<InetSocketAddress, Channel> getChannelClientCache() {
        UdpChannelCache instance = getInstance();
        return instance.channelClientCache;
    }

    public static ConcurrentHashMap<InetSocketAddress, ForwardTarget> getChannelRouteCache() {
        UdpChannelCache instance = getInstance();
        return instance.channelRouteCache;
    }
}
