package com.sw.api.dtos;

import jakarta.validation.constraints.NotBlank;

public record WorkflowEstadoUpdateDTO(
    @NotBlank(message = "El estado no puede estar vacío")
    String estado
) {}
