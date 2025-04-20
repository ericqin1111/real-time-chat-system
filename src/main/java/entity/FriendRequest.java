package entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("friend_requests")
public class FriendRequest {
    @TableField("requester_id")
    private Integer requesterId;

    @TableField("target_id")
    private Integer targetId;

    @TableField("request_type")
    private Integer requestType;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
