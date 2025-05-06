package entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("friend_requests")
public class FriendRequest {
    @TableId(value = "id", type = IdType.AUTO) // <--- 修改这里
    private Integer id;

    @TableField("requester_id")
    private Integer requesterId;

    @TableField("target_id")
    private Integer targetId;

    @TableField("request_type")
    private Integer requestType;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField("requester_name")
    private String requesterName;
}
