package com.example.referencedata.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class SecurityIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    MockMvc mvc;

    private static final String BODY = "{\"code\":\"SEC_TEST\",\"name\":\"Тест\",\"active\":true}";

    @Test
    void anonymousIsUnauthorized() throws Exception {
        mvc.perform(get("/api/dictionaries")).andExpect(status().isUnauthorized());
    }

    @Test
    void viewerCanReadButNotWrite() throws Exception {
        mvc.perform(get("/api/dictionaries").with(user("v").roles("VIEWER")))
                .andExpect(status().isOk());
        mvc.perform(post("/api/dictionaries").with(user("v").roles("VIEWER"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void editorCanWrite() throws Exception {
        mvc.perform(post("/api/dictionaries").with(user("e").roles("EDITOR"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated());
    }
}
