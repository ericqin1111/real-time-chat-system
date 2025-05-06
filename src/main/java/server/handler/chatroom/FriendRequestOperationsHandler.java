package server.handler.chatroom; // 你的包名

import config.MyBatisConfig;
import entity.FriendRequest;
import entity.User;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import mapper.FriendRequestMapper;
import mapper.UserMapper;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.fasterxml.jackson.databind.ObjectMapper; // 使用Jackson解析请求体
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import server.GlobalVar;

import static server.GlobalVar.businessExecutor;


public class FriendRequestOperationsHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static class SendFriendRequestPayload {
        public String targetUserId; // 前端传来的目标用户ID (字符串形式)
        public String requesterName; // 发起请求的用户的名字
    }

    private static final Logger logger = LoggerFactory.getLogger(FriendRequestOperationsHandler.class);

    private final ObjectMapper jacksonObjectMapper; // 用于解析请求体

    // API 路径模式
    // GET /api/users/{userId}/friend-requests/pending
    private static final Pattern GET_PENDING_REQUESTS_PATTERN = Pattern.compile("^/api/social/(\\d+)/friend-requests/pending(?:\\?.*)?$");
    // POST /api/social/{userId}/friend-requests/send
    private static final Pattern SEND_REQUEST_PATTERN = Pattern.compile("^/api/social/(\\d+)/friend-requests/send(?:\\?.*)?$");
    // POST /api/social/{userId}/friend-requests/{requestId}/accept
    private static final Pattern ACCEPT_REQUEST_PATTERN = Pattern.compile("^/api/social/(\\d+)/friend-requests/(\\d+)/accept(?:\\?.*)?$");
    // POST /api/social/{userId}/friend-requests/{requestId}/decline
    private static final Pattern DECLINE_REQUEST_PATTERN = Pattern.compile("^/api/social/(\\d+)/friend-requests/(\\d+)/decline(?:\\?.*)?$");



    private static class AcceptFriendRequestPayload {
        public String aliasName; // 当前用户想给请求者设置的备注名
    }





    // 发送 404 响应
    private void sendNotFound(ChannelHandlerContext ctx, HttpHeaders requestHeaders) {
        sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, "API endpoint not found.", requestHeaders);
    }



    public FriendRequestOperationsHandler() {
        this.jacksonObjectMapper = new ObjectMapper();
        this.jacksonObjectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        String uri = request.uri();
        HttpMethod method = request.method();
        HttpHeaders headers = request.headers();

        Matcher matcher;

        // 使用正则表达式匹配并分发请求到对应方法
        if (method.equals(HttpMethod.GET)) {
            matcher = GET_PENDING_REQUESTS_PATTERN.matcher(uri);
            if (matcher.matches()) {
                try {
                    int currentUserId = Integer.parseInt(matcher.group(1));
                    handleGetPendingRequests(ctx, headers, currentUserId); // 使用 MybatisConfig.executeQuery
                } catch (NumberFormatException e) { sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, "路径用户ID无效", headers); }
                return;
            }
        } else if (method.equals(HttpMethod.POST)) {
            System.out.println("1111111111111111111111111111");
            matcher = SEND_REQUEST_PATTERN.matcher(uri);
            if (matcher.matches()) {
                try {
                    System.out.println("aaaaaaaaaaaaaaa");
                    int currentUserId = Integer.parseInt(matcher.group(1));
                    handleSendNewRequest(ctx, headers, request, currentUserId); // 需要手动事务
                } catch (NumberFormatException e) { sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, "路径用户ID无效", headers); }
                return;
            }
            System.out.println("222222222222222222222");
            matcher = ACCEPT_REQUEST_PATTERN.matcher(uri);
            if (matcher.matches()) {
                try {
                    int currentUserId = Integer.parseInt(matcher.group(1));
                    int requestId = Integer.parseInt(matcher.group(2));
                    handleAcceptRequest(ctx, headers, request, requestId, currentUserId); // 需要手动事务
                } catch (NumberFormatException e) { sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, "路径ID无效", headers); }
                return;
            }
            System.out.println("333333333333333333");
            matcher = DECLINE_REQUEST_PATTERN.matcher(uri);
            if (matcher.matches()) {
                try {
                    int currentUserId = Integer.parseInt(matcher.group(1));
                    int requestId = Integer.parseInt(matcher.group(2));
                    handleDeclineRequest(ctx, headers, requestId, currentUserId); // 需要手动事务
                } catch (NumberFormatException e) { sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, "路径ID无效", headers); }
                return;
            }
        }

        logger.debug("未匹配的FriendRequestOperationsHandler请求: {} {}", method, uri);
        sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND, "请求的API端点未找到", headers);
    }

    // --- 使用 MyBatisConfig.executeQuery 的方法 ---
    private void handleGetPendingRequests(ChannelHandlerContext ctx, HttpHeaders headers, int currentUserId) {
        businessExecutor.submit(() -> {
            try {
                logger.info("处理用户 {} 的待处理好友请求获取...", currentUserId);
                // 直接使用你的 MyBatisConfig 工具类执行查询
                List<FriendRequest> requests = MyBatisConfig.executeQuery(
                        FriendRequestMapper.class, // Mapper 接口
                        mapper -> mapper.findPendingRequestsForUser(currentUserId) // 执行方法
                );
                requests = (requests == null) ? Collections.emptyList() : requests;

                logger.info("成功获取用户 {} 的 {} 条好友请求", currentUserId, requests.size());
                // 直接写入结果对象，依赖 Pipeline 中的 JsonOutboundEncoder 序列化
                ctx.writeAndFlush(requests);

            } catch (Exception e) {
                logger.error("获取用户 {} 的好友请求失败: {}", currentUserId, e.getMessage(), e);
                ctx.channel().eventLoop().execute(() -> sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "获取好友请求失败", headers));
            }
        });
    }

    // --- 需要手动事务管理的方法 ---

    // **你需要确保你的 MyBatisConfig 类提供了获取 SqlSessionFactory 的方法**
    // **或者你可以创建之前示例中的 MyBatisUtil 类**
    private SqlSessionFactory getSessionFactory() {
        // 这里假设你的 MyBatisConfig 有一个静态方法 getSqlSessionFactory()
        // 如果你的类名或方法名不同，请替换！
        // 如果 MyBatisConfig 没有，你需要添加这个功能，或者创建 MyBatisUtil
        return MyBatisConfig.getSqlSessionFactory();
        // 或者 return MyBatisUtil.getSqlSessionFactory(); (如果你创建了MyBatisUtil)
    }

