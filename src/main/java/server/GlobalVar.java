package server;

import client.ChatClient;
import io.netty.util.AttributeKey;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GlobalVar {
    private static Map<String, ChatClient> clientMap = new ConcurrentHashMap<>();

    public static final AttributeKey<Map<String, String>> PARAM_KEY =
            AttributeKey.valueOf("Params");
    public static final AttributeKey<String> USERNAME = AttributeKey.valueOf("username");
    public static final String JDBC="jdbc:mysql://localhost:3306/chat_system";
    public static final String USER = "root";
    public static final String PASS = "123456";
    public static final int SERVER_PORT = 8080;
    public static final String ALLOWED_PORT = "8090";
    public static final String UPLOAD_DIR = "upload/";

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
}
