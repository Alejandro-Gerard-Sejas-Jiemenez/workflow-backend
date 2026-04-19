package com.sw.api.dtos;

import java.time.LocalDateTime;

public record NotificacionResponseDTO(
    String id,
    String usuarioId,
    String mensaje,
    String tipo,
    boolean leido,
    LocalDateTime fecha
) {}
