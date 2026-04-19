package com.sw.api.services;

import com.sw.api.dtos.AuthResponse;
import com.sw.api.dtos.LoginRequest;
import com.sw.api.dtos.RegisterRequest;
import com.sw.api.dtos.RefreshTokenRequest;
import com.sw.api.models.Usuario;
import com.sw.api.models.Rol;
import com.sw.api.repositories.UsuarioRepository;
import com.sw.api.repositories.RolRepository;
import com.sw.api.security.JwtService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import io.jsonwebtoken.ExpiredJwtException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RolRepository rolRepository;

    @Transactional
    public AuthResponse registrar(RegisterRequest request) {
        if (usuarioRepository.findByEmail(request.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El email ya está registrado");
        }

        Rol rolPorDefecto = rolRepository.findByNombre("ROLE_USER")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Rol base no encontrado en la DB"));

        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setEmail(request.email());
        nuevoUsuario.setNombre(request.nombre());
        nuevoUsuario.setApellido(request.apellido());
        nuevoUsuario.setDepartamento(request.departamento());
        nuevoUsuario.setTelefono(request.telefono());
        nuevoUsuario.setPassword(passwordEncoder.encode(request.password()));
        nuevoUsuario.setEstadoConexion(true);
        nuevoUsuario.setUltimaConexion(LocalDateTime.now());
        nuevoUsuario.setRol(rolPorDefecto);

        Usuario usuarioGuardado = usuarioRepository.save(nuevoUsuario);

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("id", usuarioGuardado.getId());
        extraClaims.put("nombre", usuarioGuardado.getNombre());
        extraClaims.put("apellido", usuarioGuardado.getApellido());
        extraClaims.put("rol", usuarioGuardado.getRol().getNombre());

        var jwtToken = jwtService.generarToken(extraClaims, usuarioGuardado);
        return new AuthResponse(jwtToken);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        var user = usuarioRepository.findByEmailAndActivoTrue(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado o inactivo"));

        // Actualizar estado de conexión
        user.setEstadoConexion(true);
        user.setUltimaConexion(LocalDateTime.now());
        usuarioRepository.save(user);

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("id", user.getId());
        extraClaims.put("nombre", user.getNombre());
        extraClaims.put("apellido", user.getApellido());
        String nombreRol = (user.getRol() != null) ? user.getRol().getNombre() : "SIN_ROL";
        extraClaims.put("rol", nombreRol);
        
        var jwtToken = jwtService.generarToken(extraClaims, user);
        return new AuthResponse(jwtToken);
    }

    public void processLogout(String token) {
        String email = jwtService.extractUsername(token);
        var user = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        
        user.setEstadoConexion(false);
        usuarioRepository.save(user);

        jwtService.blacklistToken(token);
    }

    public AuthResponse actualizarToken(RefreshTokenRequest request) {
        String tokenViejo = request.token();
        String email = null;

        try {
            email = jwtService.extractUsername(tokenViejo);
        } catch (ExpiredJwtException e) {
            email = e.getClaims().getSubject();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token inválido o corrupto");
        }

        if (email == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo extraer información del token");
        }

        var user = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Usuario no encontrado en la base de datos"));

        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("id", user.getId());
        extraClaims.put("nombre", user.getNombre());
        extraClaims.put("apellido", user.getApellido());
        String nombreRol = (user.getRol() != null) ? user.getRol().getNombre() : "SIN_ROL";
        extraClaims.put("rol", nombreRol);

        var nuevoToken = jwtService.generarToken(extraClaims, user);
        return new AuthResponse(nuevoToken);
    }
}