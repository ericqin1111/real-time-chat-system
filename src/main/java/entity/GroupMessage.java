package entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("group_messages")
public class GroupMessage {
    @TableField("message_id")
    private Long messageId;

    @TableField("group_id")
    private Integer groupId;

    @TableField("sender_id")
    private Integer senderId;

    @TableField("content")
    private String content;

    @TableField(value = "sent_time", fill = FieldFill.INSERT)
    private LocalDateTime sentTime;
}