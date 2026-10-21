package com.resonanceplanner.notification;

import com.resonanceplanner.common.config.AppProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/** The WebSocket of the real-time notifications (D135): plain WebSocket on /ws, without STOMP and without SockJS. */
@Configuration(proxyBeanMethods = false)
@EnableWebSocket
public class NotificationSocketConfig implements WebSocketConfigurer {

    public static final String PATH = "/ws";

    private final NotificationSocketHandler handler;
    private final AppProperties appProperties;

    public NotificationSocketConfig(NotificationSocketHandler handler, AppProperties appProperties) {
        this.handler = handler;
        this.appProperties = appProperties;
    }

    // From a browser, only the frontend may open it: the same origins as the CORS of the API (D48)
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, PATH)
                .setAllowedOrigins(appProperties.cors().allowedOrigins().toArray(String[]::new));
    }
}
