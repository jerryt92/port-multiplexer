package io.github.jerryt92.multiplexer.forward.tcp;

import io.github.jerryt92.multiplexer.entity.ForwardTarget;
import io.netty.channel.Channel;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @Date: 2025/4/17
 * @Author: jerryt92
 */
public class TcpChannelCache {
    private static final TcpChannelCache INSTANCE = new TcpChannelCache();

    private static TcpChannelCache getInstance() {
        return INSTANCE;
    }

    private TcpChannelCache() {
    }

    /**
     * TCP通道的缓存
     * Channel's proxy client cache
     */
    private final ConcurrentHashMap<Channel, Channel> channelClientCache = new ConcurrentHashMap<>();
    /**
     * 通道的转发缓存
     * Channel's forward cache
     */
    private final ConcurrentHashMap<Channel, ForwardTarget> channelRouteCache = new ConcurrentHashMap<>();

    public static ConcurrentHashMap<Channel, Channel> getChannelClientCache() {
        TcpChannelCache instance = getInstance();
        return instance.channelClientCache;
    }

    public static ConcurrentHashMap<Channel, ForwardTarget> getChannelRouteCache() {
        TcpChannelCache instance = getInstance();
        return instance.channelRouteCache;
    }
}
