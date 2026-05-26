package com.sw.api.workflow.dtos;

import jakarta.validation.constraints.NotBlank;

public record WorkflowEstadoUpdateDTO(
    @NotBlank(message = "El estado no puede estar vacío")
    String estado
) {}
