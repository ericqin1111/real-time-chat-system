package DTO;

import lombok.Data;

@Data
public class MessageDTO {
    private long messageId; // 或 String，取决于数据库生成类型
    private int senderId;
    private String senderName; // 发送者名字 (群聊中需要)
    private String senderAvatarUrl; // 发送者头像 URL
    private String content; // 消息内容 (文本或文件名)
    private String time; // 格式化的时间字符串
    private boolean isMe; // **由后端根据请求者计算**
    private int contentType; // 1:文本, 2:文件, 3:图片...
    private FileInfoDTO fileInfo; // 可选，文件信息 (url, size, name)
    // Getters and Setters...
}

