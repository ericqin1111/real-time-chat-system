package mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.GroupMessageStat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;


public interface GroupMessageStatMapper extends BaseMapper<GroupMessageStat> {

    @Select("select total_messages + 1 from group_message_stats where group_id = #{groupid} for update")
    int getTotalMessagesByGroupId(int groupid);

    @Update("update group_message_stats set total_messages = total_messages + 1 where group_id = #{groupid}")
    void updateTotalMessages(Integer groupid);
}
