package server.handler.general;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import server.handler.RouterConfig;


import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class RouterHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    // 定义路由规则（路径 -> 对应的 Handler 链）
    private static final Map<String, Consumer<ChannelPipeline>> routeMap = RouterConfig.getRouteMap();




    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        String uri = request.uri();
        HttpMethod method = request.method();
        System.out.println(method + " " + uri);
        // 匹配路由
        String matchedPath = findMatchedPath(uri);
        if (matchedPath == null) {
            sendError(ctx, HttpResponseStatus.NOT_FOUND);
            return;
        }

        // 动态添加 Handler 链
        Consumer<ChannelPipeline> handlerChainBuilder = routeMap.get(matchedPath);
        if (handlerChainBuilder != null) {
            // 移除自身，避免重复路由
            ctx.pipeline().remove(this);
            // 构建子处理链
            handlerChainBuilder.accept(ctx.pipeline());
            // 重新触发请求处理
            ctx.fireChannelRead(request.retain());
        } else {
            sendError(ctx, HttpResponseStatus.NOT_FOUND);
        }
    }

    // 路径匹配（支持前缀匹配）
    private String findMatchedPath(String uri) {
        for (String path : routeMap.keySet()) {
            if (uri.startsWith(path)) {
                return path;
            }
        }
        return null;
    }

    // 发送错误响应
    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                status,
                Unpooled.copiedBuffer("Path Not Found", CharsetUtil.UTF_8)
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
