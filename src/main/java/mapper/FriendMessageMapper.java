package mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.FriendMessage;
import entity.GroupMessage;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface FriendMessageMapper extends BaseMapper<FriendMessage> {
    @Select("select * from friend_messages where sender_id = #{sendId} and receiver_id = #{receiveId}")
    List<FriendMessage> findByGroupId(int sendId, int receiveId);
}
