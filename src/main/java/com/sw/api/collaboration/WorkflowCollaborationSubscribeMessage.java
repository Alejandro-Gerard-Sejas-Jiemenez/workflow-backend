package com.sw.api.collaboration;

public record WorkflowCollaborationSubscribeMessage(
        String type,
        String workflowId
) {}
