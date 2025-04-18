package server;

import io.netty.util.AttributeKey;

import java.util.Map;

public class GlobalVar {
    public static final AttributeKey<Map<String, String>> PARAM_KEY =
            AttributeKey.valueOf("Params");
    public static final AttributeKey<String> USERNAME = AttributeKey.valueOf("username");
    public static final String JDBC="jdbc:mysql://localhost:3306/chat_system";
    public static final String USER = "root";
    public static final String PASS = "123456";
    public static final int SERVER_PORT = 8080;
    public static final String ALLOWED_PORT = "8090";
}
