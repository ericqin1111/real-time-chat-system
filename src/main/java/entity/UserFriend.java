package entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("user_friends")
public class UserFriend {
    @TableField("user_id")
    private Integer userId;

    @TableField("friend_id")
    private Integer friendId;

    @TableField("alias_name")
    private String aliasName;
}
