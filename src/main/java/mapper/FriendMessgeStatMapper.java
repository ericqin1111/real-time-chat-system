package mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.FriendMessageStat;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

public interface FriendMessgeStatMapper extends BaseMapper<FriendMessageStat> {
    @Select("SELECT * FROM friend_message_stats WHERE user_id = #{userId} AND friend_id = #{friendId}")
    FriendMessageStat findStats(@Param("userId") int userId, @Param("friendId") int friendId);

    /**
     * 获取特定好友关系的总消息数。
     *
     * @param userId   用户ID
     * @param friendId 好友ID
     * @return 总消息数，如果记录不存在可能返回 null 或需要 COALESCE 处理
     */
    @Select("SELECT total_count FROM friend_message_stats WHERE user_id = #{userId} AND friend_id = #{friendId}")
    Integer getTotalCount(@Param("userId") int userId, @Param("friendId") int friendId);


    /**
     * 获取特定好友关系的已读消息数。
     *
     * @param userId   用户ID
     * @param friendId 好友ID
     * @return 已读消息数，如果记录不存在可能返回 null 或需要 COALESCE 处理
     */
    @Select("SELECT read_count FROM friend_message_stats WHERE user_id = #{userId} AND friend_id = #{friendId}")
    Integer getReadCount(@Param("userId") int userId, @Param("friendId") int friendId);

    /**
     * 计算特定好友关系的未读消息数。
     *
     * @param userId   用户ID
     * @param friendId 好友ID
     * @return 未读消息数
     */
    @Select("SELECT COALESCE(total_count, 0) - COALESCE(read_count, 0) FROM friend_message_stats " +
            "WHERE user_id = #{userId} AND friend_id = #{friendId}")
    int getUnreadCount(@Param("userId") int userId, @Param("friendId") int friendId);


    /**
     * 获取一个用户与其所有好友的总消息数映射 (好友ID -> 总数)。
     *
     * @param userId 用户ID
     * @return Map<好友ID, 总消息数>
     */
    @MapKey("friend_id") // 指定 Map 的 Key 是 friend_id 列
    @Select("SELECT friend_id, total_count FROM friend_message_stats WHERE user_id = #{userId}")
    Map<Integer, Integer> getTotalCountsForUser(@Param("userId") int userId);


    /**
     * 计算一个用户所有好友关系的总未读消息数。
     *
     * @param userId 用户ID
     * @return 总未读数
     */
    @Select("SELECT SUM(COALESCE(total_count, 0) - COALESCE(read_count, 0)) FROM friend_message_stats " +
            "WHERE user_id = #{userId}")
    int getTotalUnreadCountForUser(@Param("userId") int userId);
}
