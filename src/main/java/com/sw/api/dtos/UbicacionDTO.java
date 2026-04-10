package com.sw.api.dtos;

import java.util.UUID;

public record UbicacionDTO(
    UUID id,
    String ciudad,
    String zonaBarrios,
    String direccionExacta,
    String latitud,
    String longitud
) {}
