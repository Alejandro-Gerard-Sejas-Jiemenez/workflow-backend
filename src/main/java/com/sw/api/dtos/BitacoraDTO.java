package com.sw.api.dtos;

import java.time.LocalDateTime;

public record BitacoraDTO(
    String id,
    String usuarioId,
    String usuarioNombre,
    String accion,
    String entidad,
    LocalDateTime fecha,
    String detalles
) {}
