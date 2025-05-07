package server;

import client.ChatClient;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import server.handler.utils.UserChannelInfo;


public class GlobalVar {
    private static Map<String, ChatClient> clientMap = new ConcurrentHashMap<>();

    //http请求参数
    public static final AttributeKey<Map<String, String>> PARAM_KEY =
            AttributeKey.valueOf("Params");
    public static final AttributeKey<String> USERID = AttributeKey.valueOf("userid");
    public static final String JDBC="jdbc:mysql://localhost:3306/chat_system";
    public static final String USER = "root";
    public static final String PASS = "12345";
    public static final int SERVER_PORT = 8080;
    public static final int HTTPS_PORT = 8080;
    public static final String ALLOWED_PORT = "8090";
    public static final String UPLOAD_DIR = "upload/";
    //websocketframe数据
    public static final AttributeKey<Map<String, String>> DATA_CONTEXT =
            AttributeKey.valueOf("dataContext");
    //全局用户websockerchannel
    public static final ConcurrentHashMap<String, UserChannelInfo> userChannelMap = new ConcurrentHashMap<>();

    public static final ExecutorService businessExecutor = createBusinessExecutor();
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();



    public static LocalDateTime toLocalDateTime(Date date) {
        return date == null ? null : date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }





    private static ExecutorService createBusinessExecutor() {
        // 使用 Guava 的 ThreadFactoryBuilder 给线程命名，方便调试
        // 如果不想引入 Guava，可以手动实现 ThreadFactory
        try {
            int cpuCountResult = Runtime.getRuntime().availableProcessors();

            int corePoolSize = Math.max(1, cpuCountResult); // 核心数至少为 1
            int maximumPoolSize = Math.max(1, cpuCountResult * 2); // 最大数至少为 1
            // 如果核心数可能大于最大数（例如 cpuCountResult=0 时，core=1, max=1），需要调整
            if (maximumPoolSize < corePoolSize) {
                maximumPoolSize = corePoolSize; // 确保 maximumPoolSize >= corePoolSize
            }



            ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
                    .setNameFormat("biz-logic-%d").build();

            // 创建一个有界阻塞队列，用于存放等待执行的任务
            BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(1000);

            ThreadPoolExecutor executor = new ThreadPoolExecutor(
                    corePoolSize,       // 核心线程数
                    maximumPoolSize,    // 最大线程数
                    60L,                // 非核心线程空闲时的存活时间
                    TimeUnit.SECONDS,   // 存活时间单位
                    workQueue,          // 任务队列
                    namedThreadFactory, // 线程工厂
                    new ThreadPoolExecutor.CallerRunsPolicy()
            );
            return executor;
        } catch (Throwable t) {
            System.err.println("createBusinessExecutor 内部捕获到严重错误 !!!");
            t.printStackTrace();
            throw t;
        }

    }

    // --- 新增：关闭线程池的方法 ---
    public static void shutdownExecutors() {
        System.out.println("开始关闭业务线程池...");
        businessExecutor.shutdown(); // 不再接受新任务，等待已提交任务完成
        try {
            // 等待一段时间让任务执行完毕
            if (!businessExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                System.err.println("业务线程池超时未关闭，尝试强制关闭...");
                businessExecutor.shutdownNow(); // 尝试中断正在执行的任务
                // 再等待一段时间
                if (!businessExecutor.awaitTermination(60, TimeUnit.SECONDS))
                    System.err.println("业务线程池未能终止");
            }
        } catch (InterruptedException ie) {
            // (重新) 尝试强制关闭
            businessExecutor.shutdownNow();
            // 保留中断状态
            Thread.currentThread().interrupt();
        }
        System.out.println("业务线程池已关闭。");
    }




    public static void addClient(ChatClient client) {
        clientMap.put(client.getUsername(), client);
    }

    // 获取客户端
    public static ChatClient getClient(String username) {
        return clientMap.get(username);
    }

    // 清理客户端
    public static void removeClient(String username) {
        clientMap.remove(username);
    }


    // 添加
    public static void addUserChannel(String userId, Channel channel) {
        userChannelMap.put(userId, new UserChannelInfo(channel));
        System.out.println("after add:" +  userChannelMap.keySet());
    }

    // 移除
    public static void removeUserChannel(String userId) {
        userChannelMap.remove(userId);
        System.out.println("after remove:"+ userChannelMap.keySet());
    }

    public static void sendMessageToUser(String userId, Object msg) {
        UserChannelInfo info = userChannelMap.get(userId);



        System.out.println("send to:" + userId);
        if (info != null) {
            ReentrantLock lock = info.getLock();
            lock.lock();
            try {
                Channel channel = info.getChannel();
                if (channel.isActive()) {
                    System.out.println("come to send message to user");
                    channel.writeAndFlush(msg).addListener(future -> {
                        if (!future.isSuccess()) {
                            System.err.println("Failed to send message to user " + userId + ": " + future.cause());
                            // 这里可以考虑移除 userChannelMap（说明通道已不可用）
                            userChannelMap.remove(userId);
                        } else {
                            System.out.println("Message sent to user " + userId);
                        }
                    });
                }
            } finally {
                lock.unlock();
            }
        }
    }
}
