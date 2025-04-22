package server.handler.general;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.ReferenceCountUtil;

import java.nio.ByteBuffer;
import java.util.List;

public class WSUnifiedEncoder extends MessageToMessageEncoder<Object> {

    // 预分配业务数据头（零拷贝）
    private static final ByteBuf BUSINESS_HEADER = Unpooled.unreleasableBuffer(
            Unpooled.wrappedBuffer(new byte[]{0x00}).asReadOnly() // 业务类型标识
    );

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, List<Object> out) {

        System.out.println(msg.getClass().getName());
        if (! (msg instanceof DefaultFullHttpResponse) && ! (msg instanceof BinaryWebSocketFrame)) {
                byte[] bytes = serializeBusinessObject(msg);
                ByteBuf buf = Unpooled.wrappedBuffer(
                        BUSINESS_HEADER.duplicate(),
                        Unpooled.wrappedBuffer(bytes)
                );
                out.add(new BinaryWebSocketFrame(buf));
        }else {
            out.add(ReferenceCountUtil.retain(msg));
        }

    }

    private byte[] serializeBusinessObject(Object obj) {
        // 使用JSON序列化（示例）
        try {
            return MAPPER.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化失败", e);
        }
    }
}
