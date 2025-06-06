package org.chatRoom;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest
@AutoConfigureMockMvc
public class InterceptorTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testInterceptor() throws Exception{
        mockMvc.perform(get("/asdf/value"))
                .andExpect(status().isOk())
                .andDo(result -> System.out.println("拦截器是否触发"));
    }
}
