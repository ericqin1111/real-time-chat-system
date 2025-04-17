package mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;


public interface UserMapper extends BaseMapper<User> {
    @Select("select * from users")
    List<User> getAll();
}
