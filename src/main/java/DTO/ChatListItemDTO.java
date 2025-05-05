package DTO;

import lombok.Data;

@Data
public class ChatListItemDTO {
    private int id; // 好友的 userId 或 群组的 groupId
    private String name; // 好友的 username 或 群组的 groupName
    private String type; // "friend" 或 "group"
    private String avatarUrl; // 头像的 URL (需要逻辑生成，见下文)
    private String lastMessageContent; // 最新消息内容预览
    private String lastMessageTime; // 最新消息时间 (格式化字符串)
    private int unreadCount; // 未读消息数

    private long timestampForSort;
    // Getters and Setters...
}