//    private void handleSendNewRequest(ChannelHandlerContext ctx, HttpHeaders headers, FullHttpRequest httpRequest, int currentUserId) {
//        businessExecutor.submit(() -> {
//            System.out.println("4444444444444");
//            String requestBody = null;
//
//            String requestBody = httpRequest.content().toString(StandardCharsets.UTF_8);
//            SendFriendRequestPayload payload;
//            try {
//                payload = jacksonObjectMapper.readValue(requestBody, SendFriendRequestPayload.class);
//            } catch (Exception e) { /* ... 错误处理 ... */ return; }
//            System.out.println("bbbbbbbbbbb");
//            if (payload.targetUserId == null  ) { /* ... 错误处理 ... */ return; }
//            if (payload.requesterName == null  ) { /* ... 错误处理 ... */ return; }
//            System.out.println("ccccccccccc");
//
//
//
//
//            Integer targetId;
//            try { targetId = Integer.parseInt(payload.targetUserId); }
//            catch (NumberFormatException e) { /* ... 错误处理 ... */ return; }
//            System.out.println("dddddddddddd");
//            if (currentUserId == targetId) { /* ... 错误处理 ... */ return; }
//            System.out.println("eeeeeeeeee");
//
//            // 手动管理 SqlSession 和事务
//            SqlSession sqlSession = getSessionFactory().openSession(true); // autoCommit=true 因为是单条插入
//            try {
//                FriendRequestMapper frMapper = sqlSession.getMapper(FriendRequestMapper.class);
//                // (可选) 校验 targetId 是否存在
//                // (可选) 检查是否已存在请求或已是好友
//                System.out.println("fffffff");
//                FriendRequest newRequest = new FriendRequest();
//                newRequest.setRequesterId(currentUserId);
//                newRequest.setTargetId(targetId);
//                newRequest.setRequesterName(payload.requesterName);
//                newRequest.setRequestType(0); // 0 = pending
//                System.out.println("ggggggggg");
//                frMapper.insertFriendRequest(newRequest);
//                // sqlSession.commit(); // 如果 openSession(true) 则不需要
//                System.out.println("hhhhhhhhhhh");
//                logger.info("用户 {}向用户 {} 发送了好友请求", currentUserId, targetId);
//                sendJsonResponse(ctx, HttpResponseStatus.CREATED, Map.of("success", true, "message", "好友请求已发送"), headers);
//
//            } catch (Exception e) {
//                // sqlSession.rollback(); // 如果 autoCommit=true, rollback 无效
//                logger.error("发送好友请求失败 ({} -> {}): {}", currentUserId, targetId, e.getMessage(), e);
//                ctx.channel().eventLoop().execute(() -> sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "发送好友请求失败", headers));
//            } finally {
//                if (sqlSession != null) {
//                    sqlSession.close(); // 必须关闭 Session
//                }
//            }
//        });

