package server.handler.example;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.util.AttributeKey;
import server.GlobalVar;

import java.util.HashMap;
import java.util.Map;



public class WSExampleHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {



    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) {
        System.out.println(msg.text());
        Map<String, String> map = ctx.channel().attr(GlobalVar.PARAM_KEY).get();
        System.out.println(map.get("user"));
        ctx.writeAndFlush(new TextWebSocketFrame(msg.text()));
    }


}
