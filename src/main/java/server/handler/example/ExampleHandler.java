package server.handler.example;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import server.handler.utils.JsonUtil;

import java.util.ArrayList;
import java.util.List;

public class ExampleHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest) throws Exception {
        List<String> list = new ArrayList<>();
        list.add("thats ok!!!!!");
        String json = JsonUtil.toJson(list);
        // 3. 构建 HTTP 响应
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.copiedBuffer(json, CharsetUtil.UTF_8)
        );
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}
