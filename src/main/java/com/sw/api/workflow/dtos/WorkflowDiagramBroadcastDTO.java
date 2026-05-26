package com.sw.api.workflow.dtos;

public record WorkflowDiagramBroadcastDTO(
        String type,
        String workflowId,
        String diagramData,
        String sourceClientId,
        long updatedAt
) {}
