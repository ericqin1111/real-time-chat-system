package server.handler.example; // 假设你的业务处理器放在这个包下

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*; // 仅作示例，你可能处理其他类型的对象
import io.netty.buffer.Unpooled; // 仅作示例
import io.netty.util.CharsetUtil; // 仅作示例
import server.GlobalVar; // 确保可以访问业务线程池 ExecutorService

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * @param <T>
 */
// 1. 继承 SimpleChannelInboundHandler 并指定你期望处理的入站消息类型 T
public class GenericBusinessHandler<T> extends SimpleChannelInboundHandler<T> {

    // 通常业务 Handler 会依赖某些 Service 类来执行具体操作
    // private final YourBusinessService businessService; // 可以通过构造函数注入

    // 获取业务线程池实例 (通过 GlobalVar 获取)
    private final ExecutorService businessExecutor = GlobalVar.businessExecutor;

    /*
    public GenericBusinessHandler(YourBusinessService service) {
        this.businessService = service;
    }
    */

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, T request) throws Exception {
        final String eventLoopThreadName = Thread.currentThread().getName();
        System.out.println("[" + eventLoopThreadName + "] " + this.getClass().getSimpleName() + ": 收到请求 -> " + request);

        // --------------------------------------------------------------------
        // 步骤 1: (可选) 从请求对象 request 中提取执行业务逻辑所需的数据
        // --------------------------------------------------------------------
        // final RelevantData data = extractDataFromRequest(request);


        // --------------------------------------------------------------------
        // 步骤 2: 创建一个 Runnable 或 Callable 任务来封装耗时的业务逻辑
        // --------------------------------------------------------------------
        Runnable businessLogicTask = () -> {
            final String businessThreadName = Thread.currentThread().getName();
            System.out.println("[" + businessThreadName + "] 开始执行业务逻辑...");

            try {
                // ------------------------------------------------------------
                // 步骤 2a: 在这里执行实际的、可能阻塞的业务逻辑
                //          例如：调用数据库、访问外部 API、进行复杂计算等
                // ------------------------------------------------------------
                // YourBusinessResult result = businessService.performLongOperation(data);
                // 模拟操作:
                Object result = simulateBlockingOperation();
                System.out.println("[" + businessThreadName + "] 业务逻辑执行完成，结果: " + result);


                // ------------------------------------------------------------
                // 步骤 2b: (可选) 根据业务逻辑结果准备响应对象
                // ------------------------------------------------------------
                // YourResponseObject response = createResponseFromResult(result);
                // 模拟响应:
                Object response = createDummyResponse(result, businessThreadName);


                // ------------------------------------------------------------
                // 步骤 2c: 重要 将写回响应的操作调度回 EventLoop 线程执行
                //          因为 Netty 的 Channel I/O 操作必须在对应的 EventLoop 中执行
                // ------------------------------------------------------------
                ctx.channel().eventLoop().execute(() -> {
                    final String responseThreadName = Thread.currentThread().getName();
                    System.out.println("[" + responseThreadName + "] 准备发送响应...");
                    if (ctx.channel().isActive()) {
                        // 发送响应，可以根据需要添加 Listener
                        ctx.writeAndFlush(response)/*.addListener(ChannelFutureListener.CLOSE_ON_FAILURE)*/;
                        System.out.println("[" + responseThreadName + "] 响应已发送。");
                    } else {
                        System.out.println("[" + responseThreadName + "] Channel 不再活跃，响应未发送。");
                    }
                });

            } catch (InterruptedException e) {
                // 如果业务逻辑（如 sleep）被中断
                System.err.println("[" + businessThreadName + "] 业务逻辑任务被中断。");
                Thread.currentThread().interrupt(); // 保留中断状态
                sendErrorResponse(ctx, "任务被中断", businessThreadName, HttpResponseStatus.INTERNAL_SERVER_ERROR); // 发送错误响应

            } catch (Exception e) {
                // 处理业务逻辑执行过程中发生的其他所有异常
                System.err.println("[" + businessThreadName + "] 业务逻辑执行出错: " + e.getMessage());
                e.printStackTrace(); // 打印详细错误供调试
                sendErrorResponse(ctx, "处理请求时发生内部错误", businessThreadName, HttpResponseStatus.INTERNAL_SERVER_ERROR); // 发送错误响应
            }
        };

        // --------------------------------------------------------------------
        // 步骤 3: 将任务提交给业务线程池执行
        // --------------------------------------------------------------------
        try {
            businessExecutor.execute(businessLogicTask);
            System.out.println("[" + eventLoopThreadName + "] 业务逻辑任务已提交。EventLoop 线程空闲。");
        } catch (Exception e) {
            // 处理提交任务时可能发生的异常 (如 RejectedExecutionException)
            System.err.println("[" + eventLoopThreadName + "] 提交任务到业务线程池失败: " + e.getMessage());
            sendErrorResponse(ctx, "服务器繁忙，请稍后重试", eventLoopThreadName, HttpResponseStatus.SERVICE_UNAVAILABLE);
        }

        // channelRead0 方法在此处快速返回，EventLoop 线程不会被阻塞
    }

    /**
     * 处理在 EventLoop 线程中发生的未捕获异常（例如解码错误、Handler 配置错误等）。
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        final String eventLoopThreadName = Thread.currentThread().getName();
        System.err.println(this.getClass().getSimpleName() + " 在 EventLoop 线程 [" + eventLoopThreadName + "] 捕获异常:");
        cause.printStackTrace();
        // 发送通用错误响应并关闭连接
        sendErrorResponse(ctx, "服务器内部错误", eventLoopThreadName, HttpResponseStatus.INTERNAL_SERVER_ERROR, true);
        // ctx.close(); // sendErrorResponse 可以选择性地关闭连接
    }

    // --- 辅助方法 ---

    /**
     * 模拟耗时操作 业务需要的方法，这里只是举个例子
     */
    private Object simulateBlockingOperation() throws InterruptedException {
        TimeUnit.MILLISECONDS.sleep(100 + (long) (Math.random() * 500)); // 随机睡眠 100-600 毫秒
        return "Simulated_Result_" + System.currentTimeMillis();
    }

    /**
     * 根据结果创建响应对象（这里是示例，创建 HTTP 响应） 根据业务的要求，返回需要的响应数据
     */
    private Object createDummyResponse(Object result, String processingThreadName) {
        String content = "Processed Result: " + result + " (handled by " + processingThreadName + ")";
        // 注意：这里强制转为 FullHttpResponse 是为了示例完整性
        // 实际中你应该根据你的应用协议返回正确的对象类型，可能需要出站 Encoder 处理
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                Unpooled.copiedBuffer(content, CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        return response;
    }

    /**
     * 发送错误响应的辅助方法 (HTTP 示例)
     *
     * @param ctx          ChannelHandlerContext
     * @param message      错误消息
     * @param threadName   发生错误的线程名
     * @param status       HTTP 状态码
     * @param closeOnFail  是否在发送后关闭连接
     */
    private void sendErrorResponse(ChannelHandlerContext ctx, String message, String threadName, HttpResponseStatus status, boolean closeOnFail) {
        // 同样，确保这个操作在 EventLoop 中执行
        ctx.channel().eventLoop().execute(() -> {
            if (!ctx.channel().isActive()) {
                return; // 如果 Channel 已关闭，则不发送
            }
            String responseContent = "Error occurred on thread [" + threadName + "]: " + message;
            // 注意：这里强制转为 FullHttpResponse 是为了示例完整性
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    status,
                    Unpooled.copiedBuffer(responseContent, CharsetUtil.UTF_8)
            );
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());

            ChannelFuture future = ctx.writeAndFlush(response);
            if (closeOnFail) {
                future.addListener(ChannelFutureListener.CLOSE); // 发送完毕后关闭连接
            }
        });
    }
    // 重载一个默认关闭连接的版本
    private void sendErrorResponse(ChannelHandlerContext ctx, String message, String threadName, HttpResponseStatus status) {
        sendErrorResponse(ctx, message, threadName, status, true);
    }

    // --- 你需要根据实际情况替换或实现以下方法 ---
    /*
    private RelevantData extractDataFromRequest(T request) {
        // TODO: 从入站消息 request 中提取业务所需的数据
        return new RelevantData(...);
    }

    private YourResponseObject createResponseFromResult(YourBusinessResult result) {
        // TODO: 根据业务结果 result 构建要发送给客户端的响应对象
        return new YourResponseObject(...);
    }
    */
}