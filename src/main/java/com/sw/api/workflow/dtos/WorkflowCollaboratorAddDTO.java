package com.sw.api.workflow.dtos;

import jakarta.validation.constraints.NotBlank;

public record WorkflowCollaboratorAddDTO(
        @NotBlank(message = "El userId es obligatorio")
        String userId,
        String role
) {}
