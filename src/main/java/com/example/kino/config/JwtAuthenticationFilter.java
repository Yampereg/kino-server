package com.example.kino.config;

import com.example.kino.auth.JwtService;
import com.example.kino.user.User;
import com.example.kino.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        System.out.println("JwtAuthenticationFilter: Filtering request - " + request.getRequestURI());

        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("JwtAuthenticationFilter: No Bearer token found in header");
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        if (!jwtService.validateToken(jwt)) {
            System.out.println("JwtAuthenticationFilter: Invalid JWT token");
            filterChain.doFilter(request, response);
            return;
        }

        String userEmail = jwtService.extractEmail(jwt);
        System.out.println("JwtAuthenticationFilter: Extracted email from token: " + userEmail);

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
            System.out.println("JwtAuthenticationFilter: Authentication set for user: " + user.getEmail());
        } else if (userOpt.isEmpty()) {
            System.out.println("JwtAuthenticationFilter: No user found with email: " + userEmail);
        } else {
            System.out.println("JwtAuthenticationFilter: Authentication already present in context");
        }

        filterChain.doFilter(request, response);
    }
}