//        businessExecutor.submit(() -> {
//            logger.info("==> 开始处理发送好友请求: 操作用户={}, 目标路径信息={}", currentUserId, httpRequest.uri());
//
//            String requestBody = null;
//            SendFriendRequestPayload payload = null;
//            Integer targetId = null; // 声明在try外部以便finally可用
//
//            // --- 第一步：安全地读取和解析请求体 ---
//            try {
//                logger.debug("步骤 1/5: 读取请求体...");
//                if (httpRequest == null) {
//                    throw new IllegalStateException("Internal Error: FullHttpRequest object is null!");
//                }
//                ByteBuf content = httpRequest.content();
//                if (content == null) {
//                    throw new IllegalStateException("Internal Error: FullHttpRequest content is null!");
//                }
//                logger.debug("请求体 ByteBuf 类型: {}, 引用计数: {}, 可读字节: {}",
//                        content.getClass().getSimpleName(), content.refCnt(), content.readableBytes());
//
//                if (!content.isReadable()) {
//                    logger.warn("请求体内容为空。");
//                    requestBody = ""; // 视为空字符串，后续解析会失败并报错
//                } else {
//                    requestBody = content.toString(StandardCharsets.UTF_8);
//                    logger.debug("成功读取请求体内容。");
//                }
//
//                // --- 第二步：解析 JSON Payload ---
//                logger.debug("步骤 2/5: 解析 JSON 请求体...");
//                if (requestBody.isEmpty()) {
//                    logger.warn("请求体为空，无法解析 SendFriendRequestPayload。");
//                    ctx.channel().eventLoop().execute(() -> sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, "请求体不能为空", headers));
//                    return; // 结束任务
//                }
//                payload = jacksonObjectMapper.readValue(requestBody, SendFriendRequestPayload.class);
//                logger.debug("成功解析 JSON Payload。");
//
//                // --- 第三步：校验 Payload 数据 ---
//                logger.debug("步骤 3/5: 校验 Payload 数据...");
//                if (payload.targetUserId == null || payload.targetUserId.isEmpty()) {
//                    logger.warn("目标用户ID (targetUserId) 为空。");
//                    ctx.channel().eventLoop().execute(() -> sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, "目标用户ID不能为空", headers));
//                    return;
//                }
//                if (payload.requesterName == null || payload.requesterName.isEmpty()){
//                    logger.warn("请求者名称 (requesterName) 为空。");
//                    ctx.channel().eventLoop().execute(() -> sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, "请求者名称不能为空", headers));
//                    return;
//                }
//                logger.debug("Payload 数据基本校验通过。");
//
//                // --- 第四步：解析目标ID并进行业务校验 ---
//                logger.debug("步骤 4/5: 解析目标ID并进行业务校验...");
//                try {
//                    targetId = Integer.parseInt(payload.targetUserId);
//                } catch (NumberFormatException e) {
//                    logger.warn("目标用户ID格式无效 (应为数字): {}", payload.targetUserId);
//                    ctx.channel().eventLoop().execute(() -> sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, "目标用户ID格式无效", headers));
//                    return;
//                }
//
//                if (currentUserId == targetId.intValue()) {
//                    logger.warn("用户 {} 尝试添加自己为好友。", currentUserId);
//                    ctx.channel().eventLoop().execute(() -> sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, "不能添加自己为好友", headers));
//                    return;
//                }
//                logger.debug("目标ID解析成功: {}", targetId);
//                // (在这里可以添加检查目标用户是否存在、是否已经是好友、是否已发送请求等逻辑)
//
//            } catch (Exception e) {
//                // 捕获读取、解析、校验阶段的任何异常
//                logger.error("处理发送好友请求失败 (阶段 1-4): 用户={}, 请求体='{}', 错误: {}", currentUserId, requestBody, e.getMessage(), e);
//                ctx.channel().eventLoop().execute(() -> sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, "请求处理失败: " + e.getMessage(), headers));
//                return; // 出错，结束任务
//            }
//
//
//            // --- 第五步：数据库操作 ---
//            logger.debug("步骤 5/5: 执行数据库操作...");
//            SqlSession sqlSession = null;
//            try {
//                // 使用自动提交的 Session，因为这里只有一个 INSERT 操作
//                sqlSession = getSessionFactory().openSession(true);
//                FriendRequestMapper frMapper = sqlSession.getMapper(FriendRequestMapper.class);
//
//                FriendRequest newRequest = new FriendRequest();
//                newRequest.setRequesterId(currentUserId);
//                newRequest.setTargetId(targetId);
//                newRequest.setRequesterName(payload.requesterName);
//                newRequest.setRequestType(0); // 0 = pending
//                // newRequest.setCreatedAt(...); // 如果不由MyBatis FieldFill处理
//
//                logger.debug("即将调用 frMapper.insertFriendRequest: {}", newRequest);
//                int insertedRows = frMapper.insertFriendRequest(newRequest);
//                logger.debug("调用 frMapper.insertFriendRequest 完成, 影响行数: {}", insertedRows);
//
//                // 检查插入是否成功 (通常MyBatis配置为失败时抛异常，但也可以检查行数)
//                if (insertedRows > 0) {
//                    logger.info("用户 {} 向用户 {} 成功发送了好友请求", currentUserId, targetId);
//                    // 使用辅助方法发送成功响应
//                    sendJsonResponse(ctx, HttpResponseStatus.CREATED, Map.of("success", true, "message", "好友请求已发送"), headers);
//                } else {
//                    // 这种情况比较少见，除非有特殊约束或并发问题
//                    logger.error("发送好友请求后，数据库影响行数为0 ({} -> {})", currentUserId, targetId);
//                    ctx.channel().eventLoop().execute(() -> sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "发送好友请求失败(数据库未更新)", headers));
//                }
//
//            } catch (Exception e) {
//                // 捕获数据库操作异常
//                logger.error("发送好友请求的数据库操作失败 ({} -> {}): {}", currentUserId, targetId, e.getMessage(), e);
//                // 告知客户端错误
//                ctx.channel().eventLoop().execute(() -> sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "发送好友请求失败(数据库操作错误)", headers));
//            } finally {
//                if (sqlSession != null) {
//                    sqlSession.close(); // 无论如何都要关闭 Session
//                    logger.debug("数据库 Session 已关闭 (发送好友请求操作)。");
//                }
//            }
//        }); // businessExecutor.submit 结束
//    }

    private void handleSendNewRequest(ChannelHandlerContext ctx, HttpHeaders headers, FullHttpRequest httpRequest, int currentUserId) {
        // --- 第一步：在 IO 线程同步读取和解析请求体 ---
        String requestBody = null;
        SendFriendRequestPayload payload = null;
        try {
            logger.debug("步骤 1/5: 读取并解析请求体 (同步)...");
            ByteBuf content = httpRequest.content();
            if (content == null || !content.isReadable()) {
                logger.warn("请求体内容为空或不可读。");
                // 对于发送好友请求，通常 Body 是必须的
                sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, "请求体不能为空", headers);
                return; // 不再继续
            }
            requestBody = content.toString(StandardCharsets.UTF_8);
            logger.debug("成功读取请求体内容。");

            payload = jacksonObjectMapper.readValue(requestBody, SendFriendRequestPayload.class);
            logger.debug("成功解析 JSON Payload。");

            // --- 第二步：校验 Payload 数据 (同步) ---
            logger.debug("步骤 2/5: 校验 Payload 数据...");
            if (payload.targetUserId == null || payload.targetUserId.isEmpty()) {
                logger.warn("目标用户ID (targetUserId) 为空。");
                sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, "目标用户ID不能为空", headers);
                return;
            }
            if (payload.requesterName == null || payload.requesterName.isEmpty()){
                logger.warn("请求者名称 (requesterName) 为空。");
                sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, "请求者名称不能为空", headers);
                return;
            }
            logger.debug("Payload 数据基本校验通过。");

            // --- 第三步：解析目标ID并进行初步业务校验 (同步) ---
            logger.debug("步骤 3/5: 解析目标ID并进行业务校验...");
            Integer targetId;
            try {
                targetId = Integer.parseInt(payload.targetUserId);
            } catch (NumberFormatException e) {
                logger.warn("目标用户ID格式无效 (应为数字): {}", payload.targetUserId);
                sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, "目标用户ID格式无效", headers);
                return;
            }

            if (currentUserId == targetId.intValue()) {
                logger.warn("用户 {} 尝试添加自己为好友。", currentUserId);
                sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, "不能添加自己为好友", headers);
                return;
            }
            logger.debug("目标ID解析成功: {}", targetId);

            // --- 第四步：将数据库操作提交到业务线程池 ---
            // **注意：现在传递给线程池的是解析好的 payload 和 targetId，而不是整个 httpRequest**
            final SendFriendRequestPayload finalPayload = payload; // Lambda 需要 final 或 effectively final
            final Integer finalTargetId = targetId;

            businessExecutor.submit(() -> {
                logger.info("==> 开始处理发送好友请求 (数据库操作): 操作用户={}, 目标用户={}", currentUserId, finalTargetId);
                logger.debug("步骤 4/5: 执行数据库操作...");
                SqlSession sqlSession = null;
                try {
                    sqlSession = getSessionFactory().openSession(true); // autoCommit true
                    FriendRequestMapper frMapper = sqlSession.getMapper(FriendRequestMapper.class);

                    // (可选) 校验 targetId 是否存在
                    // (可选) 检查是否已存在请求或已是好友

                    FriendRequest newRequest = new FriendRequest();
                    newRequest.setRequesterId(currentUserId);
                    newRequest.setTargetId(finalTargetId);
                    newRequest.setRequesterName(finalPayload.requesterName); // 使用 finalPayload
                    newRequest.setRequestType(0); // 0 = pending

                    logger.debug("即将调用 frMapper.insertFriendRequest: {}", newRequest);
                    int insertedRows = frMapper.insertFriendRequest(newRequest);
                    logger.debug("调用 frMapper.insertFriendRequest 完成, 影响行数: {}", insertedRows);

                    if (insertedRows > 0) {
                        logger.info("用户 {} 向用户 {} 成功发送了好友请求", currentUserId, finalTargetId);
                        // 在业务线程中发送响应需要切换回 IO 线程
                        ctx.channel().eventLoop().execute(() -> sendJsonResponse(ctx, HttpResponseStatus.CREATED, Map.of("success", true, "message", "好友请求已发送"), headers));
                    } else {
                        logger.error("发送好友请求后，数据库影响行数为0 ({} -> {})", currentUserId, finalTargetId);
                        ctx.channel().eventLoop().execute(() -> sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "发送好友请求失败(数据库未更新)", headers));
                    }

                } catch (Exception e) {
                    logger.error("发送好友请求的数据库操作失败 ({} -> {}): {}", currentUserId, finalTargetId, e.getMessage(), e);
                    ctx.channel().eventLoop().execute(() -> sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "发送好友请求失败(数据库操作错误)", headers));
                } finally {
                    if (sqlSession != null) {
                        sqlSession.close();
                        logger.debug("数据库 Session 已关闭 (发送好友请求操作)。");
                    }
                }
            }); // businessExecutor.submit 结束

        } catch (Exception e) {
            // 捕获读取、解析、校验阶段的同步异常
            logger.error("处理发送好友请求失败 (准备阶段): 用户={}, 错误: {}", currentUserId, e.getMessage(), e);
            sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, "请求处理失败: " + e.getMessage(), headers);
        }
    }



    // 内部 DTO 只需要 targetUserId 了


