package org.bhp.heros_journey;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
                // Enable CSRF protection with SameSite=Strict
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                )
                // Configure SameSite cookie attribute
                .httpBasic(AbstractHttpConfigurer::disable)
                // Allow public access to static resources and the game endpoint
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/", "/index.html", "/style.css", "/game.js").permitAll()
                        .requestMatchers("/api/game/**").permitAll()
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}


