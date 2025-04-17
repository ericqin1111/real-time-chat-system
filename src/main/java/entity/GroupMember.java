package entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("group_members")
public class GroupMember {
    @TableField("group_id")
    private Integer groupId;

    @TableField("user_id")
    private Integer userId;

    @TableField("member_alias")
    private String memberAlias;
}