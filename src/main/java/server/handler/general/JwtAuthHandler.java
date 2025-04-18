package server.handler.general;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import server.handler.JwtAuthConfig;
import server.handler.utils.JwtUtil;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class JwtAuthHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    // 需要认证的 API 路径前缀
    private static final List<String> SECUREFREE_PATHS = JwtAuthConfig.getSECUREFREE_PATHS();


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        String uri = request.uri();

        // 1. 判断是否为需要认证的路径
        if (!isSecuredFreePath(uri)) {
            // 2. 提取并验证 JWT
            String token = extractToken(request);
            if (token == null || !JwtUtil.verifyToken(token)) {
                sendUnauthorizedResponse(ctx);
                return; // 终止处理
            }
        }

        // 3. 传递请求到下一个 Handler
        ctx.fireChannelRead(request.retain());
    }

    // 判断路径是否需要认证
    private boolean isSecuredFreePath(String uri) {
        return SECUREFREE_PATHS.stream().anyMatch(uri::startsWith);
    }

    // 从请求头提取 JWT
    private String extractToken(FullHttpRequest request) {
        String authHeader = request.headers().get("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }



    // 返回 401 Unauthorized
    private void sendUnauthorizedResponse(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.UNAUTHORIZED,
                Unpooled.copiedBuffer("Unauthorized: Invalid or missing token", CharsetUtil.UTF_8)
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        sendUnauthorizedResponse(ctx);
    }
}
