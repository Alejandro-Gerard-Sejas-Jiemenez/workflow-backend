package com.sw.api.services;

import com.sw.api.dtos.AuthResponse;
import com.sw.api.dtos.LoginRequest;
import com.sw.api.dtos.RegisterRequest;
import com.sw.api.dtos.RefreshTokenRequest;
import com.sw.api.models.Usuario;
import com.sw.api.models.Perfil;
import com.sw.api.models.TipoPerfil;
import com.sw.api.models.Rol;
import com.sw.api.repositories.UsuarioRepository;
import com.sw.api.repositories.PerfilRepository;
import com.sw.api.repositories.TipoPerfilRepository;
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
    private final PerfilRepository perfilRepository;
    private final TipoPerfilRepository tipoPerfilRepository;
    private final RolRepository rolRepository; // Inyectamos el nuevo repositorio

    @Transactional
    public AuthResponse registrar(RegisterRequest request) {
        // 1. Validar si el correo existe (ahora responde con un error 400 limpio)
        if (usuarioRepository.findByCorreo(request.correo()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El correo ya está registrado");
        }

        // 2. Buscar catálogos (Tipo de Perfil y Rol)
        TipoPerfil tipoSeleccionado = tipoPerfilRepository.findByNombre(request.tipoPerfil())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de perfil inválido"));

        Rol rolPorDefecto = rolRepository.findByNombre("ROLE_USER")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Rol base no encontrado en la DB"));

        // 3. Crear y guardar Usuario
        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setCorreo(request.correo());
        nuevoUsuario.setPassword(passwordEncoder.encode(request.password()));
        nuevoUsuario.setEstadoConexion(true);
        nuevoUsuario.setUltimaConexion(LocalDateTime.now());
        nuevoUsuario.setRol(rolPorDefecto); // ¡Asignamos el rol aquí!

        Usuario usuarioGuardado = usuarioRepository.save(nuevoUsuario);

        // 4. Crear y guardar Perfil
        Perfil nuevoPerfil = new Perfil();
        nuevoPerfil.setNombre(request.nombre());
        nuevoPerfil.setApellido(request.apellido());
        nuevoPerfil.setFotoUrl(request.fotoUrl());
        nuevoPerfil.setUsuario(usuarioGuardado);
        nuevoPerfil.setTipoPerfil(tipoSeleccionado);

        perfilRepository.save(nuevoPerfil);

        // 5. Armar los Custom Claims para el JWT
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("id", usuarioGuardado.getId());
        extraClaims.put("nombre", nuevoPerfil.getNombre());
        extraClaims.put("apellido", nuevoPerfil.getApellido());
        extraClaims.put("rol", usuarioGuardado.getRol().getNombre());

        // 6. Generar token
        var jwtToken = jwtService.generarToken(extraClaims, usuarioGuardado);
        return new AuthResponse(jwtToken);
    }

    public AuthResponse login(LoginRequest request) {
        // 1. Verificar credenciales
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.correo(), request.password()));

        // 2. Buscar Usuario
        var user = usuarioRepository.findByCorreo(request.correo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        // 3. Buscar Perfil
        var perfil = perfilRepository.findByUsuario(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil no encontrado"));

        // 4. Armar los Custom Claims para el JWT
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("id", user.getId());
        extraClaims.put("nombre", perfil.getNombre());
        extraClaims.put("apellido", perfil.getApellido());
        String nombreRol = (user.getRol() != null) ? user.getRol().getNombre() : "SIN_ROL";
        extraClaims.put("rol", nombreRol);
        // 5. Generar token
        var jwtToken = jwtService.generarToken(extraClaims, user);
        return new AuthResponse(jwtToken);
    }

    public AuthResponse actualizarToken(RefreshTokenRequest request) {
        String tokenViejo = request.token();
        String correo = null;

        try {
            // Intentamos extraer el correo normalmente (por si el token aún no expira pero
            // quieren uno nuevo)
            correo = jwtService.extractUsername(tokenViejo);

        } catch (ExpiredJwtException e) {
            // Si el token ya expiró, extraemos el correo directamente desde la excepción
            correo = e.getClaims().getSubject();
        } catch (Exception e) {
            // Si es un token malformado o inventado, lo rebotamos
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token inválido o corrupto");
        }

        if (correo == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo extraer información del token");
        }

        // 1. Buscamos al usuario con ese correo
        var user = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Usuario no encontrado en la base de datos"));

        // 2. Buscamos su perfil actualizado
        var perfil = perfilRepository.findByUsuario(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil no encontrado"));

        // 3. Armamos los datos (claims) frescos por si el administrador le cambió el
        // rol o el nombre
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("id", user.getId());
        extraClaims.put("nombre", perfil.getNombre());
        extraClaims.put("apellido", perfil.getApellido());
        String nombreRol = (user.getRol() != null) ? user.getRol().getNombre() : "SIN_ROL";
        extraClaims.put("rol", nombreRol);

        // 4. Generamos y devolvemos el nuevo token de acceso
        var nuevoToken = jwtService.generarToken(extraClaims, user);
        return new AuthResponse(nuevoToken);
    }
}