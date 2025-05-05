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

    @Select("SELECT group_id FROM group_members WHERE user_id = #{userId}")
    List<Integer> findGroupIdsByUserId(@Param("userId") int userId);


    @Select("SELECT COUNT(*) FROM group_members WHERE user_id = #{userId} AND group_id = #{groupId}")
    Integer countUserInGroup(@Param("userId") int userId, @Param("groupId") int groupId);

    // 可以在 Service 层添加一个辅助方法:
    // default boolean isUserInGroup(int userId, int groupId) {
    //     return countUserInGroup(userId, groupId) > 0;
    // }
}
