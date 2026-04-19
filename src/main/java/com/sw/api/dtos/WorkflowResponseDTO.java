package com.sw.api.dtos;

import java.util.List;
import com.sw.api.models.Workflow;

public record WorkflowResponseDTO(
    String id,
    String nombre,
    String descripcion,
    List<Workflow.Paso> pasos,
    List<Workflow.Regla> reglas
) {}
