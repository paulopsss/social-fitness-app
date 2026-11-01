package com.fitly.app;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProtectedPageTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void visitorWithoutLoginIsSentToLogin() throws Exception {
        mockMvc.perform(get("/Main_page.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/Login_page.html"));
    }

    @Test
    void registeringLogsYouInAndOpensMainPage() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/register")
                        .param("email", "new@example.com")
                        .param("password", "secret")
                        .session(session))
                .andExpect(redirectedUrl("/Main_page.html"));

        mockMvc.perform(get("/Main_page.html").session(session))
                .andExpect(status().isOk());
    }

    @Test
    void wrongPasswordSendsYouBackToLogin() throws Exception {
        mockMvc.perform(post("/login")
                        .param("email", "nobody@example.com")
                        .param("password", "nope"))
                .andExpect(redirectedUrl("/Login_page.html?error=1"));
    }
}
