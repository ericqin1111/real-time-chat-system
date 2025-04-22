package server.handler.example;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.util.AttributeKey;
import server.GlobalVar;

import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class WSExampleHandler extends SimpleChannelInboundHandler<Object> {



    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {

        Map<String, String> content = ctx.channel().attr(GlobalVar.DATA_CONTEXT).get();
        System.out.println(content.get("data"));
        Map<String, Integer> www  = new HashMap<>();
        www.put("what", 99);
        ctx.writeAndFlush(www);
    }


}
