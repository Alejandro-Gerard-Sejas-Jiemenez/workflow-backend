package com.sw.api.auth.controllers;

import com.sw.api.auth.dtos.AuthResponse;
import com.sw.api.auth.dtos.LoginRequest;
import com.sw.api.auth.dtos.RegisterRequest;
import com.sw.api.auth.dtos.RefreshTokenRequest;
import com.sw.api.auth.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Cookie;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/registro")
    public ResponseEntity<AuthResponse> registrar(@RequestBody RegisterRequest request) {
        AuthResponse response = authService.registrar(request);
        return ResponseEntity.ok()
                .headers(crearCookieHibrida(response.token()))
                .body(response); // Enviamos en body para el celular y en Cookie para la web
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        logger.info("🔐 Login request received for email: {}", request.getEmail());
        AuthResponse response = authService.login(request);
        logger.info("🔑 Token generated: {}...", response.token().substring(0, Math.min(10, response.token().length())));
        return ResponseEntity.ok()
                .headers(crearCookieHibrida(response.token()))
                .body(response);
    }

    private HttpHeaders crearCookieHibrida(String token) {
        ResponseCookie cookie = ResponseCookie.from("jwt_token", token)
                .httpOnly(true)       // Evita ataques XSS desde Javascript
                .secure(false)        // Ponlo en true cuando subas a producción con HTTPS
                .path("/")            // Disponible para todas las rutas de la API
                .maxAge(24 * 60 * 60) // Expira en 1 día (en segundos)
                .sameSite("None")   // Permite envío de cookie en peticiones cross‑origin (dev)
                .build();
                
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
        return headers;
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.actualizarToken(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String jwt = null;
        
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("jwt_token".equals(cookie.getName())) {
                    jwt = cookie.getValue();
                    break;
                }
            }
        }
        
        if (jwt == null) {
            final String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                jwt = authHeader.substring(7);
            }
        }

        if (jwt != null) {
            authService.processLogout(jwt);
        }

        return ResponseEntity.ok()
                .headers(limpiarCookie())
                .build();
    }

    private HttpHeaders limpiarCookie() {
        ResponseCookie cookie = ResponseCookie.from("jwt_token", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .build();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
        return headers;
    }
}
