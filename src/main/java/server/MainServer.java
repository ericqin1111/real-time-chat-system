package server;

import config.MyBatisConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import server.handler.general.*;

import javax.net.ssl.SSLException;
import java.io.File;

public class MainServer {

    private final int port;

    private final SslContext sslContext;

    public MainServer(int port, SslContext sslContext) {
        this.port = port;
        this.sslContext = sslContext;
    }

    private static SslContext createSslContext() throws SSLException {
        try {
            // 指定PEM格式的证书和私钥文件路径
            File certChainFile = new File("src/server.crt");  // 证书文件
            File privateKeyFile = new File("src/server.key"); // 私钥文件

            // 构建SSL上下文
            return SslContextBuilder.forServer(certChainFile, privateKeyFile)
                    .protocols("TLSv1.2", "TLSv1.3") // 指定协议版本
                    .ciphers(null) // 使用默认加密套件
                    .build();
        } catch (SSLException e) {
            throw new SSLException("Failed to create SSL context", e);
        }
    }

    public void run() throws Exception {
        // 配置服务端线程组
        EventLoopGroup bossGroup = new NioEventLoopGroup(1); // 接收连接
        EventLoopGroup workerGroup = new NioEventLoopGroup(); // 处理业务

        try {
            SslContext sslCtx = createSslContext();
            //ServerBootstrap 是 Netty 中用于快速配置和启动 服务端 的核心类，它通过链式调用方法简化了 NIO 服务端的搭建过程。
            ServerBootstrap bootstrap = new ServerBootstrap();
            //配置ing
            bootstrap.group(bossGroup, workerGroup)//bossGroup是负责接受请求的selector,只需要一个。workergroup是负责处理余下逻辑的selector们，每个selector都会执行所有的childhandler的pipeline
                    .channel(NioServerSocketChannel.class) // 使用NIO模型，单纯是模式的选择
                    //客户端连接的处理器
                    .childHandler(new ChannelInitializer<SocketChannel>() {//childhandler都会被workergroup的selector执行，至于接收请求，bossgroup会自动进行并分配到workergroup





                        @Override
                        protected void initChannel(SocketChannel ch) throws SSLException {//ch是客户端请求channel
                            ChannelPipeline pipeline = ch.pipeline();
                            SslContext sslContext = createSslContext();
                            pipeline
                                    .addLast("httpServerCodec",new HttpServerCodec()) // HTTP 请求解码和响应编码
                                    .addLast("httpObjectAggregator", new HttpObjectAggregator(10 * 1024 * 1024))// 聚合 HTTP 请求为 FullHttpRequest
                                    // 入站处理器
                                    .addLast("corsInboundHandler", new CorsInboundHandler())//判断跨域
                                    .addLast("jwtAuthHandler", new JwtAuthHandler())//判断是否需要jwt
                                    .addLast("paramsHandler", new ParamsHandler())//解析参数
                                    .addLast("routerHandler", new RouterHandler(sslContext))//正式进入处理
                                    //出站处理器
                                    .addLast("corsOutboundHandler", new CorsOutboundHandler())//出站加跨域头
                                    .addLast("jsonOutboundEncoder", new JsonOutboundEncoder());//json化数据


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
        SslContext sslContext = createSslContext();

        new MainServer(GlobalVar.HTTPS_PORT,sslContext).run();

    }
}

