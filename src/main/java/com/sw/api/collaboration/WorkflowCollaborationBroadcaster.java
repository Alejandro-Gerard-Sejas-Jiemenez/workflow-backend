package com.sw.api.collaboration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sw.api.dtos.WorkflowDiagramBroadcastDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class WorkflowCollaborationBroadcaster {

    private final ObjectMapper objectMapper;
    private final WorkflowCollaborationSessionRegistry sessionRegistry;

    public void broadcastDiagramUpdated(String workflowId, String diagramData, String sourceClientId) {
        WorkflowDiagramBroadcastDTO payload = new WorkflowDiagramBroadcastDTO(
                "diagram-updated",
                workflowId,
                diagramData,
                sourceClientId,
                System.currentTimeMillis()
        );

        sessionRegistry.getSubscribers(workflowId).forEach(session -> sendSafely(session, payload));
    }

    public void broadcastEvent(String workflowId, Map<String, Object> payload) {
        sessionRegistry.getSubscribers(workflowId).forEach(session -> sendSafely(session, payload));
    }

    public void sendToSession(WebSocketSession session, Object payload) {
        sendSafely(session, payload);
    }

    private void sendSafely(WebSocketSession session, Object payload) {
        if (!session.isOpen()) {
            sessionRegistry.unsubscribe(session);
            return;
        }

        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
            }
        } catch (Exception exception) {
            sessionRegistry.unsubscribe(session);
        }
    }
}
