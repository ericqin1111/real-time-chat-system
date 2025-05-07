package server.handler.general;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.util.CharsetUtil;
import server.GlobalVar;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class  ParamsHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    // 定义存储参数的 AttributeKey
    // AttributeKey 的作用是作为 唯一标识符，用于在 Channel 的上下文中存取数据。它本身不存储任何业务数据，只是用来标识一个存储位置。

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
        Map<String, String> params = parseParams(ctx ,request);

        // 3. 将参数存入 ctx 的 Attribute
        ctx.channel().attr(GlobalVar.PARAM_KEY).set(params);
        System.out.println("ok params");
        // 4. 传递请求到业务 Handler
        ctx.fireChannelRead(request.retain());
    }

    private Map<String, String> parseParams(ChannelHandlerContext ctx , FullHttpRequest request) {
        Map<String, String> params = new HashMap<>();
        ByteBuf content = request.content();

        // 根据 Content-Type 解析参数
        String contentType = request.headers().get(HttpHeaderNames.CONTENT_TYPE);
        System.out.println("contentType: " + contentType);
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
        }else {
            // 初始化 multipart 解析器
            HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(request);
            if (decoder.isMultipart()) {
                try {
                    // 遍历所有请求体部分
                    for (InterfaceHttpData data : decoder.getBodyHttpDatas()) {
                        if (data.getHttpDataType() == InterfaceHttpData.HttpDataType.FileUpload) {


                            // 处理文件上传
                            handleFileUpload( ctx ,(FileUpload) data, params);
                        } else if (data.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute) {
                                // 处理普通参数
                            handleAttribute((Attribute) data, params);
                        }
                        data.release();
                    }
                }
                catch (Exception e){
                    e.printStackTrace();
                }
                }

        }
        return params;
    }

    // 处理文件上传
    private void handleFileUpload(ChannelHandlerContext ctx, FileUpload fileUpload, Map<String,String> params) throws IOException {
        if (fileUpload.isCompleted()) {
            System.out.println(fileUpload.getName());
            // 生成唯一文件名
            String originalName = fileUpload.getFilename();
            String uniqueName = generateUniqueFileName(originalName);
            params.put("fileName", uniqueName);

            //使用线程池来本地化文件，不过如果本地化失败没有进行反馈，待完善。
            GlobalVar.businessExecutor.execute(() ->{
                try {
                // 保存文件到本地
                Path uploadPath = Paths.get(GlobalVar.UPLOAD_DIR, uniqueName);
                Files.createDirectories(uploadPath.getParent());
                fileUpload.renameTo(uploadPath.toFile());
                }catch (Exception e){
                    e.printStackTrace();
                }
            });

        }
    }
    // 处理普通参数
    private void handleAttribute(Attribute attribute, Map<String, String> params) throws IOException {
        params.put(attribute.getName(), attribute.getValue());
    }

    // 生成唯一文件名（时间戳+随机数+原始文件名哈希）
    private String generateUniqueFileName(String originalName) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.valueOf(ThreadLocalRandom.current().nextInt(1000, 9999));
        String hash = Integer.toHexString(originalName.hashCode());
        return timestamp + "_" + random + "_" + hash + getFileExtension(originalName);
    }

    // 获取文件扩展名
    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex == -1) ? "" : filename.substring(dotIndex);
    }
}