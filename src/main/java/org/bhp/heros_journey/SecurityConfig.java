package org.bhp.heros_journey;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Enable CSRF protection
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                )
                // Configure security headers
                .headers(headers -> headers
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .preload(true)
                                .maxAgeInSeconds(31536000)
                        )
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                        // Content Security Policy for this game application:
                        // - 'self' allows resources from the same origin only (no external CDNs)
                        // - script-src 'self' 'unsafe-inline' is intentional for game.js dynamic DOM updates
                        // - style-src 'self' 'unsafe-inline' for inline game styles
                        // - This policy is appropriate because:
                        //   1. The game is self-contained (no external API calls except AI chat)
                        //   2. All static assets (game.js, style.css) are served from 'self'
                        //   3. Inline scripts are used for game UI updates (appendLog, etc.)
                        // - Trade-off: 'unsafe-inline' is less restrictive but necessary for dynamic game behavior
                        //   Future optimization: Consider extracting inline styles/scripts to separate files
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'")
                        )
                )
                // Disable HTTP Basic authentication
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


