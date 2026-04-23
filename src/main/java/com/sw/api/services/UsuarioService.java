package com.sw.api.services;

import com.sw.api.dtos.UsuarioCreateDTO;
import com.sw.api.dtos.UsuarioResponseDTO;
import com.sw.api.dtos.UsuarioUpdateDTO;
import com.sw.api.models.Usuario;
import com.sw.api.models.Rol;
import com.sw.api.repositories.UsuarioRepository;
import com.sw.api.repositories.RolRepository;
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
        usuario.setDepartamento(dto.departamento());
        usuario.setRol(rol);
        usuario.setActivo(true);

        return mapToDTO(usuarioRepository.save(usuario));
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
        if (dto.departamento() != null)
            usuario.setDepartamento(dto.departamento());

        if (dto.rolId() != null) {
            Rol rol = rolRepository.findById(dto.rolId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado"));
            usuario.setRol(rol);
        }

        return mapToDTO(usuarioRepository.save(usuario));
    }

    public void eliminar(String id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        usuario.setActivo(false);
        usuarioRepository.save(usuario);
    }

    public void restaurar(String id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        usuario.setActivo(true);
        usuarioRepository.save(usuario);
    }

    private UsuarioResponseDTO mapToDTO(Usuario usuario) {
        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getNombre(),
                usuario.getDepartamento(),
                (usuario.getRol() != null) ? usuario.getRol().getNombre() : "SIN_ROL",
                usuario.isEstadoConexion(),
                usuario.getUltimaConexion());
    }
}
