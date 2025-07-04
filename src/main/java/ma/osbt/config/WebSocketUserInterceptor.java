 

package ma.osbt.config;

 
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
 
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class WebSocketUserInterceptor extends HttpSessionHandshakeInterceptor implements HandshakeInterceptor {
	@Override
	public boolean beforeHandshake(
	        ServerHttpRequest request,
	        ServerHttpResponse response,
	        WebSocketHandler wsHandler,
	        Map<String, Object> attributes) throws Exception {

	    // Optionnel : log ou debug
	    System.out.println("WebSocket Handshake initiated");

	    return super.beforeHandshake(request, response, wsHandler, attributes);
	}

}

