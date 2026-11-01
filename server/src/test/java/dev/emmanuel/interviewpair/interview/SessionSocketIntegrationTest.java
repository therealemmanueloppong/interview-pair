package dev.emmanuel.interviewpair.interview;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jayway.jsonpath.JsonPath;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

/** Two real WebSocket clients against a real server: the live part of a mock interview. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SessionSocketIntegrationTest {

    @Value("${local.server.port}") int port;

    private final HttpClient http = HttpClient.newHttpClient();
    private final java.util.List<WebSocket> open = new java.util.ArrayList<>();

    private record Person(String token, String id) {}

    /** Collects every text message a socket receives. */
    private static class Inbox implements WebSocket.Listener {
        final BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        final StringBuilder partial = new StringBuilder();
        volatile int closeCode = -1;

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            partial.append(data);
            if (last) {
                messages.add(partial.toString());
                partial.setLength(0);
            }
            ws.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int code, String reason) {
            closeCode = code;
            return null;
        }

        @Override
        public void onOpen(WebSocket ws) {
            ws.request(1);
        }

        /** Waits for the next message of this type, skipping other kinds (such as presence). */
        String next(String type) throws InterruptedException {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (System.nanoTime() < deadline) {
                String message = messages.poll(200, TimeUnit.MILLISECONDS);
                if (message != null && type.equals(JsonPath.read(message, "$.type"))) {
                    return message;
                }
            }
            throw new AssertionError("No '" + type + "' message arrived within 5 seconds");
        }

        /** True if a message of this type arrives within the wait time. */
        boolean receives(String type, Predicate<String> matching, Duration wait) throws InterruptedException {
            long deadline = System.nanoTime() + wait.toNanos();
            while (System.nanoTime() < deadline) {
                String message = messages.poll(100, TimeUnit.MILLISECONDS);
                if (message != null && type.equals(JsonPath.read(message, "$.type")) && matching.test(message)) {
                    return true;
                }
            }
            return false;
        }
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private String send(String method, String path, String token, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url(path)))
                .header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (token != null) {
            b.header("Authorization", "Bearer " + token);
        }
        HttpResponse<String> response = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as(method + " " + path + " -> " + response.body()).isLessThan(300);
        return response.body();
    }

    private Person register(String name) throws Exception {
        String json = send("POST", "/api/auth/register", null, """
                {"email":"%s@example.com","password":"correct horse","displayName":"%s"}"""
                .formatted(UUID.randomUUID(), name));
        return new Person(JsonPath.read(json, "$.token"), JsonPath.read(json, "$.user.id"));
    }

    private String matchedSession(Person first, Person second) throws Exception {
        Instant slot = TestSlots.unique();
        String body = """
                {"topic":"ALGORITHMS","level":"BEGINNER","slotStart":"%s"}""".formatted(slot);
        send("POST", "/api/interview-requests", first.token(), body);
        return JsonPath.read(send("POST", "/api/interview-requests", second.token(), body), "$.sessionId");
    }

    private String ticket(Person person, String sessionId) throws Exception {
        return JsonPath.read(send("POST", "/api/sessions/" + sessionId + "/ws-ticket", person.token(), null), "$.ticket");
    }

    private Inbox connect(Person person, String sessionId) throws Exception {
        Inbox inbox = new Inbox();
        WebSocket socket = http.newWebSocketBuilder()
                .buildAsync(URI.create("ws://localhost:" + port + "/ws/sessions?ticket=" + ticket(person, sessionId)), inbox)
                .get(5, TimeUnit.SECONDS);
        open.add(socket);
        return inbox;
    }

    @BeforeEach
    void clearSockets() {
        open.clear();
    }

    @AfterEach
    void closeSockets() {
        open.forEach(ws -> ws.sendClose(WebSocket.NORMAL_CLOSURE, "test over"));
    }

    @Test
    void theInterviewerSeesTheCodeAsTheIntervieweeTypesAndRolesCanSwitch() throws Exception {
        Person ada = register("Ada");
        Person grace = register("Grace");
        String id = matchedSession(ada, grace);

        Inbox adaInbox = connect(ada, id);
        Inbox graceInbox = connect(grace, id);

        // Each person gets the session as they may see it.
        String adaState = adaInbox.next("state");
        String graceState = graceInbox.next("state");
        assertThat((Boolean) JsonPath.read(adaState, "$.session.youAreInterviewee")).isTrue();
        assertThat((Object) JsonPath.read(adaState, "$.session.question.hints")).isNull();
        assertThat((Boolean) JsonPath.read(graceState, "$.session.youAreInterviewee")).isFalse();
        assertThat((Object) JsonPath.read(graceState, "$.session.question.hints")).isNotNull();

        // Both people appear as online.
        assertThat(graceInbox.receives("presence", m -> ((java.util.List<?>) JsonPath.read(m, "$.online")).size() == 2,
                Duration.ofSeconds(3))).isTrue();

        // Ada types, Grace sees it.
        WebSocket adaSocket = open.get(0);
        adaSocket.sendText("{\"type\":\"code\",\"text\":\"def has_duplicate(nums):\\n    return True\"}", true);
        String relayed = graceInbox.next("code");
        assertThat((String) JsonPath.read(relayed, "$.text")).isEqualTo("def has_duplicate(nums):\n    return True");

        // Grace is only watching, so her edit is refused and Ada never sees it.
        WebSocket graceSocket = open.get(1);
        graceSocket.sendText("{\"type\":\"code\",\"text\":\"sneaky edit\"}", true);
        assertThat((String) JsonPath.read(graceInbox.next("error"), "$.message")).contains("Only the person answering");
        // A refused edit also resyncs the sender with a fresh state, so read it here to keep the queue in order.
        assertThat((Boolean) JsonPath.read(graceInbox.next("state"), "$.session.youAreInterviewee")).isFalse();
        assertThat(adaInbox.receives("code", m -> true, Duration.ofMillis(700))).isFalse();

        // Switching roles pushes a new state to both people and flips who may type.
        send("POST", "/api/sessions/" + id + "/switch", grace.token(), null);
        String adaAfter = adaInbox.next("state");
        String graceAfter = graceInbox.next("state");
        assertThat((Boolean) JsonPath.read(adaAfter, "$.session.youAreInterviewee")).isFalse();
        assertThat((Boolean) JsonPath.read(graceAfter, "$.session.youAreInterviewee")).isTrue();
        assertThat((Integer) JsonPath.read(graceAfter, "$.session.round")).isEqualTo(2);
        assertThat((Object) JsonPath.read(graceAfter, "$.session.question.hints")).isNull();

        graceSocket.sendText("{\"type\":\"code\",\"text\":\"grace answers now\"}", true);
        assertThat((String) JsonPath.read(adaInbox.next("code"), "$.text")).isEqualTo("grace answers now");
    }

    @Test
    void aTicketOpensOneSocketOnlyOnce() throws Exception {
        Person ada = register("Ada");
        Person grace = register("Grace");
        String id = matchedSession(ada, grace);
        String ticket = ticket(ada, id);
        URI uri = URI.create("ws://localhost:" + port + "/ws/sessions?ticket=" + ticket);

        WebSocket first = http.newWebSocketBuilder().buildAsync(uri, new Inbox()).get(5, TimeUnit.SECONDS);
        open.add(first);

        assertThatThrownBy(() -> http.newWebSocketBuilder().buildAsync(uri, new Inbox()).get(5, TimeUnit.SECONDS))
                .hasStackTraceContaining("401");
    }

    @Test
    void aSocketWithoutAValidTicketIsRefused() {
        for (String query : new String[] {"", "?ticket=", "?ticket=made-up"}) {
            assertThatThrownBy(() -> http.newWebSocketBuilder()
                    .buildAsync(URI.create("ws://localhost:" + port + "/ws/sessions" + query), new Inbox())
                    .get(5, TimeUnit.SECONDS))
                    .as(query)
                    .hasStackTraceContaining("401");
        }
    }

    @Test
    void openingTheSessionInASecondTabClosesTheFirstWithoutAReconnectLoop() throws Exception {
        Person ada = register("Ada");
        Person grace = register("Grace");
        String id = matchedSession(ada, grace);

        Inbox firstTab = connect(ada, id);
        firstTab.next("state");
        Inbox secondTab = connect(ada, id);
        secondTab.next("state");

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (firstTab.closeCode == -1 && System.nanoTime() < deadline) {
            Thread.sleep(50);
        }
        assertThat(firstTab.closeCode).isEqualTo(SessionSocketHandler.REPLACED);
    }

    @Test
    void aLongSolutionFitsInOneMessage() throws Exception {
        Person ada = register("Ada");
        Person grace = register("Grace");
        String id = matchedSession(ada, grace);
        Inbox adaInbox = connect(ada, id);
        Inbox graceInbox = connect(grace, id);
        adaInbox.next("state");
        graceInbox.next("state");

        String longCode = "x = 1\n".repeat(3000); // 18,000 characters, far above Tomcat's 8 KB default
        open.get(0).sendText("{\"type\":\"code\",\"text\":\"" + longCode.replace("\n", "\\n") + "\"}", true);

        assertThat((String) JsonPath.read(graceInbox.next("code"), "$.text")).isEqualTo(longCode);
    }
}
