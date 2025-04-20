package entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("chat_groups")
public class ChatGroup {
    @TableId(value = "group_id", type = IdType.AUTO)
    private Integer groupId;

    @TableField("group_name")
    private String groupName;

    @TableField("member_count")
    private Integer memberCount;

    @TableField("group_avatar")
    private byte[] groupAvatar;

    @TableField("group_avatar_mime_type")
    private String groupAvatarMimeType;
}