package ma.osbt.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final JwtUtils jwtUtils;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(UserDetailsService userDetailsService,
                          JwtUtils jwtUtils,
                          JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.userDetailsService = userDetailsService;
        this.jwtUtils = jwtUtils;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        JsonUsernamePasswordAuthenticationFilter jsonAuthFilter =
                new JsonUsernamePasswordAuthenticationFilter();
        jsonAuthFilter.setAuthenticationManager(
                authenticationManager(http.getSharedObject(AuthenticationConfiguration.class)));
        jsonAuthFilter.setFilterProcessesUrl("/api/auth/login");
        jsonAuthFilter.setJwtUtils(jwtUtils);

        http
            .securityMatcher("/api/**")
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(
                    "/api/auth/**",
                    "/api/public/**",
                    "/api/professionnels/inscription",
                    "/api/fonctionnalites/citations",
                    "/api/fonctionnalites/ressources/**",
                    "/api/forum/**",
                    "/webhook",
                    "/error",
                    "/",
                    "/api/disponibilites/publiques",
                    "/api/ai/analyze-emotion",
                    "/api/ai/chat",
                    "/webhook/stripe/**",
                    "/webhook/paypal/**"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/fonctionnalites/**")
                    .hasAnyRole("USER", "PSYCHOLOGUE", "PSYCHIATRE", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/fonctionnalites/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/fonctionnalites/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/fonctionnalites/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/fonctionnalites/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/disponibilites")
                    .hasAnyRole("USER", "PSYCHOLOGUE", "PSYCHIATRE")
                .requestMatchers(HttpMethod.GET, "/api/disponibilites/**")
                    .hasAnyRole("USER", "PSYCHOLOGUE", "PSYCHIATRE")
                .requestMatchers(HttpMethod.POST, "/api/disponibilites/**")
                    .hasAnyRole("PSYCHOLOGUE", "PSYCHIATRE")
                .requestMatchers(HttpMethod.PUT, "/api/disponibilites/**")
                    .hasAnyRole("PSYCHOLOGUE", "PSYCHIATRE")
                .requestMatchers(HttpMethod.PATCH, "/api/disponibilites/**")
                    .hasAnyRole("PSYCHOLOGUE", "PSYCHIATRE")
                .requestMatchers(HttpMethod.DELETE, "/api/disponibilites/**")
                    .hasAnyRole("PSYCHOLOGUE", "PSYCHIATRE")
                .requestMatchers("/api/humeurs/**").hasRole("USER")
                .requestMatchers("/api/reservations/mes-reservations").hasRole("USER")
                .requestMatchers("/api/consultations/**").hasRole("USER")
                .requestMatchers(HttpMethod.POST, "/api/reservations").hasRole("USER")
                .requestMatchers("/api/reservations/annuler/*").hasRole("USER")
                .requestMatchers("/api/professionnels/tous").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/reservations/pro/**")
                    .hasAnyRole("PSYCHOLOGUE", "PSYCHIATRE")
                .requestMatchers(HttpMethod.PATCH, "/api/reservations/statut/**")
                    .hasAnyRole("PSYCHOLOGUE", "PSYCHIATRE")
                .requestMatchers(HttpMethod.PATCH, "/api/professionnel/prix-consultation")
                    .hasAnyRole("PSYCHOLOGUE", "PSYCHIATRE")
                .requestMatchers(HttpMethod.GET, "/api/professionnel/prix-consultation")
                    .hasAnyRole("PSYCHOLOGUE", "PSYCHIATRE")
                .requestMatchers(
                    "/api/professionnels/en-attente",
                    "/api/professionnels/fichiers/**",
                    "/api/utilisateurs/**"
                ).hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/professionnels/validation/*")
                    .hasRole("ADMIN")
                .requestMatchers("/api/import/fonctionnalites").hasRole("ADMIN")
                .requestMatchers("/api/fonctionnalites/premium/access/**")
                    .hasAnyRole("PREMIUM", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/fonctionnalites/upgrade-to-premium/**")
                    .hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/messages/admin/tous").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/admin/supprimer-sujet/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/forum/admin/tous").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/messages/admin/supprimer/**")
                    .hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterAt(jsonAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .addLogoutHandler(logoutHandler())
                .logoutSuccessHandler(logoutSuccessHandler())
                .permitAll()
            );

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain wsFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/ws-message/**", "/ws-consultation-test/**", "/ws-consultation/**")
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain defaultFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/", "/webhook", "/error", "/webhook/stripe/**",
                    "/test/**", "/test-chat.html", "/**/*.html",
                    "/ws-consultation/**", "/ws-consultation-test/**", "/ws-message/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

    @Bean
    public LogoutHandler logoutHandler() {
        return (request, response, authentication) -> {
            if (authentication != null) {
                new SecurityContextLogoutHandler().logout(request, response, authentication);
            }
        };
    }

    @Bean
    public LogoutSuccessHandler logoutSuccessHandler() {
        return (request, response, authentication) -> {
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("Déconnexion réussie");
        };
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Non authentifié\"}");
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
            "http://localhost:5173",
            "http://192.168.1.219",
            "https://frontend-psyconnect.vercel.app"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> builder.simpleDateFormat("HH:mm");
    }
}