//    private void handleAcceptRequest(ChannelHandlerContext ctx, HttpHeaders headers, FullHttpRequest httpRequest, Integer requestId, Integer currentUserId) {
//
//        // --- 1. 同步读取和解析可选的请求体 (获取 aliasName) ---
//        String aliasForRequesterByCurrentUser = null; // 当前用户(target)想给请求者(requester)设置的备注名
//        try {
//            if (httpRequest.content().isReadable() && httpRequest.content().readableBytes() > 0) {
//                String body = httpRequest.content().toString(StandardCharsets.UTF_8);
//                if (body != null && !body.isEmpty()) {
//                    // 这里仍然需要 AcceptFriendRequestPayload DTO
//                    AcceptFriendRequestPayload payload = jacksonObjectMapper.readValue(body, AcceptFriendRequestPayload.class);
//                    aliasForRequesterByCurrentUser = payload.aliasName;
//                    logger.debug("从请求体中解析得到 aliasName: {}", aliasForRequesterByCurrentUser);
//                }
//            } else {
//                logger.debug("接受好友请求的请求体为空，将使用默认备注名。");
//            }
//        } catch (Exception e) {
//            // 这里只记录警告，因为 aliasName 是可选的
//            logger.warn("解析接受请求的Body以获取aliasName失败 (将使用默认备注): {}", e.getMessage());
//            // 不中断流程，允许后续使用默认备注
//        }
//
//        // --- 2. 将数据库操作提交到业务线程池 ---
//        // **将解析得到的 aliasName (可能为 null) 传递给业务逻辑**
//        final String finalAliasFromRequest = aliasForRequesterByCurrentUser;
//
//
//        businessExecutor.submit(() -> {
//
//            System.out.println("55555555555555555");
//
//            try { // 解析可选的请求体中的 aliasName
//                if (httpRequest.content().isReadable() && httpRequest.content().readableBytes() > 0) {
//                    String body = httpRequest.content().toString(StandardCharsets.UTF_8);
//                    if (body != null && !body.isEmpty()) {
//                        AcceptFriendRequestPayload payload = jacksonObjectMapper.readValue(body, AcceptFriendRequestPayload.class);
//
//                    }
//                }
//            } catch (Exception e) { logger.warn("解析接受请求的Body以获取aliasName失败: {}", e.getMessage()); }
//
//            // 手动管理 SqlSession 和事务
//            SqlSession sqlSession = getSessionFactory().openSession(false); // autoCommit = false
//            try {
//                FriendRequestMapper frMapper = sqlSession.getMapper(FriendRequestMapper.class);
//                UserMapper userMapper = sqlSession.getMapper(UserMapper.class); // 需要UserMapper
//
//                FriendRequest requestDetails = frMapper.findRequestById(requestId);
//                if (requestDetails == null || !requestDetails.getTargetId().equals(currentUserId) || requestDetails.getRequestType() != 0) {
//                    sqlSession.rollback(); // 回滚（虽然可能没修改）
//                    logger.warn("接受好友请求失败：无效操作。请求ID: {}, 操作用户: {}", requestId, currentUserId);
//                    ctx.channel().eventLoop().execute(() -> sendErrorResponse(ctx, HttpResponseStatus.FORBIDDEN, "操作无效或权限不足", headers));
//                    return;
//                }
//
//                // 1. 更新请求状态
//                int updatedRows = frMapper.updateRequestStatus(requestId, 1, currentUserId); // 1 = 已接受
//
//                if (updatedRows > 0) {
//                    // 2. 添加双向好友关系
//                    User currentUser = userMapper.findUserById(currentUserId);
//                    String currentUserName = (currentUser != null && currentUser.getUsername() != null) ? currentUser.getUsername() : ("用户 " + currentUserId);
//                    String requesterName = requestDetails.getRequesterName();
//                    String finalAliasForRequester = (aliasForRequesterByCurrentUser != null && !aliasForRequesterByCurrentUser.isEmpty()) ? aliasForRequesterByCurrentUser : requesterName;
//
//                    frMapper.addUserFriend(currentUserId, requestDetails.getRequesterId(), finalAliasForRequester);
//                    frMapper.addUserFriend(requestDetails.getRequesterId(), currentUserId, currentUserName);
//
//                    sqlSession.commit(); // 提交事务
//                    logger.info("用户 {} 接受了好友请求 ID: {}", currentUserId, requestId);
//                    sendJsonResponse(ctx, HttpResponseStatus.OK, Map.of("success", true, "message", "好友请求已接受"), headers);
//                } else {
//                    sqlSession.rollback(); // 回滚事务
//                    logger.warn("接受好友请求 ID: {} 更新数据库失败或已被处理", requestId);
//                    ctx.channel().eventLoop().execute(() -> sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, "接受请求失败或请求已被处理", headers));
//                }
//            } catch (Exception e) {
//                sqlSession.rollback(); // 出错时回滚
//                logger.error("处理接受好友请求 ID: {} 错误: {}", requestId, e.getMessage(), e);
//                ctx.channel().eventLoop().execute(() -> sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "处理请求时发生服务器内部错误", headers));
//            } finally {
//                if (sqlSession != null) {
//                    sqlSession.close(); // 必须关闭 Session
//                }
//            }
//        });
//    }

    private void handleAcceptRequest(ChannelHandlerContext ctx, HttpHeaders headers, FullHttpRequest httpRequest, Integer requestId, Integer currentUserId) {

        // --- 1. 在 IO 线程同步读取和解析可选的请求体 (获取 aliasName) ---
        String aliasFromRequestBody = null; // 存储从请求体解析出的 aliasName
        try {
            if (httpRequest.content().isReadable() && httpRequest.content().readableBytes() > 0) {
                String body = httpRequest.content().toString(StandardCharsets.UTF_8);
                if (body != null && !body.isEmpty()) {
                    AcceptFriendRequestPayload payload = jacksonObjectMapper.readValue(body, AcceptFriendRequestPayload.class);
                    aliasFromRequestBody = payload.aliasName;
                    logger.debug("从请求体中解析得到 aliasName: {}", aliasFromRequestBody);
                } else {
                    logger.debug("接受好友请求的请求体为空或无法解析。");
                }
            } else {
                logger.debug("接受好友请求的请求体为空，将使用默认备注名。");
            }
        } catch (Exception e) {
            // 只记录警告，因为 aliasName 是可选的
            logger.warn("解析接受请求的Body以获取aliasName失败 (将使用默认备注): {}", e.getMessage());
            // 不中断流程，允许后续使用默认备注
        }

        // --- 2. 创建 final 副本，以便在 Lambda 中安全使用 ---
        final String finalAliasFromRequest = aliasFromRequestBody; // 这个 final 变量会被 lambda 捕获

        // --- 3. 将数据库操作提交到业务线程池 ---
        businessExecutor.submit(() -> { // Lambda 表达式开始
            logger.info("==> 开始处理接受好友请求 (数据库操作): 操作用户={}, 请求ID={}", currentUserId, requestId);
            SqlSession sqlSession = null;
            try {
                // 使用手动事务管理
                sqlSession = getSessionFactory().openSession(false); // autoCommit = false
                FriendRequestMapper frMapper = sqlSession.getMapper(FriendRequestMapper.class);
                UserMapper userMapper = sqlSession.getMapper(UserMapper.class);

                logger.debug("步骤 1/3: 查询请求详情 (ID={})", requestId);
                FriendRequest requestDetails = frMapper.findRequestById(requestId);

                // 权限和状态校验
                if (requestDetails == null || !requestDetails.getTargetId().equals(currentUserId) || requestDetails.getRequestType() != 0) {
                    sqlSession.rollback();
                    logger.warn("接受好友请求失败：无效操作。请求ID: {}, 操作用户: {}", requestId, currentUserId);
                    ctx.channel().eventLoop().execute(() -> sendErrorResponse(ctx, HttpResponseStatus.FORBIDDEN, "操作无效或权限不足", headers));
                    return; // 结束任务
                }
                logger.debug("请求详情校验通过。");

                logger.debug("步骤 2/3: 更新请求状态为已接受 (ID={})", requestId);
                int updatedRows = frMapper.updateRequestStatus(requestId, 1, currentUserId);

                if (updatedRows > 0) {
                    logger.debug("请求状态更新成功。");
                    logger.debug("步骤 3/3: 添加双向好友关系...");

                    User currentUser = userMapper.findUserById(currentUserId);
                    String currentUserName = (currentUser != null && currentUser.getUsername() != null) ? currentUser.getUsername() : ("用户 " + currentUserId);
                    String requesterName = requestDetails.getRequesterName() != null ? requestDetails.getRequesterName() : ("用户 " + requestDetails.getRequesterId());

                    // **在 Lambda 内部 *读取* final 副本 finalAliasFromRequest**
                    String finalAliasForRequester = (finalAliasFromRequest != null && !finalAliasFromRequest.isEmpty()) ?
                            finalAliasFromRequest : requesterName; // 使用 final 副本

                    logger.debug("添加好友关系: {} -> {} (备注: {})", currentUserId, requestDetails.getRequesterId(), finalAliasForRequester);
                    frMapper.addUserFriend(currentUserId, requestDetails.getRequesterId(), finalAliasForRequester); // 使用最终备注

                    logger.debug("添加好友关系: {} -> {} (备注: {})", requestDetails.getRequesterId(), currentUserId, currentUserName);
                    frMapper.addUserFriend(requestDetails.getRequesterId(), currentUserId, currentUserName);

                    sqlSession.commit(); // 提交事务
                    logger.info("用户 {} 接受了好友请求 ID: {}, 来自用户 {}", currentUserId, requestId, requestDetails.getRequesterId());
                    sendJsonResponse(ctx, HttpResponseStatus.OK, Map.of("success", true, "message", "好友请求已接受"), headers);
                } else {
                    sqlSession.rollback(); // 回滚事务
                    logger.warn("接受好友请求 ID: {} 更新数据库失败或已被处理", requestId);
                    ctx.channel().eventLoop().execute(() -> sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, "接受请求失败或请求已被处理", headers));
                }
            } catch (Exception e) {
                if (sqlSession != null) sqlSession.rollback(); // 出错时回滚
                logger.error("处理接受好友请求 ID: {} 错误: {}", requestId, e.getMessage(), e);
                ctx.channel().eventLoop().execute(() -> sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "处理请求时发生服务器内部错误", headers));
            } finally {
                if (sqlSession != null) {
                    sqlSession.close(); // 必须关闭 Session
                    logger.debug("数据库 Session 已关闭 (接受好友请求操作)。");
                }
            }
        }); // businessExecutor.submit 结束
    } // handleAcceptRequest 方法结束

    private void handleDeclineRequest(ChannelHandlerContext ctx, HttpHeaders headers, Integer requestId, Integer currentUserId) {
        businessExecutor.submit(() -> {

            System.out.println("6666666666666");
            // 手动管理 SqlSession 和事务 (虽然这里只有一步更新，但保持风格一致)
            SqlSession sqlSession = getSessionFactory().openSession(false); // autoCommit = false
            try {
                FriendRequestMapper mapper = sqlSession.getMapper(FriendRequestMapper.class);

                FriendRequest requestDetails = mapper.findRequestById(requestId);
                if (requestDetails == null || !requestDetails.getTargetId().equals(currentUserId) || requestDetails.getRequestType() != 0) {
                    sqlSession.rollback();
                    logger.warn("拒绝好友请求失败：无效操作。请求ID: {}, 操作用户: {}", requestId, currentUserId);
                    ctx.channel().eventLoop().execute(() -> sendErrorResponse(ctx, HttpResponseStatus.FORBIDDEN, "操作无效或权限不足", headers));
                    return;
                }

                int updatedRows = mapper.updateRequestStatus(requestId, 2, currentUserId); // 2 = 已拒绝
                if (updatedRows > 0) {
                    sqlSession.commit(); // 提交事务
                    logger.info("用户 {} 拒绝了好友请求 ID: {}", currentUserId, requestId);
                    sendJsonResponse(ctx, HttpResponseStatus.OK, Map.of("success", true, "message", "好友请求已拒绝"), headers);
                } else {
                    sqlSession.rollback(); // 回滚事务
                    logger.warn("拒绝好友请求 ID: {} 更新数据库失败或已被处理", requestId);
                    ctx.channel().eventLoop().execute(() -> sendErrorResponse(ctx, HttpResponseStatus.BAD_REQUEST, "拒绝请求失败或请求已被处理", headers));
                }
            } catch (Exception e) {
                sqlSession.rollback(); // 出错时回滚
                logger.error("处理拒绝好友请求 ID: {} 错误: {}", requestId, e.getMessage(), e);
                ctx.channel().eventLoop().execute(() -> sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "处理请求时发生服务器内部错误", headers));
            } finally {
                if (sqlSession != null) {
                    sqlSession.close(); // 必须关闭 Session
                }
            }
        });
    }

    // --- 辅助方法 (同前) ---
    // 发送 JSON 响应 (改为直接写对象，依赖JsonOutboundEncoder)
    private void sendJsonResponse(ChannelHandlerContext ctx, HttpResponseStatus status, Object body, HttpHeaders requestHeaders) {
        // 注意：这个版本假设你的Pipeline中有JsonOutboundEncoder可以处理Object
        // 并且它会设置正确的 Content-Type 和 Content-Length
        // 如果你的JsonOutboundEncoder不处理Map或POJO，你还是需要用org.json或Jackson手动转为字符串再写入ByteBuf

        // 直接将Java对象写入，由JsonOutboundEncoder处理序列化和响应构建
        // 为了更明确地设置状态码，我们仍然可以构建一个HttpResponse对象，但只包含头部
        // 然后写入对象作为内容。但这比较复杂。

        // 简单起见，如果JsonOutboundEncoder能处理，并且你不需要复杂的头部控制：
        ctx.writeAndFlush(body); // 直接写入

        // 如果需要设置状态码和头部，并且JsonOutboundEncoder能处理对象:
        // 你可能需要一个更复杂的出站处理器组合，或者在这里手动序列化并设置头部
        // String jsonBody = jacksonObjectMapper.writeValueAsString(body); // 使用Jackson示例
        // ByteBuf content = Unpooled.copiedBuffer(jsonBody, CharsetUtil.UTF_8);
        // FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, content);
        // response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        // response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
        // ctx.writeAndFlush(response);
    }

    // 发送错误 JSON 响应 (使用 org.json)
    private void sendErrorResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String message, HttpHeaders requestHeaders) {
        JSONObject errorJson = new JSONObject();
        errorJson.put("success", false);
        errorJson.put("message", message);
        errorJson.put("code", status.code());

        ByteBuf content = Unpooled.copiedBuffer(errorJson.toString(), CharsetUtil.UTF_8);
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, content);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
        ctx.writeAndFlush(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Handler内部发生未捕获异常: {}", cause.getMessage(), cause);
        if (ctx.channel().isActive()) {
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "服务器内部未知错误", null);
        }
        // ctx.close(); // 视情况决定是否关闭
    }
}