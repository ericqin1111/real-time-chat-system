package client;

import io.netty.channel.ChannelHandlerContext;
import server.GlobalVar;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Scanner;

public class ChatClient {
    private String username;
    private ChannelHandlerContext ctx;

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
        //连接服务端
        SocketChannel socketChannel=SocketChannel.open(new InetSocketAddress("127.0.0.1",8080));
        //接收服务端响应数据
        Selector selector=Selector.open();
        socketChannel.configureBlocking(false);
        socketChannel.register(selector, SelectionKey.OP_READ);
        //创建线程
        new Thread(new ClientThread(selector)).start();

        //向服务器发送消息
        Scanner scanner=new Scanner(System.in);
        while(scanner.hasNextLine()){
            String msg = scanner.nextLine();
            if(msg.length()>0){
                socketChannel.write(Charset.forName("UTF-8").encode(name+":"+msg));
            }
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



