package entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("group_message_stats")
public class GroupMessageStat {
    @TableId(value = "group_id")
    private Integer groupId;

    @TableField("total_messages")
    private Integer totalMessages;
}