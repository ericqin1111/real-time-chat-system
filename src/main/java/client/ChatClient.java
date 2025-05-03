package client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import server.GlobalVar;
import server.handler.ssl.ChatClientHandler;

import javax.net.ssl.SSLException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class ChatClient {
    private String username;
    private ChannelHandlerContext ctx;
    private Channel channel;

    public String getUsername() {
        return username;
    }

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    public ChatClient() {
    }

    // 客户端构造方法，接收用户名和上下文
    public ChatClient(String username, ChannelHandlerContext ctx) {
        this.username = username;
        this.ctx = ctx;
    }

    //客户端启动方法
    public void startClient(String name) throws IOException {
//        //连接服务端
//        SocketChannel socketChannel=SocketChannel.open(new InetSocketAddress("127.0.0.1",8080));
//        //接收服务端响应数据
//        Selector selector=Selector.open();
//        socketChannel.configureBlocking(false);
//        socketChannel.register(selector, SelectionKey.OP_READ);
//        //创建线程
//        new Thread(new ClientThread(selector)).start();
//
//        //向服务器发送消息
//        Scanner scanner=new Scanner(System.in);
//        while(scanner.hasNextLine()){
//            String msg = scanner.nextLine();
//            if(msg.length()>0){
//                socketChannel.write(Charset.forName("UTF-8").encode(name+":"+msg));
//            }
//        }
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            // 1. 创建SSL上下文（开发环境跳过验证）
            SslContext sslContext = SslContextBuilder.forClient()
                    .trustManager(new File("src/server.crt"))
                    .build();

            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(io.netty.channel.socket.SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();

                            // 2. 添加SSL处理器
                            p.addLast(sslContext.newHandler(ch.alloc(), "localhost", 8443));

                            // 3. 添加字符串编解码器
                            p.addLast(new StringEncoder());
                            p.addLast(new StringDecoder());

                            // 4. 添加业务处理器
                            p.addLast(new ChatClientHandler(name));
                        }
                    });

            // 5. 连接服务器
            ChannelFuture f = b.connect("localhost", 8443).sync();
            this.channel = f.channel();

            // 6. 控制台输入处理
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                String line = in.readLine();
                if (line == null || "quit".equalsIgnoreCase(line)) {
                    break;
                }
                channel.writeAndFlush(name + ":" + line);
            }

            channel.closeFuture().sync();
        } catch (InterruptedException | SSLException e) {
            throw new IOException(e);
        } finally {
            group.shutdownGracefully();
        }
    }
    public static void createChatClient(ChannelHandlerContext ctx, String username) {
        ChatClient client = new ChatClient(username, ctx);
        GlobalVar.addClient(client); // 假设 GlobalVar 维护着在线客户端列表
    }


    public static void main(String[] args){
        try {
            new ChatClient().startClient("131");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

        }



