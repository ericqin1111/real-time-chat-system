package server.handler;

import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpServerCodec;
import lombok.Getter;
import server.handler.example.ExampleHandler;
import server.handler.example.WSExampleHandler;

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




        //这里写websocket的路由规则
        routeMap.put("/wsexample", pipeline -> {
            pipeline.addLast(new WSExampleHandler());
        });
    }











}
