package server.handler.general;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;
import server.GlobalVar;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class HeartbeatAndFrameHandler extends SimpleChannelInboundHandler<BinaryWebSocketFrame> {

    private static final byte HEARTBEAT_FLAG = 0x01;
    private static final byte DATA_FLAG = 0x00;
    private static final byte FRIEND_FLAG = 0x02;
    private static final byte GROUP_FLAG = 0x03;
    private static final byte FRIEND_FILE_FLAG = 0x04;
    private static final byte GROUP_FILE_FLAG = 0x05;

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

    public HeartbeatAndFrameHandler(int maxReadIdle) {
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
            } else if ((frameType == DATA_FLAG) || (frameType == FRIEND_FLAG) || (frameType == GROUP_FLAG)) {
                handleDataFrame(ctx, content, frameType);
            }  else if ((frameType == FRIEND_FILE_FLAG) || (frameType == GROUP_FILE_FLAG)) {
                handleFileFrame(ctx, content, frameType);
            }
            else {
                System.err.println("Unknown frame type: " + frameType);
                String userid =  ctx.channel().attr(GlobalVar.USERID).get();
                GlobalVar.removeUserChannel(userid);
                ctx.close();
            }

    }





    //=============== 私有方法 ===============//
    private void handleReaderIdle(ChannelHandlerContext ctx) {
        int count = readIdleCount.incrementAndGet();
        if (count >= maxReadIdle) {
            System.out.println("读空闲超过阈值，关闭连接: " + ctx.channel());
            String userid =  ctx.channel().attr(GlobalVar.USERID).get();
            GlobalVar.removeUserChannel(userid);
            ctx.close();
        } else {
            System.out.println("读空闲计数: " + count);
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

    private void handleDataFrame(ChannelHandlerContext ctx, ByteBuf content, int frameType) {
        try {

            System.out.println("data get:" + content.toString(CharsetUtil.UTF_8));
            // 读取剩余字节并转换为JSON
            byte[] jsonBytes = ByteBufUtil.getBytes(content);
            Map<String, String> dataMap = objectMapper.readValue(jsonBytes,
                    objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, String.class));
            dataMap.put("type", Integer.toString(frameType));
            // 存入上下文
            ctx.channel().attr(GlobalVar.DATA_CONTEXT).set(dataMap);

            System.out.println("Data stored in context: " + dataMap);
            ctx.fireChannelRead(content.retain());
        } catch (Exception e) {
            System.err.println("JSON parse error: " + e.getMessage());
            ctx.close();
        }
    }
    private void handleFileFrame(ChannelHandlerContext ctx, ByteBuf content, int frameType) {
        try {


        //1.元数据长度
        int metaLen = content.readInt();

        // 2. 读元数据内容
        byte[] metaBytes = new byte[metaLen];
        content.readBytes(metaBytes);
        Map<String, String> dataMap = objectMapper.readValue(metaBytes,
                objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, String.class));
        String metaJson = new String(metaBytes, StandardCharsets.UTF_8);
        dataMap.put("type", Integer.toString(frameType));

        byte[] bytes = new byte[content.readableBytes()];
        content.readBytes(bytes);




        // 生成唯一文件名
            String originalName = dataMap.get("fileName");
            String uniqueName = generateUniqueFileName(originalName);
            dataMap.remove("fileName");
            dataMap.put("fileName", uniqueName);

            //使用线程池来本地化文件，不过如果本地化失败没有进行反馈，待完善。
            GlobalVar.businessExecutor.execute(() ->{
                try {
                    // 保存文件到本地
                    Path uploadPath = Paths.get(GlobalVar.UPLOAD_DIR, uniqueName);
                    Files.createDirectories(uploadPath.getParent());
                    Files.write(uploadPath, bytes);
                }catch (Exception e){
                    e.printStackTrace();
                }
            });


        }catch (Exception e){
            System.err.println("JSON parse error: " + e.getMessage());
        }
    }


    // 生成唯一文件名（时间戳+随机数+原始文件名哈希）
    private String generateUniqueFileName(String originalName) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.valueOf(ThreadLocalRandom.current().nextInt(1000, 9999));
        String hash = Integer.toHexString(originalName.hashCode());
        return timestamp + "_" + random + "_" + hash + getFileExtension(originalName);
    }
    // 获取文件扩展名
    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex == -1) ? "" : filename.substring(dotIndex);
    }

}
