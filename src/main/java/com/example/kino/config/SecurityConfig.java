package com.example.kino.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    private static final String LOGIN_PATH = "/login"; // change if your front-end login route differs

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(
                            "http://localhost:3000",
                            "https://kino-client-steel.vercel.app"
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors().and()
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
            .authorizeHttpRequests(auth -> auth
                // permit auth endpoints and optional client-side login route
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/login").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // Preflight fix
                .anyRequest().authenticated()
            )
            // handle authentication failures consistently for browser vs API clients
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new ApiOrBrowserAuthEntryPoint(LOGIN_PATH))
            )
            // register JWT filter before UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * AuthenticationEntryPoint that redirects browsers to login (when Accept: text/html)
     * and returns 401 JSON for API/XHR/fetch requests.
     */
    private static class ApiOrBrowserAuthEntryPoint implements AuthenticationEntryPoint {
        private final String loginPath;

        public ApiOrBrowserAuthEntryPoint(String loginPath) {
            this.loginPath = loginPath;
        }

        @Override
        public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
            String accept = request.getHeader("Accept");
            String xRequestedWith = request.getHeader("X-Requested-With");

            boolean looksLikeBrowserNavigation = accept != null && accept.contains("text/html") &&
                    (xRequestedWith == null || !"XMLHttpRequest".equalsIgnoreCase(xRequestedWith));

            if (looksLikeBrowserNavigation) {
                // redirect browser so SPA route can show login page
                response.sendRedirect(request.getContextPath() + loginPath);
            } else {
                // API / XHR / fetch requests -> consistent 401 + JSON body
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"unauthenticated\"}");
            }
        }
    }
}
