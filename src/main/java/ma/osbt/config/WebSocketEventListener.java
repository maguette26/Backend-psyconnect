package ma.osbt.config;

import lombok.RequiredArgsConstructor;
import ma.osbt.dto.ChatNotification;
import ma.osbt.dto.OnlineStatusDTO;
import ma.osbt.entitie.Personne;
import ma.osbt.repository.PersonneRepository;
import ma.osbt.service.implementation.OnlineStatusService;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.*;

@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final OnlineStatusService onlineStatusService;
    private final SimpMessagingTemplate messagingTemplate;
    private final PersonneRepository personneRepository; 

    @EventListener
    public void handleConnect(SessionConnectedEvent event) {

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        if (accessor.getUser() == null) return;

        String email = accessor.getUser().getName();

        Personne user = personneRepository.findByEmail(email)
                .orElse(null);

        if (user == null) return;

        onlineStatusService.userConnected(user.getId(), sessionId);

        messagingTemplate.convertAndSend(
                "/topic/online-status",
                new ChatNotification(
                        ChatNotification.Type.USER_ONLINE,
                        new OnlineStatusDTO(user.getId(), user.getUsername(), true)
                )
        );
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        Long userId = onlineStatusService.userDisconnected(sessionId);
        if (userId == null) return;

        messagingTemplate.convertAndSend(
                "/topic/online-status",
                new ChatNotification(
                        ChatNotification.Type.USER_OFFLINE,
                        new OnlineStatusDTO(userId, "unknown", false)
                )
        );
    }
}