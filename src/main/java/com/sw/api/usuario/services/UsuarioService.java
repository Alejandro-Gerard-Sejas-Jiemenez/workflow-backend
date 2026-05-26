package com.sw.api.usuario.services;

import com.sw.api.bitacora.services.BitacoraService;

import com.sw.api.usuario.dtos.UsuarioCreateDTO;
import com.sw.api.usuario.dtos.UsuarioResponseDTO;
import com.sw.api.usuario.dtos.UsuarioUpdateDTO;
import com.sw.api.usuario.models.Usuario;
import com.sw.api.usuario.models.Rol;
import com.sw.api.usuario.repositories.UsuarioRepository;
import com.sw.api.usuario.repositories.RolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final BitacoraService bitacoraService;

    public UsuarioResponseDTO crear(UsuarioCreateDTO dto) {
        if (usuarioRepository.findByEmail(dto.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El email ya está registrado");
        }

        Rol rol = rolRepository.findById(dto.rolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado"));

        Usuario usuario = new Usuario();
        usuario.setNombre(dto.nombre());
        usuario.setEmail(dto.email());
        usuario.setPassword(passwordEncoder.encode(dto.password()));
        usuario.setDepartamentos(dto.departamentos());
        usuario.setRol(rol);
        usuario.setActivo(true);

        Usuario saved = usuarioRepository.save(usuario);
        bitacoraService.registrarAccion("CREAR_USUARIO", "Usuario", "Se creó el usuario " + saved.getEmail() + " con rol " + rol.getNombre());
        return mapToDTO(saved);
    }

    public List<UsuarioResponseDTO> obtenerTodos() {
        return usuarioRepository.findAllByActivoTrue().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<UsuarioResponseDTO> obtenerInactivos() {
        return usuarioRepository.findAllByActivoFalse().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<UsuarioResponseDTO> obtenerDesigners() {
        return usuarioRepository.findAllByActivoTrue().stream()
                .filter(usuario -> usuario.getRol() != null && "ROLE_DESIGNER".equals(usuario.getRol().getNombre()))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public UsuarioResponseDTO obtenerPorId(String id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        return mapToDTO(usuario);
    }

    public UsuarioResponseDTO actualizar(String id, UsuarioUpdateDTO dto) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (dto.nombre() != null)
            usuario.setNombre(dto.nombre());
        if (dto.departamentos() != null)
            usuario.setDepartamentos(dto.departamentos());

        if (dto.rolId() != null) {
            Rol rol = rolRepository.findById(dto.rolId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado"));
            usuario.setRol(rol);
        }

        Usuario saved = usuarioRepository.save(usuario);
        bitacoraService.registrarAccion("ACTUALIZAR_USUARIO", "Usuario", "Se actualizó el usuario " + saved.getEmail());
        return mapToDTO(saved);
    }

    public void eliminar(String id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        usuario.setActivo(false);
        usuarioRepository.save(usuario);
        bitacoraService.registrarAccion("DESACTIVAR_USUARIO", "Usuario", "Se desactivó el usuario " + usuario.getEmail());
    }

    public void restaurar(String id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        usuario.setActivo(true);
        usuarioRepository.save(usuario);
        bitacoraService.registrarAccion("RESTAURAR_USUARIO", "Usuario", "Se restauró el usuario " + usuario.getEmail());
    }

    public void actualizarFcmToken(String email, String token) {
        usuarioRepository.findByEmail(email).ifPresent(u -> {
            u.setFcmToken(token);
            usuarioRepository.save(u);
        });
    }

    private UsuarioResponseDTO mapToDTO(Usuario usuario) {
        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getNombre(),
                usuario.getDepartamentos(),
                (usuario.getRol() != null) ? usuario.getRol().getNombre() : "SIN_ROL",
                usuario.isEstadoConexion(),
                usuario.getUltimaConexion());
    }
}
