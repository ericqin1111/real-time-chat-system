package server.handler.ssl;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ChatClientHandler extends SimpleChannelInboundHandler<String> {
    private final String username;

    public ChatClientHandler(String username) {
        this.username = username;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        // 直接打印服务器返回的消息
        System.out.println(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}