package com.sw.api.dtos;

import java.util.UUID;

public record ImagenPublicacionDTO(
    UUID id,
    String urlImage,
    Boolean esPortada
) {}
