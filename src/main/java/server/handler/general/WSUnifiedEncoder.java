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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WSUnifiedEncoder extends MessageToMessageEncoder<Object> {

    // 预分配业务数据头（零拷贝）
    private static final ByteBuf BUSINESS_HEADER = Unpooled.unreleasableBuffer(
            Unpooled.wrappedBuffer(new byte[]{0x00}).asReadOnly() // 业务类型标识
    );
    private static final ByteBuf FRIMESS_HEADER = Unpooled.unreleasableBuffer(
            Unpooled.wrappedBuffer(new byte[]{0x02}).asReadOnly() // 业务类型标识
    );
    private static final ByteBuf GROUPMESS_HEADER = Unpooled.unreleasableBuffer(
            Unpooled.wrappedBuffer(new byte[]{0x03}).asReadOnly()
    );
    private static final ByteBuf FRIFILE_HEADER = Unpooled.unreleasableBuffer(
            Unpooled.wrappedBuffer(new byte[]{0x04}).asReadOnly()
    );
    private static final ByteBuf GROUPFILE_HEADER = Unpooled.unreleasableBuffer(
            Unpooled.wrappedBuffer(new byte[]{0x05}).asReadOnly()
    );


    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, List<Object> out) {

        System.out.println(msg.getClass().getName());
        if (! (msg instanceof DefaultFullHttpResponse) && ! (msg instanceof BinaryWebSocketFrame)) {
                Map<String, String> map = (Map<String, String>) msg;
                String type = map.get("type");
                byte[] bytes = serializeBusinessObject(msg);
                ByteBuf buf = null;
                if (type.equals("2")){
                     buf = Unpooled.wrappedBuffer(
                            FRIMESS_HEADER.duplicate(),
                            Unpooled.wrappedBuffer(bytes)
                    );
                }else if (type.equals("3")){
                     buf = Unpooled.wrappedBuffer(
                            GROUPMESS_HEADER.duplicate(),
                            Unpooled.wrappedBuffer(bytes)
                    );
                }else if (type.equals("4")){
                    buf = Unpooled.wrappedBuffer(
                            FRIFILE_HEADER.duplicate(),
                            Unpooled.wrappedBuffer(bytes)
                    );
                }else if (type.equals("5")){
                     buf = Unpooled.wrappedBuffer(
                            GROUPFILE_HEADER.duplicate(),
                            Unpooled.wrappedBuffer(bytes)
                    );
                }else if (type.equals("0")){
                   buf = Unpooled.wrappedBuffer(
                            BUSINESS_HEADER.duplicate(),
                            Unpooled.wrappedBuffer(bytes)
                    );
                }


                out.add(ReferenceCountUtil.retain(new BinaryWebSocketFrame(buf)));
        }else {

            out.add(ReferenceCountUtil.retain(msg));
        }
        System.out.println("OKKKKK");

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
