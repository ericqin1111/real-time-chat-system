package server.handler.general;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ParamsHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    // 定义存储参数的 AttributeKey
    // AttributeKey 的作用是作为 唯一标识符，用于在 Channel 的上下文中存取数据。它本身不存储任何业务数据，只是用来标识一个存储位置。
    private static final AttributeKey<Map<String, String>> PARAM_KEY =
            AttributeKey.valueOf("postParams");

    //使用方法如下
    //    Map<String, String> params = ctx.channel().attr(PostParamHandler.PARAM_KEY).get();
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        // 1. 仅处理 POST 请求
        if (request.method() != HttpMethod.POST) {
            ctx.fireChannelRead(request.retain()); // 传递到下一个 Handler
            return;
        }

        // 2. 解析参数（支持表单和 JSON）
        Map<String, String> params = parseParams(request);

        // 3. 将参数存入 ctx 的 Attribute
        ctx.channel().attr(PARAM_KEY).set(params);

        // 4. 传递请求到业务 Handler
        ctx.fireChannelRead(request.retain());
    }

    private Map<String, String> parseParams(FullHttpRequest request) {
        Map<String, String> params = new HashMap<>();
        ByteBuf content = request.content();

        // 根据 Content-Type 解析参数
        String contentType = request.headers().get(HttpHeaderNames.CONTENT_TYPE);
        if (contentType.startsWith(HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString())) {
            // 解析表单数据（如 name=John&age=30）
            String formData = content.toString(CharsetUtil.UTF_8);
            QueryStringDecoder decoder = new QueryStringDecoder("?" + formData, false);
            decoder.parameters().forEach((key, values) ->
                    params.put(key, values.get(0))); // 取第一个值
        } else if (contentType.startsWith(HttpHeaderValues.APPLICATION_JSON.toString())) {
            // 解析 JSON 数据（需引入 JSON 库如 Jackson）
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(content.toString(CharsetUtil.UTF_8));
                root.fields().forEachRemaining(entry ->
                        params.put(entry.getKey(), entry.getValue().asText()));
            } catch (IOException e) {
                throw new RuntimeException("JSON 解析失败", e);
            }
        }
        return params;
    }
}