package server.handler.general;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.GlobalEventExecutor;
import jakarta.websocket.PongMessage;
import org.mybatis.logging.Logger;
import org.mybatis.logging.LoggerFactory;
import server.GlobalVar;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class HeartbeatHandler extends SimpleChannelInboundHandler<BinaryWebSocketFrame> {

    private static final byte HEARTBEAT_FLAG = 0x01;
    private static final byte DATA_FLAG = 0x00;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 使用直接内存分配（零拷贝）
    private static final ByteBuf HEARTBEAT_BUF = Unpooled.unreleasableBuffer(
            Unpooled.directBuffer(1)
                    .writeByte(0x01)  // 类型标识
                    .asReadOnly()     // 设置为只读
    );

    // 获取心跳帧（线程安全）
     static BinaryWebSocketFrame getHeartbeatFrame() {
        // 更新时间戳（直接操作底层内存）
        return new BinaryWebSocketFrame(HEARTBEAT_BUF.duplicate());
    }


    // 读空闲超时次数计数器
    private final AtomicInteger readIdleCount = new AtomicInteger(0);
    private final int maxReadIdle; // 允许的最大读空闲次数

    public HeartbeatHandler(int maxReadIdle) {
        this.maxReadIdle = maxReadIdle;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        // 处理空闲事件
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            switch (event.state()) {
                case READER_IDLE:
                    handleReaderIdle(ctx);
                    break;
                case WRITER_IDLE:
                    handleWriterIdle(ctx);
                    break;
                case ALL_IDLE:
                    handleAllIdle(ctx);
                    break;
            }
        } else {
            // 传递其他事件给后续处理器
            ctx.fireUserEventTriggered(evt);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, BinaryWebSocketFrame frame) throws Exception {
        ByteBuf content = frame.content();

        try {
            // 检查最小数据长度
            if (content.readableBytes() < 1) {
                System.err.println("Invalid frame length");
                ctx.close();
                return;
            }

            // 读取帧类型标识
            byte frameType = content.readByte();
            System.out.println("frameatext:" + frameType);
            if (frameType == HEARTBEAT_FLAG) {
                handleHeartbeat(ctx, content);
            } else if (frameType == DATA_FLAG) {

                handleDataFrame(ctx, content);
            } else {
                System.err.println("Unknown frame type: " + frameType);
                ctx.close();
            }
        } finally {
            // 释放ByteBuf资源
            frame.release();
        }
    }





    //=============== 私有方法 ===============//
    private void handleReaderIdle(ChannelHandlerContext ctx) {
        int count = readIdleCount.incrementAndGet();
        if (count >= maxReadIdle) {
            System.out.println("读空闲超过阈值，关闭连接: " + ctx.channel());
            ctx.close();
        } else {
            System.out.println("读空闲计数: " + count);
            System.out.println();
            System.out.println("当前 Pipeline 处理器链: " + ctx.pipeline().names());
            ctx.pipeline().writeAndFlush(getHeartbeatFrame());
        }
    }

    private void handleWriterIdle(ChannelHandlerContext ctx) {
        System.out.println("发送心跳请求...");
    }

    private void handleAllIdle(ChannelHandlerContext ctx) {
        System.out.println("全部空闲事件触发");
    }





    private void handleHeartbeat(ChannelHandlerContext ctx, ByteBuf content) {
        readIdleCount.set(0); // 收到心跳响应重置计数器
        System.out.println("收到心跳响应");
    }

    private void handleDataFrame(ChannelHandlerContext ctx, ByteBuf content) {
        try {

            System.out.println("data get:" + content.toString(CharsetUtil.UTF_8));
            // 读取剩余字节并转换为JSON
            byte[] jsonBytes = ByteBufUtil.getBytes(content);
            Map<String, String> dataMap = objectMapper.readValue(jsonBytes,
                    objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, String.class));

            // 存入上下文
            ctx.channel().attr(GlobalVar.DATA_CONTEXT).set(dataMap);

            System.out.println("Data stored in context: " + dataMap);
            ctx.fireChannelRead(content);
        } catch (Exception e) {
            System.err.println("JSON parse error: " + e.getMessage());
            ctx.close();
        }
    }

}
