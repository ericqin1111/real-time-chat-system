package server.handler.general;

import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolConfig;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import server.GlobalVar;
import server.handler.RouterConfig;
import server.handler.utils.JwtUtil;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class RouterHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    // 定义路由规则（路径 -> 对应的 Handler 链）
    private static final Map<String, Consumer<ChannelPipeline>> routeMap = RouterConfig.getRouteMap();

    private final SslContext sslContext;

    public RouterHandler(SslContext sslContext) {
        this.sslContext = sslContext;
    }



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
        if (isWebSocketUpgradeRequest(request)
             &&   ((InetSocketAddress)ctx.channel().localAddress()).getPort() == GlobalVar.HTTPS_PORT
                ) {
            String userid = parseGetParameters(request);
            System.out.println("我进入了web处理链");
            ctx.channel().attr(GlobalVar.USERID).set(userid);
            // 3. 获取对应的处理器链配置
            Consumer<ChannelPipeline> handlerChainBuilder = routeMap.get(uri);
            if (handlerChainBuilder != null) {
                // 4. 动态配置 Pipeline
                configureWebSocketPipeline(ctx, uri, handlerChainBuilder);
                // 5. 触发协议升级
                // 5. 触发协议升级，需保留请求对象
                GlobalVar.addUserChannel(userid, ctx.channel());
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


        // 不要移除这个东西，否则响应都发不出去！！！！！！！！！！！！！！！！！！！！！！！！！！

//        ctx.pipeline().remove("httpServerCodec");
         ctx.pipeline().remove("httpObjectAggregator");
        ctx.pipeline().remove("corsInboundHandler");
        ctx.pipeline().remove("jwtAuthHandler");
        ctx.pipeline().remove("paramsHandler");
        ctx.pipeline().remove(this);
        ctx.pipeline().remove("jsonOutboundEncoder");

        // 2. 添加SSL处理器（如果使用WSS）
        // 注意：这部分需要从外部传入sslContext或作为类成员变量
//        ctx.pipeline().addLast(sslContext.newHandler(ctx.alloc()));

        // 3. 添加 WebSocket 处理器（支持子协议、扩展和跨域）
        WebSocketServerProtocolConfig config = WebSocketServerProtocolConfig.newBuilder()
                .websocketPath(path)            // 路径
                .allowExtensions(true)         // 允许扩展
                .checkStartsWith(true)         // 检查路径前缀
                .build();

        ctx.pipeline().addLast(new WebSocketServerProtocolHandler(config));
        // 添加空闲状态处理器（核心心跳检测组件）
        ctx.pipeline().addLast(new IdleStateHandler(
                60, 0, 0, TimeUnit.SECONDS));

        ctx.pipeline().addLast(new WSUnifiedEncoder());


        ctx.pipeline().addLast(new HeartbeatAndFrameHandler(3));


        // 根据路径添加对应的 WebSocket 处理器
        System.out.println("ok wsrouter");
        handlerChainBuilder.accept(ctx.pipeline());

        ctx.pipeline().fireUserEventTriggered(WebSocketServerProtocolHandler.HandshakeComplete.class);






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
    private String parseGetParameters(FullHttpRequest request) {
        QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
        Map<String, List<String>> rawParams = decoder.parameters();

        // 5. 使用参数
        String token = null;
        if (rawParams.containsKey("token")) {
            token = rawParams.get("token").get(0); // GET参数a的第一个值
        }
        String userid = JwtUtil.getUsername(token);
        return userid; // 返回不可修改的 Map
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
