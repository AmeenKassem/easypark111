package com.example.demo.config;

import com.example.demo.security.JwtService;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);

    private final JwtService jwtService;

    public WebSocketConfig(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                // IMPORTANT: use getAccessor(), NOT StompHeaderAccessor.wrap().
                // wrap() copies the headers into a throwaway accessor, so setUser()
                // would be lost and the principal would never be attached to the
                // WebSocket session. getAccessor() returns the live, mutable accessor
                // bound to this message so the principal is registered in the
                // SimpUserRegistry and convertAndSendToUser(...) can resolve it.
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");

                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);
                        try {
                            Claims claims = jwtService.parseClaims(token);
                            Long userId = Long.valueOf(claims.getSubject());

                            accessor.setUser(
                                    new UsernamePasswordAuthenticationToken(
                                            String.valueOf(userId),
                                            null,
                                            List.of()
                                    )
                            );
                        } catch (Exception ex) {
                            // Expired/invalid token: connect without a principal so the
                            // client doesn't crash-loop. The user simply receives no
                            // real-time pushes until they re-authenticate with a fresh
                            // token (a new STOMP CONNECT will then succeed).
                            log.warn("action=ws_connect_auth_failed reason={}", ex.getClass().getSimpleName());
                        }
                    }
                }

                return message;
            }
        });
    }
}
