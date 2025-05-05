package server.handler.chatroom;


import io.netty.buffer.ByteBuf;
import DTO.ChatListItemDTO;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import io.netty.buffer.Unpooled;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;

// 导入你的工具、配置、Mapper、实体和 DTO

// import server.handler.utils.JwtUtil; // 不需要认证
import config.MyBatisConfig;
import mapper.*;
import entity.*;

import server.GlobalVar; // 假设业务线程池在这里

// @ChannelHandler.Sharable // 如果设计为无状态可共享
public class ChatHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final ExecutorService businessExecutor = GlobalVar.businessExecutor; // 获取业务线程池

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        HttpHeaders headers = request.headers();
        String uri = request.uri();

        // 1. 校验方法和路径前缀 (可选，取决于你的 RouterHandler 是否已做)
//        if (request.method() != HttpMethod.GET || !uri.startsWith("/api/users/")) {
//            sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, "Invalid request for ChatHandler.", headers);
//            return;
//        }

        // 2. 从 URI 中解析 userId

        int userId=Integer.parseInt(ctx.channel().attr(GlobalVar.USERID).get());
        System.out.println("ChatHandler: Requesting chat list for user ID: " + userId);

        // 3. 异步执行获取聊天列表的逻辑
        businessExecutor.submit(() -> {
            try {
                // 4. 调用 Mappers 获取数据
                List<ChatListItemDTO> chatList = new ArrayList<>();

                // === 获取好友列表 ===
                List<Integer> friendIds = MyBatisConfig.executeQuery(UserFriendMapper.class, m -> m.findFriendIdsByUserIds(userId));
                if (friendIds != null && !friendIds.isEmpty()) {
                    List<User> friends = MyBatisConfig.executeQuery(UserMapper.class, m -> m.findUsersByIds(friendIds));
                    for (User friend : friends) {
                        ChatListItemDTO dto = new ChatListItemDTO();
                        dto.setId(friend.getUserId()); // 好友聊天的 ID 是好友的 userId
                        dto.setName(friend.getUsername()); // 好友的用户名
                        dto.setType("friend"); // 类型是好友
                        // TODO: (可选) 查询与该好友的最后一条消息和时间
                        FriendMessage lastMsg = MyBatisConfig.executeQuery(FriendMessageMapper.class, m -> m.findLastMessageBetweenUsers(userId, friend.getUserId()));
                        if (lastMsg != null) {
                            dto.setLastMessageContent(truncate(lastMsg.getContent(), 30));
                            dto.setLastMessageTime(formatTimestampForList(lastMsg.getSentTime()));
                            dto.setTimestampForSort(lastMsg.getSentTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
                        } else {
                            dto.setTimestampForSort(0L); // 没有消息则时间戳为0
                        }
                        // TODO: (可选) 查询未读消息数
                        // int unread = MyBatisConfig.execute(FriendMessageStatsMapper.class, m->m.getUnreadCount(userId, friend.getUserId()));
                        // dto.setUnreadCount(unread);
                        dto.setUnreadCount(0); // 暂设为0
                        // TODO: (可选) 获取好友头像 URL
                        // dto.setAvatarUrl(...);
                        chatList.add(dto);
                    }
                }

                // === 获取群组列表 ===
                List<Integer> groupIds = MyBatisConfig.executeQuery(GroupMemberMapper.class, m -> m.findGroupIdsByUserId(userId));
                if (groupIds != null && !groupIds.isEmpty()) {
                    List<ChatGroup> groups = MyBatisConfig.executeQuery(ChatGroupMapper.class, m -> m.findGroupsByIds(groupIds));
                    for (ChatGroup group : groups) {
                        ChatListItemDTO dto = new ChatListItemDTO();
                        dto.setId(group.getGroupId()); // 群聊的 ID 是群组的 groupId
                        dto.setName(group.getGroupName()); // 群名称
                        dto.setType("group"); // 类型是群组
                        // TODO: (可选) 获取群头像 URL
                        // dto.setAvatarUrl(...);

                        // TODO: (可选) 查询群的最后一条消息和时间
                        GroupMessage lastMsg = MyBatisConfig.executeQuery(GroupMessageMapper.class, m -> m.findLastMessageInGroup(group.getGroupId()));
                        if (lastMsg != null) {
                            // TODO: 可能需要查询发送者名字用于预览
                            dto.setLastMessageContent(truncate(lastMsg.getContent(), 30));
                            dto.setLastMessageTime(formatTimestampForList(lastMsg.getSentTime()));
                            dto.setTimestampForSort(lastMsg.getSentTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
                        } else {
                            dto.setTimestampForSort(0L);
                        }
                        // TODO: (可选) 查询该用户在此群的未读消息数
                        // int total = ...; int read = ...; dto.setUnreadCount(total - read);
                        dto.setUnreadCount(0); // 暂设为0
                        chatList.add(dto);
                    }
                }

                // 5. 排序聊天列表 (按最后消息时间倒序，无消息的排在后面)
                chatList.sort(Comparator.comparing(ChatListItemDTO::getTimestampForSort, Comparator.reverseOrder()));

                // 6. 序列化并发送 JSON 响应
                String responseJson = new JSONArray(chatList).toString();
                // 切换回 IO 线程发送
                ctx.channel().eventLoop().execute(() -> {
                    sendJsonResponse(ctx, HttpResponseStatus.OK, responseJson, headers);
                });

            } catch (Exception e) {
                System.err.println("ChatHandler: Error fetching chat list for user " + userId + ": " + e.getMessage());
                e.printStackTrace();
                ctx.channel().eventLoop().execute(() -> {
                    sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Failed to get chat list.", headers);
                });
            }
        });
    }

    // --- 辅助方法 ---

    // 修改: 不再需要 authenticate，直接从路径解析 userId
    // private Integer authenticate(HttpHeaders headers) { ... }

    // 从 URI 解析 userId (示例，你需要根据你的确切路径调整)
    // 例如解析 /api/users/123/chats -> 123
//    private Integer extractUserIdFromChatUri(String uri) {
//        try {
//            // 假设路径格式固定为 /users/{userId}/chats
//            String prefix = "/users/";
//            String suffix = "/chats";
//            if (uri.startsWith(prefix) && uri.endsWith(suffix)) {
//                // 提取中间的 ID 部分
//                String idStr = uri.substring(prefix.length(), uri.length() - suffix.length());
//                return Integer.parseInt(idStr);
//            }
//            return null; // 格式不匹配
//        } catch (Exception e) {
//            System.err.println("ChatHandler: Failed to parse userId from URI: " + uri + " - " + e.getMessage());
//            return null; // 解析失败
//        }
//    }


    private void sendJsonResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String jsonBody, HttpHeaders requestHeaders) {
        ByteBuf content = Unpooled.copiedBuffer(jsonBody, CharsetUtil.UTF_8);
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.wrappedBuffer(jsonBody.getBytes(CharsetUtil.UTF_8))
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());

        ctx.writeAndFlush(response);
        System.out.println("Sent HTTP Response from ChatHandler: " + status);
    }

    // 发送错误响应
    private void sendErrorResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String message, HttpHeaders requestHeaders) {
        sendJsonResponse(ctx, status, new JSONObject().put("error", message).toString(), requestHeaders);
    }

    // 发送 404 响应
    private void sendNotFound(ChannelHandlerContext ctx, HttpHeaders requestHeaders) {
        sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, "API endpoint not found.", requestHeaders);
    }

    // 简单的字符串截断方法
    private String truncate(String str, int maxLength) {  if (str == null || str.isEmpty()) {
        return ""; // 返回空字符串或 null，根据你的需要
    }

        // 处理无效的最大长度输入
        if (maxLength <= 0) {
            return ""; // 无法截断到 0 或负数长度
        }

        // 如果字符串本身未超过最大长度，直接返回
        if (str.length() <= maxLength) {
            return str;
        } else {
            // 否则，截取前 maxLength 个字符，并附加省略号
            return str.substring(0, maxLength) + "...";
        } }

    // 简单的时间格式化方法
    private String formatTimestampForList(LocalDateTime ldt) {
        if (ldt == null) return ""; // 处理 null 输入

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalDate messageDate = ldt.toLocalDate();

        // 定义不同的时间格式化器
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        // 根据需要调整日期格式，例如 MM/dd 或 yyyy/MM/dd
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd"); // 例如

        if (messageDate.equals(today)) {
            // 今天：只显示 HH:mm
            return ldt.format(timeFormatter);
        } else if (messageDate.equals(today.minusDays(1))) {
            // 昨天：显示 昨天 HH:mm
            return "昨天 " + ldt.format(timeFormatter);
        }
        // 你可以根据需要添加“本周”的逻辑
        // else if (messageDate.isAfter(today.minusWeeks(1))) { ... }
        else {
            // 更早：显示日期 YYYY/MM/DD (或者包含时间，根据你的选择)
            return messageDate.format(dateFormatter); // 只显示日期
            // 或者 return ldt.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")); // 显示日期和时间
        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        if (ctx.channel().isActive()) {
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Internal server error in ChatHandler.", null);
        }
        ctx.close();
    }
}