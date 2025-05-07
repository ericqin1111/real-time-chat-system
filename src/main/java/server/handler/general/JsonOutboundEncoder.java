package server.handler.general;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.*;
import io.netty.util.AttributeKey;

import java.util.Map;

public class JsonOutboundEncoder extends ChannelOutboundHandlerAdapter {

//    private static final ObjectMapper MAPPER = new ObjectMapper();
private static final ObjectMapper MAPPER = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule()); // <--- 注册模块
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // 可选配置
        return mapper;
    }


    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        System.out.println("JsonOutboundEncoder");
        // 1. 仅处理业务层返回的 Java 对象（非 HTTP 消息）
//        System.out.println("ok json0");
        if (!(msg instanceof HttpResponse) && !(msg instanceof HttpContent)) {

            // 2. 序列化为 JSON
            byte[] jsonBytes = MAPPER.writeValueAsBytes(msg);

            // 3. 分配池化内存
            ByteBuf buffer = PooledByteBufAllocator.DEFAULT.buffer(jsonBytes.length);
            buffer.writeBytes(jsonBytes);

            // 4. 构造 FullHttpResponse
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.OK,
                    buffer
            );

            // 5. 设置 JSON 响应头
            response.headers()
                    .set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
                    .setInt(HttpHeaderNames.CONTENT_LENGTH, jsonBytes.length);

            // 6. 替换原始消息为 HTTP 响应
            msg = response;
//            System.out.println("ok json1");
        }
//        System.out.println("ok json2");
        // 7. 传递处理后的消息给下一个出站处理器
        super.write(ctx, msg, promise);
    }
}