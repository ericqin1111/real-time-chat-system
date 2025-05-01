package mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.GroupMember;
import entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface GroupMemberMapper extends BaseMapper<GroupMember> {
    @Select("select user_id from group_members WHERE group_id = #{groupid}")
    List<Integer> findUsersByGroupId(@Param("groupid") Integer groupid);
}
