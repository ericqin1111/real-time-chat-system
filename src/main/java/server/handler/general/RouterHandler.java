package server.handler.general;

import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolConfig;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import server.GlobalVar;
import server.handler.RouterConfig;

import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class RouterHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    // 定义路由规则（路径 -> 对应的 Handler 链）
    private static final Map<String, Consumer<ChannelPipeline>> routeMap = RouterConfig.getRouteMap();




    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {

        QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
        String uri = decoder.path();      // 路径部分 → "/ws/chat"
        HttpMethod method = request.method();
        System.out.println(method + " " + uri);
        // 匹配路由
        String matchedPath = findMatchedPath(uri);
        if (matchedPath == null) {
            sendError(ctx, HttpResponseStatus.NOT_FOUND);
            return;
        }

        //判断是否为 WebSocket 升级请求
        if (isWebSocketUpgradeRequest(request)) {
            Map<String, String> params = parseGetParameters(request);
            ctx.channel().attr(GlobalVar.PARAM_KEY).set(params);
            // 3. 获取对应的处理器链配置
            Consumer<ChannelPipeline> handlerChainBuilder = routeMap.get(uri);
            if (handlerChainBuilder != null) {
                // 4. 动态配置 Pipeline
                configureWebSocketPipeline(ctx, uri, handlerChainBuilder);
                // 5. 触发协议升级
                // 5. 触发协议升级，需保留请求对象

                ctx.fireChannelRead(request.retain());
            } else {
                sendError(ctx,  HttpResponseStatus.NOT_FOUND);
            }
        }else{
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
    }

















    private boolean isWebSocketUpgradeRequest(FullHttpRequest request) {
        HttpHeaders headers = request.headers();
        return headers.containsValue("Upgrade", "websocket", true)
                && headers.containsValue("Connection", "Upgrade", true);
    }

    private void configureWebSocketPipeline(ChannelHandlerContext ctx, String path, Consumer<ChannelPipeline> handlerChainBuilder) {
//        // 动态移除 HTTP 处理器

        ctx.pipeline().remove("corsInboundHandler");
        ctx.pipeline().remove("jwtAuthHandler");
        ctx.pipeline().remove("paramsHandler");
        ctx.pipeline().remove(this);
        ctx.pipeline().remove("jsonOutboundEncoder");


        // 2. 添加 WebSocket 处理器（支持子协议、扩展和跨域）
        WebSocketServerProtocolConfig config = WebSocketServerProtocolConfig.newBuilder()
                .websocketPath(path)            // 路径
                .allowExtensions(true)         // 允许扩展
                .checkStartsWith(true)         // 检查路径前缀
                .build();

        ctx.pipeline().addLast(new WebSocketServerProtocolHandler(config));


        // 根据路径添加对应的 WebSocket 处理器
        System.out.println("ok wsrouter");
        handlerChainBuilder.accept(ctx.pipeline());

        ctx.pipeline().fireUserEventTriggered(WebSocketServerProtocolHandler.HandshakeComplete.class);
        System.out.println("当前 Pipeline 处理器链: " + ctx.pipeline().names());
    }

    // 路径匹配（支持前缀匹配）
    private String findMatchedPath(String uri) {
        // 优先精确匹配
        if (routeMap.containsKey(uri)) {
            return uri;
        }
        // 其次匹配路径前缀（如 /ws/chat 匹配 /ws/chat）
        for (String path : routeMap.keySet()) {
            if (uri.startsWith(path + "/") || uri.equals(path)) {
                return path;
            }
        }
        return null;
    }

    // 解析 GET 请求参数为 Map<String, String>
    private Map<String, String> parseGetParameters(FullHttpRequest request) {
        QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
        Map<String, List<String>> rawParams = decoder.parameters();

        // 转换为 Map<String, String>（若有多个值，取第一个）
        Map<String, String> params = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : rawParams.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();
            if (!values.isEmpty()) {
                params.put(key, values.get(0)); // 取第一个值
            }
        }
        return Collections.unmodifiableMap(params); // 返回不可修改的 Map
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
