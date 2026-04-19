package com.sw.api.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NotificacionCreateDTO(
    @NotBlank(message = "El usuarioId es obligatorio")
    String usuarioId,

    @NotBlank(message = "El mensaje es obligatorio")
    String mensaje,

    @NotBlank(message = "El tipo es obligatorio")
    String tipo
) {}
