package com.sw.api.usuario.dtos;

import java.util.List;
import java.time.LocalDateTime;

public record UsuarioResponseDTO(
        String id,
        String email,
        String nombre,
        List<String> departamentos,
        String rol,
        boolean estadoConexion,
        LocalDateTime ultimaConexion) {
}
