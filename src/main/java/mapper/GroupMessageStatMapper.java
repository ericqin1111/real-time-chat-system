package mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.GroupMessageStat;
import org.apache.ibatis.annotations.Update;

public interface GroupMessageStatMapper extends BaseMapper<GroupMessageStat> {
    @Update("update group_message_stats set total_messages = total_messages + 1 where group_id = #{groupid}")
    void updateTotalMessages(Integer groupid);
}
