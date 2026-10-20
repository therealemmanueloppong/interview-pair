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
                // Bearer, never Basic: a Basic challenge is what makes browsers show a login box.
                .andExpect(header().string("WWW-Authenticate", org.hamcrest.Matchers.startsWith("Bearer")));
    }

    @Test
    void frontendRoutesAreNotBlockedBySecurity() throws Exception {
        // Depending on whether a frontend build is on the classpath these are 200 or 404, never 401.
        for (String path : new String[] {"/", "/dashboard/settings"}) {
            mvc.perform(get(path)).andExpect(result ->
                    org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
        }
    }

    @Test
    void unknownApiPathsAreNeverAnsweredWithTheFrontend() throws Exception {
        mvc.perform(get("/api/nope")).andExpect(status().isUnauthorized());
    }
}
