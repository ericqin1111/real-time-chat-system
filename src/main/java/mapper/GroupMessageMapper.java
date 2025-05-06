package mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.GroupMessage;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface GroupMessageMapper extends BaseMapper<GroupMessage> {

    @Select("select * from group_messages where group_id = #{groupId}")
    List<GroupMessage> findByGroupId(int groupId);

//    @Insert("INSERT INTO group_messages (group_id, sender_id, content, sent_time) " +
//            "VALUES (#{groupId}, #{senderId}, #{content}, NOW())") // Assuming sent_time uses DB default
//    @Options(useGeneratedKeys = true, keyProperty = "messageId", keyColumn = "message_id") // 获取自增/生成的ID
//    int insert(GroupMessage message);

    /**
     * 根据群组ID查询聊天记录（分页）。
     *
     * @param groupId 群组ID
     * @param offset  查询偏移量
     * @param limit   查询数量
     * @return GroupMessage 对象列表
     */
    @Select("SELECT * FROM group_messages " +
            "WHERE group_id = #{groupId} " +
            "ORDER BY sent_time DESC " +
            "LIMIT #{limit} OFFSET #{offset}")
    List<GroupMessage> findMessagesByGroupId(@Param("groupId") int groupId,
                                             @Param("limit") int limit,
                                             @Param("offset") int offset);


    /**
     * (可选) 查询指定群组的最后一条聊天记录。
     *
     * @param groupId 群组ID
     * @return 最后一条 GroupMessage 对象，如果没有则返回 null
     */
    @Select("SELECT * FROM group_messages " +
            "WHERE group_id = #{groupId} " +
            "ORDER BY sent_time DESC " +
            "LIMIT 1")
    GroupMessage findLastMessageInGroup(@Param("groupId") int groupId);


}
