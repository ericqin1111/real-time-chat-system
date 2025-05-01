package server.handler.example;// 可以放在一个新的测试包下

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import server.GlobalVar; // 确保能访问到业务线程池

import java.util.concurrent.TimeUnit;

/**
 * 一个用于测试业务线程池效果的 Handler。
 * 它接收 HTTP 请求，模拟一个耗时操作，并将该操作提交给业务线程池执行。
 */
public class ThreadPoolTestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        // 获取当前线程（应该是 Netty 的 EventLoop 线程）
        String eventLoopThreadName = Thread.currentThread().getName();
        System.out.println("[" + eventLoopThreadName + "] ThreadPoolTestHandler: 收到请求 URI -> " + request.uri());

        // 模拟这是一个需要耗时处理的操作（比如 3 秒）
        final int sleepTimeSeconds = 3;



        // 1. 创建一个 Runnable 任务来封装耗时操作
        Runnable timeConsumingTask = () -> {
            // 获取当前线程（应该是业务线程池里的线程）
            String businessThreadName = Thread.currentThread().getName();
            System.out.println("[" + businessThreadName + "] 开始执行模拟耗时任务 (预计 " + sleepTimeSeconds + " 秒)...");

            try {
                // 模拟阻塞/耗时操作
                TimeUnit.SECONDS.sleep(sleepTimeSeconds);

                // 耗时操作完成
                System.out.println("[" + businessThreadName + "] 模拟耗时任务完成。");

                // 准备响应内容
                String responseContent = "任务已在业务线程 [" + businessThreadName + "] 中处理完成，耗时 " + sleepTimeSeconds + " 秒。";
                FullHttpResponse response = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1,
                        HttpResponseStatus.OK,
                        Unpooled.copiedBuffer(responseContent, CharsetUtil.UTF_8)
                );
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
                response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());

                // **重要**：将发送响应的操作调度回 EventLoop 线程执行
                ctx.channel().eventLoop().execute(() -> {
                    String responseThreadName = Thread.currentThread().getName();
                    System.out.println("[" + responseThreadName + "] 准备发送响应...");
                    // 检查 Channel 是否还活跃
                    if (ctx.channel().isActive()) {
                        // 发送响应并添加监听器，完成后关闭连接（对于简单测试方便）
                        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                        System.out.println("[" + responseThreadName + "] 响应已发送。");
                    } else {
                        System.out.println("[" + responseThreadName + "] Channel 不再活跃，响应未发送。");
                    }
                });

            } catch (InterruptedException e) {
                System.err.println("[" + businessThreadName + "] 耗时任务被中断。");
                Thread.currentThread().interrupt(); // 保留中断状态
                // (可选) 发送错误响应
                sendErrorResponse(ctx, "任务被中断", businessThreadName);
            } catch (Exception e) {
                System.err.println("[" + businessThreadName + "] 耗时任务执行出错: " + e.getMessage());
                // (可选) 发送错误响应
                sendErrorResponse(ctx, "任务执行出错: " + e.getMessage(), businessThreadName);
            }
        };

        // 2. 将任务提交给业务线程池
        try {
            GlobalVar.businessExecutor.execute(timeConsumingTask);
            System.out.println("[" + eventLoopThreadName + "] 任务已成功提交给业务线程池。EventLoop 线程现在空闲。");
        } catch (Exception e) {
            // 处理提交任务时可能发生的异常（例如线程池已关闭或队列满且拒绝策略为 Abort）
            System.err.println("[" + eventLoopThreadName + "] 提交任务到业务线程池失败: " + e.getMessage());
            sendErrorResponse(ctx, "服务器繁忙，无法处理请求: " + e.getMessage(), eventLoopThreadName);
        }

        // channelRead0 方法在 EventLoop 线程上快速返回
    }

    // 辅助方法：发送错误响应
    private void sendErrorResponse(ChannelHandlerContext ctx, String message, String threadName) {
        ctx.channel().eventLoop().execute(() -> {
            String responseContent = "错误发生在线程 [" + threadName + "]: " + message;
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.INTERNAL_SERVER_ERROR,
                    Unpooled.copiedBuffer(responseContent, CharsetUtil.UTF_8)
            );
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            if (ctx.channel().isActive()) {
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            }
        });
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // 处理在 EventLoop 线程中发生的未捕获异常
        System.err.println("ThreadPoolTestHandler 在 EventLoop 线程 [" + Thread.currentThread().getName() + "] 捕获异常:");
        cause.printStackTrace();
        sendErrorResponse(ctx, "服务器内部错误: " + cause.getMessage(), Thread.currentThread().getName());
        // ctx.close(); // sendErrorResponse 已经会 close 了
    }
}