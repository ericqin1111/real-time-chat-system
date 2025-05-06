package server.handler.chatroom;

import DTO.MessageDTO;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import io.netty.buffer.Unpooled;
import io.netty.buffer.ByteBuf;
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.ZoneId;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher; // 用于路径匹配
import java.util.regex.Pattern; // 用于路径匹配
import java.util.stream.Collectors;

import config.MyBatisConfig;
import mapper.*;
import entity.*;

import server.GlobalVar; // 假设业务线程池在这里

// @ChannelHandler.Sharable // 如果设计为无状态可共享
public class MessageHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final ExecutorService businessExecutor = GlobalVar.businessExecutor;

    // 定义路径匹配模式 (正则表达式)
    // 匹配 /api/users/{userId}/messages/friend/{friendId} (允许可选的查询参数)
    private static final Pattern FRIEND_MSG_PATTERN = Pattern.compile("^/api/users/(\\d+)/messages/friend/(\\d+)(?:\\?.*)?$");
    // 匹配 /api/users/{userId}/messages/group/{groupId} (允许可选的查询参数)
    private static final Pattern GROUP_MSG_PATTERN = Pattern.compile("^/api/users/(\\d+)/messages/group/(\\d+)(?:\\?.*)?$");

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        HttpHeaders headers = request.headers();
        String uri = request.uri();

        // 只处理 GET 请求
        if (request.method() != HttpMethod.GET) {
            sendErrorResponse(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED, "GET method required.", headers);
            return;
        }

        // --- 尝试匹配好友消息路径 ---
        Matcher friendMatcher = FRIEND_MSG_PATTERN.matcher(uri);
        if (friendMatcher.matches()) {
            try {
                int userId = Integer.parseInt(friendMatcher.group(1)); // 从路径捕获组获取 userId
                System.out.println(userId);
                int friendId = Integer.parseInt(friendMatcher.group(2)); // 从路径捕获组获取 friendId
                System.out.println(friendId);

                // 解析分页参数 (从 Query String)
                QueryStringDecoder decoder = new QueryStringDecoder(uri);
                int limit = getQueryParamAsInt(decoder, "limit", 20); // 默认 20 条
                int offset = getQueryParamAsInt(decoder, "offset", 0);  // 默认从 0 开始

                System.out.println("MessageHandler: Requesting friend messages for user " + userId + ", friend " + friendId);
                handleGetFriendMessages(ctx, headers, userId, friendId, limit, offset);
                return; // 处理完毕

            } catch (NumberFormatException e) {
                sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, "Invalid userId or friendId in path.", headers);
                return;
            }
        }

        // --- 尝试匹配群组消息路径 ---
        Matcher groupMatcher = GROUP_MSG_PATTERN.matcher(uri);
        if (groupMatcher.matches()) {
            try {
                int userId = Integer.parseInt(groupMatcher.group(1));
                int groupId = Integer.parseInt(groupMatcher.group(2));

                QueryStringDecoder decoder = new QueryStringDecoder(uri);
                int limit = getQueryParamAsInt(decoder, "limit", 20);
                int offset = getQueryParamAsInt(decoder, "offset", 0);

                System.out.println("MessageHandler: Requesting group messages for user " + userId + ", group " + groupId);
                handleGetGroupMessages(ctx, headers, userId, groupId, limit, offset);
                return; // 处理完毕

            } catch (NumberFormatException e) {
                sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, "Invalid userId or groupId in path.", headers);
                return;
            }
        }

        // --- 如果路径都不匹配 ---
        // 可以选择传递给下一个 Handler 或返回 404
        // ctx.fireChannelRead(request.retain());
        sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, "Message API endpoint not found.", headers);
    }

    // --- 处理获取好友消息 ---
    private void handleGetFriendMessages(ChannelHandlerContext ctx, HttpHeaders headers, int userId, int friendId, int limit, int offset) {
        businessExecutor.submit(() -> { // 异步执行
            try {
                // 1. 直接调用 Mapper 查询
                List<FriendMessage> messages = MyBatisConfig.executeQuery(FriendMessageMapper.class,
                        m -> m.findMessagesBetweenUsers(userId, friendId, limit, offset)); // 传递分页参数

                System.out.println("METHOD:"+messages);

                // 2. 转换 DTO, 设置 isMe
                List<MessageDTO> messageDTOs = messages.stream().map(msg -> {
                    MessageDTO dto = new MessageDTO();
                    dto.setMessageId(msg.getMessageId());
                    dto.setSenderId(msg.getSenderId());
                    dto.setContent(msg.getContent());
//                    dto.setContentType(msg.getContentType());
//
//                    if(dto.getContentType()==2){
//                        dto.setFileName(msg.getFileName());
//                    }
                    dto.setTime(msg.getSentTime() != null ? msg.getSentTime().atZone(ZoneId.systemDefault()).toInstant().toString() : null);
                    Integer contentTypeFromDb = msg.getContentType(); // 先获取 Integer 类型的值
                    int contentTypeValue = 1; // 设置一个默认值，例如 1 代表文本
                    if (contentTypeFromDb != null) {
                        contentTypeValue = contentTypeFromDb; // 如果数据库值不为 null，则使用它 (自动拆箱)
                    } else {
                        System.out.println("MessageHandler: Message ID " + msg.getMessageId() + " has null content_type. Defaulting to 1 (text).");
                        // 你可以在这里记录更详细的日志或根据业务决定默认值
                    }
                    dto.setContentType(contentTypeValue);
                    dto.setMe(msg.getSenderId().equals(userId)); // **设置 isMe**
                    // TODO: 可选：填充文件信息 (如果 contentType 是文件)
                    // if(dto.getContentType() == 2) { dto.setFileInfo(...); }
                    return dto;
                }).collect(Collectors.toList());

                System.out.println("METHOD DTO:"+messageDTOs);
                // 3. 发送 JSON 响应
                String responseJson = new JSONArray(messageDTOs).toString();

                System.out.println("METHOD JSON:"+responseJson);
                ctx.channel().eventLoop().execute(() -> {
                    sendJsonResponse(ctx, HttpResponseStatus.OK, responseJson, headers);
                });
            } catch (Exception e) {
                System.err.println("MessageHandler: Error fetching friend messages: " + e.getMessage());
                ctx.channel().eventLoop().execute(() -> {
                    sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Failed to get messages.", headers);
                });
            }
        });
    }

    // --- 处理获取群组消息 ---
    private void handleGetGroupMessages(ChannelHandlerContext ctx, HttpHeaders headers, int userId, int groupId, int limit, int offset) {
        businessExecutor.submit(() -> { // 异步执行
            try {
                System.out.println("Group1");
                // 1. (可选但推荐) 验证用户是否是群成员
                Integer count = MyBatisConfig.executeQuery(GroupMemberMapper.class, m -> m.isUserInGroup(userId, groupId));
                boolean isMember = (count != null && count > 0);
                if (!isMember) {
                    System.out.println("Group2");
                    ctx.channel().eventLoop().execute(() -> {
                        sendErrorResponse(ctx, HttpResponseStatus.FORBIDDEN, "Not a member of this group.", headers);
                    });
                    return;
                }

                System.out.println("Group3");
                // 2. 获取群消息
                List<GroupMessage> messages = MyBatisConfig.executeQuery(GroupMessageMapper.class,
                        m -> m.findMessagesByGroupId(groupId, limit, offset)); // 传递分页参数

                System.out.println("GroupMessages:"+messages);
                System.out.println("Group4");
                // 3. 批量获取发送者信息
                List<Integer> senderIds = messages.stream().map(GroupMessage::getSenderId).distinct().collect(Collectors.toList());
                Map<Integer, User> senderMap = Collections.emptyMap();
                System.out.println("GroupMessages:"+senderIds);
                if (!senderIds.isEmpty()) {
                    senderMap = MyBatisConfig.executeQuery(UserMapper.class, m -> m.findUsersByIds(senderIds))
                            .stream().collect(Collectors.toMap(User::getUserId, u -> u));
                }

                System.out.println("Group5");

                // 4. 转换 DTO, 设置 isMe 和发送者信息
                Map<Integer, User> finalSenderMap = senderMap; // Lambda final
                List<MessageDTO> messageDTOs = messages.stream().map(msg -> {
                    MessageDTO dto = new MessageDTO();
                    dto.setMessageId(msg.getMessageId());
                    dto.setSenderId(msg.getSenderId());
                    User senderInfo = finalSenderMap.get(msg.getSenderId());
                    if (senderInfo != null) {
                        dto.setSenderName(senderInfo.getUsername());
                        // TODO: 设置头像 URL
                        // dto.setSenderAvatarUrl(...);
                    } else {
                        dto.setSenderName("User " + msg.getSenderId()); // 兜底
                    }
                    dto.setContent(msg.getContent());
                    dto.setTime(msg.getSentTime() != null ? msg.getSentTime().atZone(ZoneId.systemDefault()).toInstant().toString() : null);
                    dto.setContentType(msg.getContentType());
                    dto.setMe(msg.getSenderId().equals(userId)); // **设置 isMe**
                    // TODO: 可选：填充文件信息
                    return dto;
                }).collect(Collectors.toList());

                System.out.println("Group6");
                // 5. 发送 JSON 响应
                String responseJson = new JSONArray(messageDTOs).toString();
                ctx.channel().eventLoop().execute(() -> {
                    sendJsonResponse(ctx, HttpResponseStatus.OK, responseJson, headers);
                });

            } catch (Exception e) {
                System.err.println("MessageHandler: Error fetching group messages: " + e.getMessage());
                e.printStackTrace();
                ctx.channel().eventLoop().execute(() -> {
                    sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Failed to get messages.", headers);
                });
            }
        });
    }


    // --- 辅助方法 ---

    // 从 URI 解析 ID (简单版本，只取最后一个数字)
    private Integer extractIdFromUri(String uri, String prefix) {
        if (uri.startsWith(prefix)) {
            try {
                String remaining = uri.substring(prefix.length());
                // 去掉可能的查询参数
                if (remaining.contains("?")) {
                    remaining = remaining.substring(0, remaining.indexOf("?"));
                }
                return Integer.parseInt(remaining);
            } catch (Exception e) { /* ... */ }
        }
        return null;
    }

    // 从 QueryStringDecoder 获取整数参数
    private int getQueryParamAsInt(QueryStringDecoder decoder, String key, int defaultValue) {
        List<String> params = decoder.parameters().get(key);
        if (params != null && !params.isEmpty()) {
            try {
                return Integer.parseInt(params.get(0));
            } catch (NumberFormatException e) {
                // Ignore or log error
            }
        }
        return defaultValue;
    }


    // 发送 JSON 响应 (需要确保 CorsUtils.addCorsHeaders 被调用)
    private void sendJsonResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String jsonBody, HttpHeaders requestHeaders) {
        ByteBuf content = Unpooled.copiedBuffer(jsonBody, CharsetUtil.UTF_8);
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, content);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());

        ctx.writeAndFlush(response);
        System.out.println("Sent HTTP Response from MessageHandler: " + status);
    }

    // 发送错误响应
    private void sendErrorResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String message, HttpHeaders requestHeaders) {
        sendJsonResponse(ctx, status, new JSONObject().put("error", message).toString(), requestHeaders);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        if (ctx.channel().isActive()) {
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Internal server error in MessageHandler.", null);
        }
        ctx.close();
    }
}

