package com.example.referencedata.security;

import com.example.referencedata.view.LoginView;
import com.vaadin.flow.spring.security.VaadinWebSecurity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig extends VaadinWebSecurity {

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** REST API: HTTP Basic, без сессий и CSRF. */
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .httpBasic(basic -> basic.authenticationEntryPoint(this::unauthorized))
                .exceptionHandling(e -> e.accessDeniedHandler(this::forbidden))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

    // Статус выставляется напрямую, без sendError: иначе контейнер перенаправит запрос на /error,
    // который попадает в цепочку Vaadin и превращается в 302 на страницу входа.
    private void unauthorized(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"reference-data\"");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    private void forbidden(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e) {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    /** Vaadin UI: форма входа. */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll());
        super.configure(http);
        setLoginView(http, LoginView.class);
    }
}
