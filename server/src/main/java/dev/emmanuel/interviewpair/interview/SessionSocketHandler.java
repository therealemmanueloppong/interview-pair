package dev.emmanuel.interviewpair.interview;

import dev.emmanuel.interviewpair.interview.InterviewDtos.SessionView;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The live part of a mock interview. Each person has at most one open socket per session.
 *
 * Messages from the browser: {"type":"code","text":"..."} sends the whole code text. Only the
 * interviewee may send code, so there are never two writers and no merge conflicts to resolve.
 *
 * Messages to the browser: "state" (the full session as that person may see it), "code" (the
 * interviewee's latest text), "presence" (who is connected) and "error".
 */
@Component
public class SessionSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(SessionSocketHandler.class);

    /** Close code the browser treats as "do not reconnect": the same person opened another tab. */
    public static final int REPLACED = 4001;

    static final int MAX_MESSAGE_BYTES = 128 * 1024;

    private final InterviewService service;
    private final JsonMapper json;
    private final Map<UUID, Map<UUID, WebSocketSession>> rooms = new ConcurrentHashMap<>();

    public SessionSocketHandler(InterviewService service, JsonMapper json) {
        this.service = service;
        this.json = json;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession raw) throws Exception {
        UUID sessionId = (UUID) raw.getAttributes().get(TicketHandshakeInterceptor.SESSION_ID);
        UUID userId = (UUID) raw.getAttributes().get(TicketHandshakeInterceptor.USER_ID);
        // Tomcat's default text message limit is 8 KB, too small for a long solution. Code is capped at
        // 20,000 characters in the service, and 128 KB leaves room for multi-byte characters.
        raw.setTextMessageSizeLimit(MAX_MESSAGE_BYTES);
        // Sending from several threads at once is not safe on a raw session, so wrap it.
        WebSocketSession session = new ConcurrentWebSocketSessionDecorator(raw, 5_000, 64 * 1024);
        WebSocketSession previous = rooms.computeIfAbsent(sessionId, id -> new ConcurrentHashMap<>()).put(userId, session);
        if (previous != null) {
            closeQuietly(previous, new CloseStatus(REPLACED, "Opened in another tab"));
        }
        sendState(sessionId, userId);
        broadcastPresence(sessionId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession raw, TextMessage message) throws Exception {
        UUID sessionId = (UUID) raw.getAttributes().get(TicketHandshakeInterceptor.SESSION_ID);
        UUID userId = (UUID) raw.getAttributes().get(TicketHandshakeInterceptor.USER_ID);
        JsonNode node;
        try {
            node = json.readTree(message.getPayload());
        } catch (RuntimeException e) {
            raw.close(CloseStatus.BAD_DATA);
            return;
        }
        if (!"code".equals(node.path("type").asString(""))) {
            return;
        }
        JsonNode text = node.path("text");
        if (!text.isString()) {
            return;
        }
        try {
            service.updateCode(userId, sessionId, text.asString());
        } catch (InterviewException e) {
            // Usually a role switch that the sender has not seen yet. Tell them and resync.
            send(room(sessionId).get(userId), Map.of("type", "error", "message", e.getMessage()));
            sendState(sessionId, userId);
            return;
        }
        Map<UUID, WebSocketSession> room = room(sessionId);
        room.forEach((id, session) -> {
            if (!id.equals(userId)) {
                send(session, Map.of("type", "code", "text", text.asString()));
            }
        });
    }

    @Override
    public void afterConnectionClosed(WebSocketSession raw, CloseStatus status) {
        UUID sessionId = (UUID) raw.getAttributes().get(TicketHandshakeInterceptor.SESSION_ID);
        UUID userId = (UUID) raw.getAttributes().get(TicketHandshakeInterceptor.USER_ID);
        Map<UUID, WebSocketSession> room = rooms.get(sessionId);
        if (room == null) {
            return;
        }
        // Only remove our own entry. If the person reconnected, the entry is already the new socket.
        room.computeIfPresent(userId, (id, current) -> sameSocket(current, raw) ? null : current);
        if (room.isEmpty()) {
            rooms.remove(sessionId, room);
        } else {
            broadcastPresence(sessionId);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onSessionChanged(SessionChanged event) {
        Map<UUID, WebSocketSession> room = rooms.get(event.sessionId());
        if (room != null) {
            room.keySet().forEach(userId -> sendState(event.sessionId(), userId));
        }
    }

    private void sendState(UUID sessionId, UUID userId) {
        WebSocketSession session = room(sessionId).get(userId);
        if (session == null) {
            return;
        }
        try {
            SessionView view = service.getSession(userId, sessionId);
            send(session, Map.of("type", "state", "session", view));
        } catch (InterviewException e) {
            closeQuietly(session, CloseStatus.POLICY_VIOLATION);
        }
    }

    private void broadcastPresence(UUID sessionId) {
        Map<UUID, WebSocketSession> room = room(sessionId);
        List<UUID> online = List.copyOf(room.keySet());
        room.values().forEach(session -> send(session, Map.of("type", "presence", "online", online)));
    }

    private Map<UUID, WebSocketSession> room(UUID sessionId) {
        return rooms.getOrDefault(sessionId, Map.of());
    }

    private void send(WebSocketSession session, Object payload) {
        if (session == null) {
            return;
        }
        try {
            session.sendMessage(new TextMessage(json.writeValueAsString(payload)));
        } catch (IOException | RuntimeException e) {
            log.debug("Could not send to a socket: {}", e.toString());
            closeQuietly(session, CloseStatus.SERVER_ERROR);
        }
    }

    private static boolean sameSocket(WebSocketSession current, WebSocketSession raw) {
        return current instanceof ConcurrentWebSocketSessionDecorator decorator
                ? decorator.getDelegate() == raw
                : current == raw;
    }

    private static void closeQuietly(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (IOException | RuntimeException e) {
            // already closed
        }
    }
}
