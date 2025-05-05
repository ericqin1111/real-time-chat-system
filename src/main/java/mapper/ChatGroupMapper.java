package mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.ChatGroup;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ChatGroupMapper extends BaseMapper<ChatGroup> {
    @Select("SELECT * FROM chat_groups WHERE group_id = #{id}")
    ChatGroup findGroupById(@Param("id") int id); // 参数名 'id' 对应 #{id}

    /**
     * 根据群组ID列表查找群组信息列表。
     *
     * @param ids 群组ID列表
     * @return ChatGroup 对象列表
     */
    @Select("<script>" +
            "SELECT * FROM chat_groups WHERE group_id IN " +
            "<foreach item='id' collection='ids' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "<if test='ids == null or ids.size() == 0'>" +
            "AND 1=0" + // Avoid error on empty list
            "</if>" +
            "</script>")
    List<ChatGroup> findGroupsByIds(@Param("ids") List<Integer> ids);
}
