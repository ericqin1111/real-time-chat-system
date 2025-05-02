package entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("friend_messages")
public class FriendMessage {

    @TableField("message_id")
    private Integer messageId;

    @TableField("sender_id")
    private Integer senderId;

    @TableField("receiver_id")
    private Integer receiverId;

    @TableField("content")
    private String content;

    @TableField(value = "sent_time", fill = FieldFill.INSERT)
    private LocalDateTime sentTime;

    @TableField("content_type")
    private Integer contentType;
}