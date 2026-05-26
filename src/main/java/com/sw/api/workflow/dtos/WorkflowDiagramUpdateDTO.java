package com.sw.api.workflow.dtos;

import jakarta.validation.constraints.NotBlank;

public record WorkflowDiagramUpdateDTO(
        @NotBlank(message = "El contenido del diagrama no puede estar vacio")
        String diagramData,
        String sourceClientId
) {}
