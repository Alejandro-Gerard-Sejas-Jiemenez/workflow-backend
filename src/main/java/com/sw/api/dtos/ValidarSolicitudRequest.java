package com.sw.api.dtos;

import jakarta.validation.constraints.NotNull;

public record ValidarSolicitudRequest(
    @NotNull(message = "El resultado de la aprobación es obligatorio")
    boolean aprobado,
    String observaciones
) {}
