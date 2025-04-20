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


    @Insert("insert into users (username,password) values (#{username},#{password} )")
    void insertUser(String username,String password);
}
