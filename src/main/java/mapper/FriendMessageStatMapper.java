package mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.FriendMessageStat;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface FriendMessageStatMapper extends BaseMapper<FriendMessageStat> {
    @Select("select total_count + 1 from friend_message_stats where user_id = #{userId} and friend_id = #{friendId} for update")
    int getTotal(int userId,int friendId);

    @Update("update friend_message_stats set total_count = total_count + 1  where user_id = #{userId} and friend_id = #{friendId}")
    void updateTotalCount(int userId,int friendId);
}
