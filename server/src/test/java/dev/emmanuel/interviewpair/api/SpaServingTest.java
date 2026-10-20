package dev.emmanuel.interviewpair.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SpaServingTest {

    @Autowired
    MockMvc mvc;

    @Test
    void protectedApiAnswers401WithoutABrowserLoginPrompt() throws Exception {
        mvc.perform(get("/api/matches"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("WWW-Authenticate"));
    }

    @Test
    void frontendRoutesAreNotBlockedBySecurity() throws Exception {
        // No built frontend exists in unit tests, so these are 404, never 401.
        mvc.perform(get("/")).andExpect(status().isNotFound());
        mvc.perform(get("/dashboard/settings")).andExpect(status().isNotFound());
    }

    @Test
    void unknownApiPathsAreNeverAnsweredWithTheFrontend() throws Exception {
        mvc.perform(get("/api/nope")).andExpect(status().isUnauthorized());
    }
}
