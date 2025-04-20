package entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("friend_message_stats")
public class FriendMessageStat {
    @TableField("user_id")
    private Integer userId;

    @TableField("friend_id")
    private Integer friendId;

    @TableField("read_count")
    private Integer readCount;

    @TableField("total_count")
    private Integer totalCount;
}