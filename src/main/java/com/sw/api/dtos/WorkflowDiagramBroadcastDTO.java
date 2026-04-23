package com.sw.api.dtos;

public record WorkflowDiagramBroadcastDTO(
        String type,
        String workflowId,
        String diagramData,
        String sourceClientId,
        long updatedAt
) {}
