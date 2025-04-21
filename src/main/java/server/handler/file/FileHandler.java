package server.handler.file;

import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import server.GlobalVar;


import java.io.File;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;



public class FileHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest) throws Exception {
        // 仅处理 GET 请求
        if (fullHttpRequest.method() != HttpMethod.GET) {
            sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED);
            return;
        }
        ctx.pipeline().remove("jsonOutboundEncoder");
        // 解析请求路径
        String uri = fullHttpRequest.uri();
        String fileName = uri.substring(GlobalVar.UPLOAD_DIR.length() -1 );
        System.out.println(fileName);

        File file = getFile(fileName);
        sendFile(ctx, file, fileName);

    }


    private File getFile(String name) throws IOException {
        // 构建文件路径
        Path filePath = Paths.get(GlobalVar.UPLOAD_DIR, name);
        System.out.println(filePath);
        File file = filePath.toFile();
        System.out.println(file.getAbsolutePath());
        // 处理文件不存在的情况
        if (!file.exists() || !file.isFile()) {
            System.out.println("bad");
            return null;
        }
        return file;
    }

    // 获取 MIME 类型
    private String getContentType(String filename) {
        String extension = filename.substring(filename.lastIndexOf('.') + 1);
        switch (extension.toLowerCase()) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            default:
                return "application/octet-stream";
        }
    }


    // 发送文件内容（高效零拷贝）
    private void sendFile(ChannelHandlerContext ctx, File file, String filename) throws IOException {
        RandomAccessFile raf = null;
        try {
            raf = new RandomAccessFile(file, "r");
            long fileLength = raf.length();

            // 构建 HTTP 响应
            HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);

            // 设置响应头
            HttpHeaders headers = response.headers();
            headers.set(HttpHeaderNames.CONTENT_LENGTH, fileLength);
            headers.set(HttpHeaderNames.CONTENT_TYPE, getContentType(file.getName()));
            headers.set(HttpHeaderNames.CACHE_CONTROL, "max-age=86400");  // 1天缓存

            System.out.println(response.headers());
            // 发送响应头
            ctx.write(response);

            // 发送图片内容（零拷贝方式）
            FileRegion region = new DefaultFileRegion(raf.getChannel(), 0, fileLength);
            ctx.write(region);

            // 发送结束标记并刷新
            ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)
                    .addListener(ChannelFutureListener.CLOSE_ON_FAILURE);

        } finally {
            if (raf != null) {
                raf.close();
            }
        }
    }

    // 发送错误响应
    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                status,
                Unpooled.copiedBuffer("Error: " + status, CharsetUtil.UTF_8)
        );
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
