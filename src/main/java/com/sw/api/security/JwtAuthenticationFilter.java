package com.sw.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String jwt = null;
        
        // 1. INTENTO WEB: Buscar en las Cookies primero (Más seguro para navegadores)
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("jwt_token".equals(cookie.getName())) { // Así llamaremos a la cookie luego
                    jwt = cookie.getValue();
                    break;
                }
            }
        }

        // 2. INTENTO MÓVIL: Si no hay cookie, buscar en el Header "Authorization" (Para celulares/Postman)
        if (jwt == null) {
            final String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                jwt = authHeader.substring(7); // Quitamos la palabra "Bearer "
            }
        }

        // Si después de buscar en ambos lados no hay token, dejamos pasar la petición 
        // (Spring Security la bloqueará más adelante si la ruta era privada)
        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extraer el email del token y validar
        final String userEmail = jwtService.extractUsername(jwt);

        // Si hay email y el usuario no está autenticado en el contexto actual...
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            // Verificamos si el token es válido y no ha expirado
            if (jwtService.isTokenValid(jwt, userDetails)) {
                // Autenticamos al usuario oficialmente
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // Guardamos el inicio de sesión en el contexto de Spring
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        
        // Continuamos con la petición hacia el controlador
        filterChain.doFilter(request, response);
    }
}