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
                Integer userId = user.getUserId();

                String token= JwtUtil.createToken(Integer.toString(userId));
                sendSuccessResponse(ctx,token,userId,username);
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
        JSONObject responseJson = new JSONObject();
        responseJson.put("errorMessage",errorMessage);
        String responseBody = responseJson.toString();

        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.UNAUTHORIZED,
                Unpooled.wrappedBuffer(responseBody.getBytes())
        );

        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        ctx.writeAndFlush(response);
    }

    private void sendSuccessResponse(ChannelHandlerContext ctx, String token, Integer userId, String username) {
        // 1. 创建 JSON 对象
        JSONObject responseJson = new JSONObject();
        responseJson.put("token", token);
        responseJson.put("userId", userId);
        responseJson.put("username", username);

        // 2. 转为字符串
        String responseBody = responseJson.toString();
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.wrappedBuffer(responseBody.getBytes(CharsetUtil.UTF_8))
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        ctx.writeAndFlush(response);
    }



}
