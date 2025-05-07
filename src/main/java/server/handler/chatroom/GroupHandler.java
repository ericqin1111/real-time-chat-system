package server.handler.chatroom;

import DTO.ChatListItemDTO;
import DTO.CreateGroupRequestDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import config.MyBatisConfig;
import entity.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import mapper.*;
import org.json.JSONArray;
import org.json.JSONObject;
import server.GlobalVar;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class GroupHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final ExecutorService businessExecutor = GlobalVar.businessExecutor; // 获取业务线程池
    private static final ObjectMapper jsonMapper = new ObjectMapper(); // Jackson ObjectMapper 实例

    // 定义 API 路径常量
    private static final String API_GROUP_CREATE = "/api/group/create"; // 创建群组API

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        HttpHeaders headers = request.headers();
        String uri = request.uri();

        // 仅处理创建群组的POST请求
        if (HttpMethod.POST.equals(request.method()) && API_GROUP_CREATE.equals(uri)) {
            handleCreateGroup(ctx, request); // 处理创建群组的请求
        } else {
            sendNotFound(ctx, headers); // 其他路径返回 404
        }
    }

    private void handleCreateGroup(ChannelHandlerContext ctx, FullHttpRequest request) {
        HttpHeaders headers = request.headers();
        ByteBuf jsonBuf = request.content();
        String jsonStr = jsonBuf.toString(CharsetUtil.UTF_8);

        // 将数据库操作和业务逻辑提交到业务线程池
        businessExecutor.submit(() -> {
            try {
                // 1. 解析请求体 JSON 到 DTO 对象
                CreateGroupRequestDTO createReq = jsonMapper.readValue(jsonStr, CreateGroupRequestDTO.class);

                // 2. 参数校验 (移植自 GroupHandler)
                validateGroupCreationParameters(createReq.getGroupName(), createReq.getUserIds(), createReq.getUsernames());

                // --- 关键业务逻辑：创建群组 ---
                // !!! 事务管理警告 !!!
                // 下面的数据库操作来自原始的 @Transactional 方法。
                // 如果每个 MyBatisConfig.execute 调用都是独立的事务，这里的原子性将无法保证。
                // 你需要根据你的 MyBatisConfig 实现来确保这些操作在一个事务中完成。

                ChatGroup newGroup = new ChatGroup(); // 假设 ChatGroup 是你的群组实体类
                newGroup.setGroupName(createReq.getGroupName());
                newGroup.setMemberCount(createReq.getUserIds().size());

                // 3. 插入群组基本信息并获取生成的群组ID
                // 假设 ChatGroupMapper.insert 方法配置为能将生成的ID回填到 newGroup 对象
                MyBatisConfig.execute(ChatGroupMapper.class, m -> m.insert(newGroup));
                int generatedGroupId = newGroup.getGroupId(); // 依赖 MyBatis 的主键回填

                // 校验是否成功获取群组ID
                if (generatedGroupId == 0) {
                    throw new RuntimeException("创建群组失败：未能获取到群组ID");
                }

                // 4. 初始化群消息统计 (移植自 GroupHandler)
                _initGroupMessageStats(generatedGroupId);

                // 5. 批量添加群成员及初始化已读状态 (移植自 GroupHandler)
                List<Integer> userIds = createReq.getUserIds();
                List<String> usernames = createReq.getUsernames();
                for (int i = 0; i < userIds.size(); i++) {
                    int memberUserId = userIds.get(i);
                    String memberUsername = usernames.get(i);

                    _addGroupMember(generatedGroupId, memberUserId, memberUsername);
                    _initMemberReadStatus(generatedGroupId, memberUserId);
                }
                // --- 事务敏感区域结束 ---

                // 6. 构建成功响应
                JSONObject responsePayload = new JSONObject();
                responsePayload.put("message", "群组创建成功");
                responsePayload.put("groupId", generatedGroupId);

                // 切换回 IO 线程发送响应
                ctx.channel().eventLoop().execute(() -> sendJsonResponse(ctx, HttpResponseStatus.CREATED, responsePayload.toString(), headers));

            } catch (IllegalArgumentException e) { // 参数校验失败
                System.err.println("ChatHandler: 创建群组参数错误: " + e.getMessage());
                ctx.channel().eventLoop().execute(() -> sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, e.getMessage(), headers));
            } catch (Exception e) { // 其他所有异常
                System.err.println("ChatHandler: 创建群组时发生错误: " + e.getMessage());
                e.printStackTrace();
                ctx.channel().eventLoop().execute(() -> sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "创建群组失败", headers));
            }
        });
    }

    // --- 从 GroupHandler 移植过来的辅助方法 ---
    // 这些方法对于创建群组的逻辑是必需的

    private void validateGroupCreationParameters(String groupName, List<Integer> userIds, List<String> usernames) throws IllegalArgumentException {
        if (groupName == null || groupName.trim().isEmpty()) {
            throw new IllegalArgumentException("群名称不能为空");
        }
        if (userIds == null || usernames == null || userIds.size() != usernames.size()) {
            throw new IllegalArgumentException("用户ID列表和用户名列表不匹配或为空");
        }
        if (userIds.isEmpty()) {
            throw new IllegalArgumentException("群组成员不能为空");
        }
    }

    private void _initGroupMessageStats(int groupId) {
        GroupMessageStat stats = new GroupMessageStat(); // 假设 GroupMessageStat 是你的群消息统计实体类
        stats.setGroupId(groupId);
        stats.setTotalMessages(0);
        MyBatisConfig.execute(GroupMessageStatMapper.class, m -> m.insert(stats));
    }

    private void _addGroupMember(int groupId, int userId, String username) {
        GroupMember member = new GroupMember(); // 假设 GroupMember 是你的群成员实体类
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setMemberAlias(username);
        MyBatisConfig.execute(GroupMemberMapper.class, m -> m.insert(member));
    }

    private void _initMemberReadStatus(int groupId, int userId) {
        GroupMemberRead readRecord = new GroupMemberRead(); // 假设 GroupMemberRead 是你的群成员已读记录实体类
        readRecord.setGroupId(groupId);
        readRecord.setUserId(userId);
        readRecord.setReadCount(0);
        MyBatisConfig.execute(GroupMemberReadMapper.class, m -> m.insert(readRecord));
    }

    // --- Netty 响应辅助方法 ---
    private void sendJsonResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String jsonBody, HttpHeaders requestHeaders) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                status,
                Unpooled.copiedBuffer(jsonBody, CharsetUtil.UTF_8)
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());

        ctx.writeAndFlush(response);
        System.out.println("ChatHandler 发送 HTTP 响应: " + status);
    }

    private void sendErrorResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String message, HttpHeaders requestHeaders) {
        JSONObject errorJson = new JSONObject();
        errorJson.put("error", message);
        sendJsonResponse(ctx, status, errorJson.toString(), requestHeaders);
    }

    private void sendNotFound(ChannelHandlerContext ctx, HttpHeaders requestHeaders) {
        sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, "API接口未找到", requestHeaders);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        if (ctx.channel().isActive()) {
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "服务器内部错误", null);
        }
        ctx.close();
    }
}