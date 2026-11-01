package dev.emmanuel.interviewpair.config;

import dev.emmanuel.interviewpair.interview.SessionSocketHandler;
import dev.emmanuel.interviewpair.interview.TicketHandshakeInterceptor;
import dev.emmanuel.interviewpair.interview.WsTicketService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final SessionSocketHandler handler;
    private final WsTicketService tickets;

    public WebSocketConfig(SessionSocketHandler handler, WsTicketService tickets) {
        this.handler = handler;
        this.tickets = tickets;
    }

    // Only pages from this same site may open the socket. That is the default, stated here on
    // purpose so nobody widens it by accident.
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/sessions")
                .addInterceptors(new TicketHandshakeInterceptor(tickets))
                .setAllowedOrigins();
    }
}
