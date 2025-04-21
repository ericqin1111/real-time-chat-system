package server.handler.general;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.*;
import io.netty.util.AttributeKey;

import java.util.Map;

public class JsonOutboundEncoder extends ChannelOutboundHandlerAdapter {

    private static final ObjectMapper MAPPER = new ObjectMapper();


    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
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