package entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("group_member_read")
public class GroupMemberRead {
    @TableField("group_id")
    private Integer groupId;

    @TableField("user_id")
    private Integer userId;

    @TableField("read_count")
    private Integer readCount;
}