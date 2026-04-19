package com.sw.api.dtos;

import java.time.LocalDateTime;
import java.util.List;

public record ComentarioResponseDTO(
    String id,
    String usuarioId,
    String contenido,
    List<String> menciones,
    String archivoUrl,
    LocalDateTime fecha
) {}
