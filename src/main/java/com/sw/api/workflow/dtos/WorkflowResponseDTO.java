package com.sw.api.workflow.dtos;

import java.util.List;
import com.sw.api.workflow.models.Workflow;
import jakarta.validation.Valid;

public record WorkflowResponseDTO(
    String id,
    String nombre,
    String descripcion,
    String estado,
    List<Workflow.Paso> pasos,
    @Valid
    List<Workflow.Regla> reglas,
    String diagramData,
    String ownerUserId,
    List<WorkflowCollaboratorDTO> collaborators
) {}
