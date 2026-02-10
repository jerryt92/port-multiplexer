package io.github.jerryt92.multiplexer.forward.tcp;

import io.github.jerryt92.multiplexer.entity.ForwardTarget;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import lombok.Data;

import java.util.ArrayDeque;
import java.util.Queue;
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
    private final ConcurrentHashMap<Channel, OutboundState> channelClientCache = new ConcurrentHashMap<>();
    /**
     * 通道的转发缓存
     * Channel's forward cache
     */
    private final ConcurrentHashMap<Channel, ForwardTarget> channelRouteCache = new ConcurrentHashMap<>();

    /**
     * 缓存的通道状态
     * Cache channel state
     */
    @Data
    public static final class OutboundState {
        private Channel outbound;
        private ChannelFuture connectFuture;
        private final Queue<Object> pending = new ArrayDeque<>();
    }

    /**
     * 通道的代理客户端缓存
     * Channel's proxy client cache
     */
    public static ConcurrentHashMap<Channel, OutboundState> getChannelClientCache() {
        TcpChannelCache instance = getInstance();
        return instance.channelClientCache;
    }

    public static ConcurrentHashMap<Channel, ForwardTarget> getChannelRouteCache() {
        TcpChannelCache instance = getInstance();
        return instance.channelRouteCache;
    }
}
