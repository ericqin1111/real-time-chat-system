package server.handler.utils;

import io.netty.channel.Channel;

import java.util.concurrent.locks.ReentrantLock;

public class UserChannelInfo {
    private final Channel channel;
    private final ReentrantLock lock;

    public UserChannelInfo(Channel channel) {
        this.channel = channel;
        this.lock = new ReentrantLock();
    }

    public Channel getChannel() {
        return channel;
    }

    public ReentrantLock getLock() {
        return lock;
    }
}