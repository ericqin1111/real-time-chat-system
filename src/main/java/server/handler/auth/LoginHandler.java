package server.handler.auth;

import client.ChatClient;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.json.JSONObject;
import server.GlobalVar;
import mapper.UserMapper;
import entity.User;
import config.MyBatisConfig;
import server.handler.utils.JwtUtil;

import java.util.List;
import static client.ChatClient.createChatClient;

public class LoginHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest) throws Exception {
        // 解析请求体

        String content = fullHttpRequest.content().toString(CharsetUtil.UTF_8);
        String username = parseJson(content, "username");
        String password = parseJson(content, "password");


        // 查询用户是否存在并验证密码
        MyBatisConfig.execute(UserMapper.class, mapper -> {
            User user = mapper.findUserByUsername(username);
            if (user == null || !user.getPassword().equals(password)) {
                sendErrorResponse(ctx, "Invalid username or password.");
                return;
            }
            else {
                String token= JwtUtil.createToken(username);
                sendSuccessResponse(ctx,token);
            }


            // 登录成功，创建 ChatClient 类
            createChatClient(ctx, username);
        });




    }

    private String parseJson(String content, String key) {
        // 假设请求体是 JSON 格式，解析用户名和密码
        JSONObject json = new JSONObject(content);
        return json.getString(key);
    }

    private void sendErrorResponse(ChannelHandlerContext ctx, String errorMessage) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.UNAUTHORIZED,
                Unpooled.wrappedBuffer(errorMessage.getBytes())
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        ctx.writeAndFlush(response);
    }

    private void sendSuccessResponse(ChannelHandlerContext ctx,String token) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.wrappedBuffer(token.getBytes())
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        ctx.writeAndFlush(response);
    }



}
