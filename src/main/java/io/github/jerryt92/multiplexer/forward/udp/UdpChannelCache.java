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
     * TCP通道的缓存
     * Channel's proxy client cache
     */
    private final ConcurrentHashMap<InetSocketAddress, Channel> channelClientCache = new ConcurrentHashMap<>();
    /**
     * 通道的Http路由缓存
     * Channel's Http route cache
     */
    private final ConcurrentHashMap<Channel, ForwardTarget> channelRouteCache = new ConcurrentHashMap<>();

    public static ConcurrentHashMap<InetSocketAddress, Channel> getChannelClientCache() {
        UdpChannelCache instance = getInstance();
        return instance.channelClientCache;
    }

    public static ConcurrentHashMap<Channel, ForwardTarget> getChannelRouteCache() {
        UdpChannelCache instance = getInstance();
        return instance.channelRouteCache;
    }
}
