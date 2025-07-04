package ma.osbt.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
	@Autowired
	private WebSocketUserInterceptor webSocketUserInterceptor;

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
	    registry.addEndpoint("/ws-message")
	            .setAllowedOriginPatterns("*")
	            .addInterceptors(webSocketUserInterceptor)
	            .withSockJS();
	}

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");          config.setApplicationDestinationPrefixes("/app");  
        config.setUserDestinationPrefix("/user");  
    }

    
}
