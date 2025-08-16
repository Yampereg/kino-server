package com.example.kino.config;

import com.example.kino.auth.JwtService;
import com.example.kino.user.User;
import com.example.kino.user.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final UserRepository userRepository;

    private static final String LOGIN_PATH = "/login"; // change if your login route is different

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        log.debug("JwtAuthenticationFilter: Filtering request - {}", request.getRequestURI());

        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("JwtAuthenticationFilter: No Bearer token found in header");
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);

        try {
            if (!jwtService.validateToken(jwt)) {
                log.info("JwtAuthenticationFilter: JWT validation failed (invalid token)");
                handleInvalidToken(request, response);
                return;
            }

            String userEmail = jwtService.extractEmail(jwt);
            log.debug("JwtAuthenticationFilter: Extracted email from token: {}", userEmail);

            Optional<User> userOpt = userRepository.findByEmail(userEmail);

            if (userOpt.isPresent() && SecurityContextHolder.getContext().getAuthentication() == null) {
                User user = userOpt.get();

                UserDetails userDetails = org.springframework.security.core.userdetails.User
                        .withUsername(user.getEmail())
                        .password(user.getPasswordHash())
                        .authorities("USER")
                        .build();

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.debug("JwtAuthenticationFilter: Authentication set for user: {}", user.getEmail());
            }

            // proceed normally
            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException eje) {
            log.info("JwtAuthenticationFilter: JWT expired for request {}: {}", request.getRequestURI(), eje.getMessage());
            SecurityContextHolder.clearContext();
            handleExpiredToken(request, response);
            return;
        } catch (JwtException | IllegalArgumentException ex) {
            // generic JWT parsing/validation exception
            log.warn("JwtAuthenticationFilter: JWT processing error: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
            handleInvalidToken(request, response);
            return;
        } catch (Exception ex) {
            // unexpected exception - fail safe: clear context and continue (or return 401)
            log.error("JwtAuthenticationFilter: Unexpected error while processing JWT", ex);
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"internal_server_error\"}");
            return;
        }
    }

    private void handleExpiredToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // If the request looks like a browser navigation (Accept: text/html), redirect to login page.
        if (looksLikeBrowserNavigation(request)) {
            log.debug("JwtAuthenticationFilter: Redirecting browser to login due to expired token");
            response.sendRedirect(request.getContextPath() + LOGIN_PATH);
        } else {
            // For API/fetch requests, return 401 JSON so SPA can clear token and redirect client-side.
            log.debug("JwtAuthenticationFilter: Responding 401 JSON for expired token");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"token_expired\"}");
        }
    }

    private void handleInvalidToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (looksLikeBrowserNavigation(request)) {
            log.debug("JwtAuthenticationFilter: Redirecting browser to login due to invalid token");
            response.sendRedirect(request.getContextPath() + LOGIN_PATH);
        } else {
            log.debug("JwtAuthenticationFilter: Responding 401 JSON for invalid token");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"invalid_token\"}");
        }
    }

    private boolean looksLikeBrowserNavigation(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        String xRequestedWith = request.getHeader("X-Requested-With");
        // treat as browser navigation when Accept contains text/html and it's not an X-Requested-With: XMLHttpRequest fetch
        return accept != null && accept.contains("text/html") && (xRequestedWith == null || !"XMLHttpRequest".equalsIgnoreCase(xRequestedWith));
    }
}
