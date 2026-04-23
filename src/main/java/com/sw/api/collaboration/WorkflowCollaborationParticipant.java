package com.sw.api.collaboration;

public record WorkflowCollaborationParticipant(
        String sessionId,
        String clientId,
        String userId,
        String name,
        String role,
        String color
) {}
