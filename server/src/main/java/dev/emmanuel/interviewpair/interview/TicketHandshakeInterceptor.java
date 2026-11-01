package dev.emmanuel.interviewpair.interview;

import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

/** Lets a WebSocket open only with a valid one-time ticket, and remembers who it belongs to. */
public class TicketHandshakeInterceptor implements HandshakeInterceptor {

    static final String USER_ID = "userId";
    static final String SESSION_ID = "sessionId";

    private final WsTicketService tickets;

    public TicketHandshakeInterceptor(WsTicketService tickets) {
        this.tickets = tickets;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler handler,
            Map<String, Object> attributes) {
        String ticket = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams().getFirst("ticket");
        Optional<WsTicketService.Ticket> redeemed = tickets.redeem(ticket);
        if (redeemed.isEmpty()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        attributes.put(USER_ID, redeemed.get().userId());
        attributes.put(SESSION_ID, redeemed.get().sessionId());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler handler,
            Exception exception) {
        // nothing to clean up
    }
}
