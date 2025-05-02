package mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.GroupMessage;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface GroupMessageMapper extends BaseMapper<GroupMessage> {

    @Select("select * from group_messages where group_id = #{groupId}")
    List<GroupMessage> findByGroupId(int groupId);


}
