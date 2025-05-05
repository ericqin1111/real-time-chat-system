package mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.FriendMessage;
import entity.GroupMessage;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface FriendMessageMapper extends BaseMapper<FriendMessage> {
    @Select("select * from friend_messages where sender_id = #{sendId} and receiver_id = #{receiveId}")
    List<FriendMessage> findByGroupId(int sendId, int receiveId);

//    @Insert("INSERT INTO friend_messages (sender_id, receiver_id, content, sent_time) " +
//            "VALUES (#{senderId}, #{receiverId}, #{content}, NOW())") // Assuming sent_time uses DB default
//    @Options(useGeneratedKeys = true, keyProperty = "messageId", keyColumn = "message_id") // 获取自增ID
//    int insert(FriendMessage message);

    /**
     * 查询两个用户之间的聊天记录（分页）。
     *
     * @param userId1 用户1的ID
     * @param userId2 用户2的ID
     * @param offset  查询偏移量
     * @param limit   查询数量
     * @return FriendMessage 对象列表
     */
    @Select("SELECT * FROM friend_messages " +
            "WHERE (sender_id = #{userId1} AND receiver_id = #{userId2}) " +
            "   OR (sender_id = #{userId2} AND receiver_id = #{userId1}) " +
            "ORDER BY sent_time DESC " +
            "LIMIT #{limit} OFFSET #{offset}")
    List<FriendMessage> findMessagesBetweenUsers(@Param("userId1") int userId1,
                                                 @Param("userId2") int userId2,
                                                 @Param("offset") int offset,
                                                 @Param("limit") int limit);

    /**
     * (可选) 查询两个用户之间的最后一条聊天记录。
     *
     * @param userId1 用户1的ID
     * @param userId2 用户2的ID
     * @return 最后一条 FriendMessage 对象，如果没有则返回 null
     */
    @Select("SELECT * FROM friend_messages " +
            "WHERE (sender_id = #{userId1} AND receiver_id = #{userId2}) " +
            "   OR (sender_id = #{userId2} AND receiver_id = #{userId1}) " +
            "ORDER BY sent_time DESC " +
            "LIMIT 1")
    FriendMessage findLastMessageBetweenUsers(@Param("userId1") int userId1,
                                              @Param("userId2") int userId2);
}
