package org.chatRoom;

import config.MyBatisConfig;
import mapper.FriendMessageMapper;
import mapper.GroupMessageStatMapper;
import mapper.UserMapper;
import org.junit.jupiter.api.Test;



public class mysqlTest {



    @Test
    public void testMysql() throws Exception{
        MyBatisConfig.init();
        MyBatisConfig.execute(sqlSession -> {
            UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
            FriendMessageMapper friendMessageMapper = sqlSession.getMapper(FriendMessageMapper.class);
            System.out.println(friendMessageMapper.findByGroupId(1, 2));


        });
    }
}

