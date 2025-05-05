package server.handler.general;

import io.netty.buffer.Unpooled;
import  io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import server.GlobalVar;

import java.net.InetSocketAddress;

public class CorsInboundHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final FullHttpResponse PRECONFIGURED_OPTIONS_RESPONSE_HTTP;
    private static final FullHttpResponse PRECONFIGURED_OPTIONS_RESPONSE_HTTPS;

    static {
        PRECONFIGURED_OPTIONS_RESPONSE_HTTP = createOptionsResponse("http");
        PRECONFIGURED_OPTIONS_RESPONSE_HTTPS = createOptionsResponse("https");
    };

        private static FullHttpResponse createOptionsResponse(String protocol){
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.NO_CONTENT,
                    Unpooled.unreleasableBuffer(Unpooled.EMPTY_BUFFER)); // 创建不可释放的 Buffer}
            response.headers()
                    .set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN,  "http://localhost:" + GlobalVar.ALLOWED_PORT)

//                .set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, protocol + "://localhost:" + GlobalVar.ALLOWED_PORT)
                .set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS")
                .set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type, Authorization")
                .set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
                .set(HttpHeaderNames.ACCESS_CONTROL_MAX_AGE, "86400");
            response.retain(); // 增加引用计数（防止被 Netty 释放）
            return response;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {

            System.out.println("cors in :" + request.method() + " " + request.uri());
        // 处理预检请求（OPTIONS）
        if (request.method().equals(HttpMethod.OPTIONS)) {
            int port = ((InetSocketAddress) ctx.channel().localAddress()).getPort();
            FullHttpResponse response = (port==GlobalVar.HTTPS_PORT)?
                    PRECONFIGURED_OPTIONS_RESPONSE_HTTP.retainedDuplicate()
                    :PRECONFIGURED_OPTIONS_RESPONSE_HTTPS.retainedDuplicate()
                    ; // 复制并保留引用

            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            return;
        }

        // 其他请求传递到下一个 Handler

        ctx.fireChannelRead(request.retain());
    }




    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
