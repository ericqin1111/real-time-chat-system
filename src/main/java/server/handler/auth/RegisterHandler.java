package server.handler.auth;

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

import java.util.List;

import static client.ChatClient.createChatClient;

public class RegisterHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest) throws Exception {
        // 解析请求体
        String content = fullHttpRequest.content().toString(CharsetUtil.UTF_8);
        // 假设请求体是一个 JSON 格式的字符串，包含用户名和密码
        String username = parseJson(content, "username");
        String password = parseJson(content, "password");
        System.out.println(username);
        System.out.println(password);

        // 查询是否已经存在该用户名
        MyBatisConfig.execute(UserMapper.class, mapper -> {
            User user = mapper.findUserByUsername(username);
            if (user != null) {
                // 用户已存在
                sendErrorResponse(ctx, "Username already exists.");
                return;
            }
            createChatClient(ctx,username);
        });



        // 插入新用户
        MyBatisConfig.execute(UserMapper.class, mapper -> {
            mapper.insertUser(username,password);
        });

        sendSuccessResponse(ctx);
    }

    private String parseJson(String content, String key) {
        // 假设请求体是 JSON 格式，解析用户名和密码
        JSONObject json = new JSONObject(content);
        return json.getString(key);
    }

    private void sendErrorResponse(ChannelHandlerContext ctx, String errorMessage) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.BAD_REQUEST,
                Unpooled.wrappedBuffer(errorMessage.getBytes())
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        ctx.writeAndFlush(response);
    }

    private void sendSuccessResponse(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.wrappedBuffer("Registration successful.".getBytes())
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        ctx.writeAndFlush(response);
    }
}
