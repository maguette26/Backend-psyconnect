package ma.osbt.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.Map;

public class JsonUsernamePasswordAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private JwtUtils jwtUtils;

    public void setJwtUtils(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request,
                                                HttpServletResponse response)
            throws AuthenticationException {
        try {
            Map<String, String> credentials = objectMapper.readValue(
                request.getInputStream(), Map.class);
            String username = credentials.get("username");
            String password = credentials.get("password");

            UsernamePasswordAuthenticationToken authRequest =
                    new UsernamePasswordAuthenticationToken(username, password);
            setDetails(request, authRequest);
            return this.getAuthenticationManager().authenticate(authRequest);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain,
                                            Authentication authResult)
            throws IOException, ServletException {

        String token = jwtUtils.generateToken(authResult);

        String rawRole = authResult.getAuthorities().iterator().next().getAuthority();
        String roleWithoutPrefix = rawRole.startsWith("ROLE_") ? rawRole.substring(5) : rawRole;

        Long id = null;
        try {
            java.lang.reflect.Method getId = authResult.getPrincipal()
                .getClass().getMethod("getId");
            Object result = getId.invoke(authResult.getPrincipal());
            if (result instanceof Long l) id = l;
        } catch (Exception ignored) {}

        Map<String, Object> responseBody = Map.of(
            "message", "Authentification réussie",
            "token", token,
            "id", id != null ? id : -1L,
            "email", authResult.getName(),
            "role", roleWithoutPrefix
        );

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        objectMapper.writeValue(response.getWriter(), responseBody);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request,
                                              HttpServletResponse response,
                                              AuthenticationException failed)
            throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        objectMapper.writeValue(response.getWriter(), Map.of(
            "error", "Échec de l'authentification",
            "message", failed.getMessage()
        ));
    }
}