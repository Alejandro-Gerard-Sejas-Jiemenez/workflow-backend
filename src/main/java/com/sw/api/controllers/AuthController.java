package com.sw.api.controllers;

import com.sw.api.dtos.AuthResponse;
import com.sw.api.dtos.LoginRequest;
import com.sw.api.dtos.RegisterRequest;
import com.sw.api.dtos.RefreshTokenRequest;
import com.sw.api.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        AuthResponse response = authService.login(request);
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
                .sameSite("Strict")   // Evita ataques CSRF
                .build();
                
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
        return headers;
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.actualizarToken(request));
    }
}