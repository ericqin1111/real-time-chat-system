package mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;


public interface UserMapper extends BaseMapper<User> {
    @Select("select * from users")
    List<User> getAll();
    @Select("select * from users WHERE USERNAME = #{username}")
    User findUserByUsername(@Param("username") String username);

    @Select("select * from users where user_id = #{userId}")
    User findUserById(@Param("userId") String userId);

    @Select("<script>" +
            "SELECT * FROM users " + // 将 'your_user_table' 替换为你的实际用户表名
            "WHERE user_id IN " + // 将 'id_column' 替换为你的实际 ID 列名
            // 使用 foreach 标签遍历集合
            "<foreach item='singleId' collection='ids' open='(' separator=',' close=')'>" +
            "#{singleId}" + // MyBatis 的参数占位符，'singleId' 是循环中的变量名
            "</foreach>" +
            // （可选但推荐）处理空列表或 null 列表，防止生成无效 SQL "WHERE id IN ()"
            "<if test='ids == null or ids.size() == 0'>" +
            // 可以添加一个永远为假的条件，确保在列表为空时不返回任何结果
            "AND 1=0" +
            "</if>" +
            "</script>")
        // 当方法参数是集合且在动态 SQL 中使用时，强烈建议使用 @Param 注解指定名称
    List<User> findUsersByIds(@Param("ids") List<Integer> ids);


    @Insert("insert into users (username,password) values (#{username},#{password} )")
    void insertUser(String username,String password);
}
