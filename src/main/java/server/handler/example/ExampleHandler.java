package server.handler.example;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import server.handler.general.ParamsHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExampleHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest) throws Exception {
        List<String> list = new ArrayList<>();
        list.add("ok");
        list.add("true");


        // 3. 构建 HTTP 响应
        ctx.writeAndFlush(list);
    }
}
