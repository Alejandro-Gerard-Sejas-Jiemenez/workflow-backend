package com.sw.api.dtos;

import jakarta.validation.constraints.NotBlank;

public record WorkflowDiagramUpdateDTO(
    @NotBlank(message = "El contenido del diagrama no puede estar vacío")
    String diagramData
) {}
