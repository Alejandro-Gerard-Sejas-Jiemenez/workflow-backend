package com.sw.api.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PublicacionResponseDTO(
    UUID id,
    UUID idUsuario,
    String correoUsuario,
    InmuebleDTO inmueble,
    String titulo,
    String descripcionGeneral,
    String tipoTransaccion,
    BigDecimal precio,
    String moneda,
    String estadoPublicacion,
    LocalDateTime fechaPublicacion,
    java.util.List<ImagenPublicacionDTO> imagenes
) {}
