package org.chatRoom;

import org.chatRoom.entity.Category;
import org.chatRoom.persistence.CategoryMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = PetStoreSsmDevApplication.class)
class PetStoreSsmDevApplicationTests {

    @Autowired
    private CategoryMapper categoryMapper;
    @Test
    void contextLoads() {
       Category category= categoryMapper.selectById("CATS");
        System.out.println(category);

    }

}
