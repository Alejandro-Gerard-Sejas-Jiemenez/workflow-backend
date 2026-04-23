package com.sw.api.dtos;

public record WorkflowCollaboratorDTO(
        String userId,
        String nombre,
        String email,
        String role
) {}
