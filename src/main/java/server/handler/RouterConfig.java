package server.handler;

import io.netty.channel.ChannelPipeline;
import io.netty.handler.stream.ChunkedWriteHandler;
import lombok.Getter;
import server.handler.auth.RegisterHandler;
import server.handler.chatroom.ChatHandler;
import server.handler.chatroom.FriendRequestOperationsHandler;
import server.handler.chatroom.MessageHandler;
import server.handler.example.ExampleHandler;
import server.handler.example.ThreadPoolTestHandler;
import server.handler.example.WSExampleHandler;
import server.handler.auth.LoginHandler;
import server.handler.file.FileHandler;
import server.handler.websocket.OnlineHandler;
import server.handler.websocket.WebsocketHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class RouterConfig {
    @Getter
    private static final Map<String, Consumer<ChannelPipeline>> routeMap = new HashMap<>();

    static {
        // 注册路由规则

        //这里写http的路由规则
        routeMap.put("/example", pipeline -> {
            pipeline.addLast(new ExampleHandler());
        });

        routeMap.put("/test/pool", pipeline -> {
            // 这个路径不需要复杂的 Handler 链，直接添加测试 Handler
            pipeline.addLast(new ThreadPoolTestHandler());
        });

        routeMap.put("/api/users",pipeline -> {
            pipeline.addLast(new MessageHandler());
        });


        routeMap.put("/login",pipeline ->{
            pipeline.addLast(new LoginHandler());
        });

        routeMap.put("/register",pipeline ->{
            pipeline.addLast(new RegisterHandler());
        });

        routeMap.put("/file", pipeline ->{
            pipeline.addLast(new ChunkedWriteHandler());
            pipeline.addLast(new FileHandler());
        });

        routeMap.put("/users/chat",pipeline -> {
            pipeline.addLast(new ChatHandler());
        });


        //这里写websocket的路由规则
        routeMap.put("/wsexample", pipeline -> {
            pipeline.addLast(new WSExampleHandler());
        });

        routeMap.put("/websocket", pipeline -> {
            pipeline.addLast(new WebsocketHandler());
        });
    }











}
