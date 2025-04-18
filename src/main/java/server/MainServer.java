package server;

import config.MyBatisConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpServerCodec;
import server.handler.general.*;

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

                            pipeline //入站处理器
                                    .addLast(new HttpServerCodec()) // HTTP 请求解码和响应编码
                                    .addLast(new HttpObjectAggregator(65536))// 聚合 HTTP 请求为 FullHttpRequest
                                    .addLast(new CorsInboundHandler())//判断跨域
                                    .addLast(new JwtAuthHandler())//判断是否需要jwt
                                    .addLast(new ParamsHandler())//解析参数
                                    .addLast(new RouterHandler())//正式进入处理
                                    //出站处理器
                                    .addLast(new CorsOutboundHandler())//出站加跨域头
                                    .addLast(new JsonOutboundEncoder());//json化数据

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



    public static void main(String[] args) throws Exception {
        MyBatisConfig.init();


        int port = 8080;
        new MainServer(port).run();
    }
}

