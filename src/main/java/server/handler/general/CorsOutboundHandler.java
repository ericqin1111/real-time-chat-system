package server.handler.general;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import server.GlobalVar;

public class CorsOutboundHandler extends ChannelOutboundHandlerAdapter {

    // 覆盖 write 方法，拦截所有出站响应
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof FullHttpResponse) {

            FullHttpResponse response = (FullHttpResponse) msg;

            addCorsHeaders(response); // 添加 CORS 头
            System.out.println("ok cors outbound");
            System.out.println(response.headers());
        }
        // 其他类型消息（如 ByteBuf）直接传递
        super.write(ctx, msg, promise); // 必须调用父类方法传递消息
    }

    // 统一设置 CORS 头
    private void addCorsHeaders(FullHttpResponse response) {
        // 允许的源（可根据请求动态设置）
        String origin = "http://localhost:" + GlobalVar.ALLOWED_PORT;

        response.headers()
                .set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, origin)
                .set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS")
                .set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type, Authorization, X-Requested-With")
                .set(HttpHeaderNames.ACCESS_CONTROL_MAX_AGE, "3600")
                .set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
    }
}
