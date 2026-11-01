package dev.emmanuel.interviewpair.interview;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class InterviewApiTest {

    @Autowired MockMvc mvc;

    private record Person(String token, String id) {}

    private Person register(String name) throws Exception {
        String body = """
                {"email":"%s@example.com","password":"correct horse","displayName":"%s"}"""
                .formatted(UUID.randomUUID(), name);
        String json = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new Person(JsonPath.read(json, "$.token"), JsonPath.read(json, "$.user.id"));
    }

    private ResultActions as(Person p, org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder b)
            throws Exception {
        return mvc.perform(b.header("Authorization", "Bearer " + p.token()));
    }

    private ResultActions request(Person p, String topic, String level, Instant slot) throws Exception {
        String body = """
                {"topic":"%s","level":"%s","slotStart":"%s"}""".formatted(topic, level, slot);
        return as(p, post("/api/interview-requests").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String matched(Person first, Person second) throws Exception {
        Instant slot = TestSlots.unique();
        request(first, "ALGORITHMS", "BEGINNER", slot).andExpect(status().isCreated());
        String json = request(second, "ALGORITHMS", "BEGINNER", slot)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("MATCHED"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.sessionId");
    }

    @Test
    void everyEndpointNeedsALogin() throws Exception {
        UUID id = UUID.randomUUID();
        mvc.perform(get("/api/sessions")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/sessions/" + id)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/sessions/" + id + "/switch")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/sessions/" + id + "/ws-ticket")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/interview-requests/mine")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/interview-requests").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aWaitingRequestIsListedAndCanBeCancelled() throws Exception {
        Person ada = register("Ada");
        Instant slot = TestSlots.unique();

        String created = request(ada, "BEHAVIORAL", "INTERMEDIATE", slot)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.topicLabel").value("Behavioral"))
                .andExpect(jsonPath("$.slotStart").value(slot.toString()))
                .andReturn().getResponse().getContentAsString();

        as(ada, get("/api/interview-requests/mine")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));

        String id = JsonPath.read(created, "$.id");
        as(ada, delete("/api/interview-requests/" + id)).andExpect(status().isNoContent());
        as(ada, get("/api/interview-requests/mine")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void badRequestsGetClearErrors() throws Exception {
        Person ada = register("Ada");

        as(ada, post("/api/interview-requests").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        request(ada, "ALGORITHMS", "BEGINNER", Instant.parse("2020-01-01T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("That time slot has already passed"));
        as(ada, post("/api/interview-requests").contentType(MediaType.APPLICATION_JSON)
                .content("{\"topic\":\"NOT_A_TOPIC\",\"level\":\"BEGINNER\",\"slotStart\":\"2030-01-01T00:00:00Z\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void thePersonAnsweringNeverReceivesTheHintsInTheJson() throws Exception {
        Person ada = register("Ada");
        Person grace = register("Grace");
        String id = matched(ada, grace);

        as(ada, get("/api/sessions/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.youAreInterviewee").value(true))
                .andExpect(jsonPath("$.question.hints").value(nullValue()))
                .andExpect(jsonPath("$.question.solution").value(nullValue()))
                .andExpect(jsonPath("$.partner.displayName").value("Grace"));
        as(grace, get("/api/sessions/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.youAreInterviewee").value(false))
                .andExpect(jsonPath("$.question.hints", hasSize(3)))
                .andExpect(jsonPath("$.question.solution").isNotEmpty());
    }

    @Test
    void anOutsiderCannotReadTheSessionOrGetATicketForIt() throws Exception {
        Person ada = register("Ada");
        Person grace = register("Grace");
        Person mallory = register("Mallory");
        String id = matched(ada, grace);

        as(mallory, get("/api/sessions/" + id)).andExpect(status().isNotFound());
        as(mallory, post("/api/sessions/" + id + "/ws-ticket")).andExpect(status().isNotFound());
        as(mallory, post("/api/sessions/" + id + "/switch")).andExpect(status().isNotFound());
        as(ada, post("/api/sessions/" + id + "/ws-ticket")).andExpect(status().isOk())
                .andExpect(jsonPath("$.ticket").isNotEmpty());
    }

    @Test
    void switchingRolesThroughTheApiFlipsTheViews() throws Exception {
        Person ada = register("Ada");
        Person grace = register("Grace");
        String id = matched(ada, grace);

        as(grace, post("/api/sessions/" + id + "/switch")).andExpect(status().isOk())
                .andExpect(jsonPath("$.round").value(2))
                .andExpect(jsonPath("$.youAreInterviewee").value(true));
        as(ada, post("/api/sessions/" + id + "/switch")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("The roles have already been switched"));
        as(ada, get("/api/sessions")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].round").value(2))
                .andExpect(jsonPath("$[0].partner.displayName").value("Grace"));
    }

    @Test
    void feedbackIsValidatedAndOnlyShownToTheReceiver() throws Exception {
        Person ada = register("Ada");
        Person grace = register("Grace");
        String id = matched(ada, grace);

        as(ada, post("/api/sessions/" + id + "/feedback").contentType(MediaType.APPLICATION_JSON)
                .content("{\"rating\":5,\"comment\":\"Great\"}")).andExpect(status().isConflict());

        as(ada, post("/api/sessions/" + id + "/finish")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));

        as(ada, post("/api/sessions/" + id + "/feedback").contentType(MediaType.APPLICATION_JSON)
                .content("{\"rating\":6,\"comment\":\"Too high\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.rating").exists());
        as(ada, post("/api/sessions/" + id + "/feedback").contentType(MediaType.APPLICATION_JSON)
                .content("{\"rating\":4,\"comment\":\"   \"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.comment").exists());

        as(ada, post("/api/sessions/" + id + "/feedback").contentType(MediaType.APPLICATION_JSON)
                .content("{\"rating\":4,\"comment\":\"Explained trade-offs clearly.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.given").value(true))
                .andExpect(jsonPath("$.received").value(nullValue()));

        as(grace, get("/api/sessions/" + id + "/feedback")).andExpect(status().isOk())
                .andExpect(jsonPath("$.given").value(false))
                .andExpect(jsonPath("$.received.rating").value(4))
                .andExpect(jsonPath("$.received.comment").value("Explained trade-offs clearly."));
    }
}
