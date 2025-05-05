package mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.UserFriend;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface UserFriendMapper extends BaseMapper<UserFriend> {

    @Select("select friend_id from user_friends where user_id=#{userId}")
    List<Integer> findFriendIdsByUserIds(@Param("userId") Integer userId);
}
