package server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

public class MainServer {

    private final int port;

    public MainServer(int port) {
        this.port = port;
    }


    public void run() throws Exception {
        // 配置服务端线程组
        EventLoopGroup bossGroup = new NioEventLoopGroup(1); // 接收连接
        EventLoopGroup workerGroup = new NioEventLoopGroup(); // 处理业务

        try {

            //ServerBootstrap 是 Netty 中用于快速配置和启动 服务端 的核心类，它通过链式调用方法简化了 NIO 服务端的搭建过程。
            ServerBootstrap bootstrap = new ServerBootstrap();
            //配置ing
            bootstrap.group(bossGroup, workerGroup)//bossGroup是负责接受请求的selector,只需要一个。workergroup是负责处理余下逻辑的selector们，每个selector都会执行所有的childhandler的pipeline
                    .channel(NioServerSocketChannel.class) // 使用NIO模型，单纯是模式的选择
                    //客户端连接的处理器
                    .childHandler(new ChannelInitializer<SocketChannel>() {//childhandler都会被workergroup的selector执行，至于接收请求，bossgroup会自动进行并分配到workergroup





                        @Override
                        protected void initChannel(SocketChannel ch) {//ch是客户端请求channel
                            ChannelPipeline pipeline = ch.pipeline();

                            // 添加字符串编解码器
                            pipeline.addLast(new StringDecoder());//字节变字符串
                            pipeline.addLast(new StringEncoder());//出站，字符串变成字节

                            // 这里添加入站处理器
                            pipeline.addLast(new EchoServerHandler());


                            //这里添加出站处理器，注意，出站处理器是逆序执行的.
                        }
                    })
                    //这里是服务器的一般配置
                    .option(ChannelOption.SO_BACKLOG, 256) // 连接队列大小
                    .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)//使用堆外内存，零拷贝
                    .childOption(ChannelOption.SO_KEEPALIVE, true); // 保持长连接






            // 绑定端口，同步等待成功
            ChannelFuture f = bootstrap.bind(port).sync();
            System.out.println("服务器启动成功，监听端口: " + port);

            // 等待服务端监听端口关闭
            f.channel().closeFuture().sync();
        } finally {
            // 优雅关闭线程组
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    // 自定义业务处理器
    private static class EchoServerHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            // 接收客户端消息并回显

            String message = (String) msg;
            System.out.println("收到消息: " + message);
            ctx.writeAndFlush("服务器回应: " + message + "\n");
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            // 异常处理
            cause.printStackTrace();
            ctx.close();
        }
    }

    public static void main(String[] args) throws Exception {
        int port = 8080;
        new MainServer(port).run();
    }
}

