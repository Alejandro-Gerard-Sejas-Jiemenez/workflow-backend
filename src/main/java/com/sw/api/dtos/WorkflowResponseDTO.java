package com.sw.api.dtos;

import java.util.List;
import com.sw.api.models.Workflow;
import jakarta.validation.Valid;

public record WorkflowResponseDTO(
    String id,
    String nombre,
    String descripcion,
    List<Workflow.Paso> pasos,
    @Valid
    List<Workflow.Regla> reglas,
    String diagramData
) {}
