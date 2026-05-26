package com.sw.api.workflow.dtos;

public record WorkflowCollaboratorDTO(
        String userId,
        String nombre,
        String email,
        String role
) {}
