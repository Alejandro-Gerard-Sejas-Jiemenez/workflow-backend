package com.sw.api.dtos;

import java.time.LocalDateTime;

public record UsuarioResponseDTO(
    String id,
    String email,
    String nombre,
    String apellido,
    String departamento,
    String telefono,
    String rol,
    boolean estadoConexion,
    LocalDateTime ultimaConexion
) {}
