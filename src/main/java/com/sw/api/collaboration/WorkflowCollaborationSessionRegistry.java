package com.sw.api.collaboration;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WorkflowCollaborationSessionRegistry {

    private final Map<String, Set<WebSocketSession>> workflowSessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionWorkflows = new ConcurrentHashMap<>();
    private final Map<String, WorkflowCollaborationParticipant> sessionParticipants = new ConcurrentHashMap<>();

    public WorkflowCollaborationParticipant subscribe(
            String workflowId,
            WebSocketSession session,
            WorkflowCollaborationParticipant participant) {
        unsubscribe(session);
        workflowSessions.computeIfAbsent(workflowId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
        sessionWorkflows.put(session.getId(), workflowId);
        sessionParticipants.put(session.getId(), participant);
        return participant;
    }

    public WorkflowCollaborationParticipant unsubscribe(WebSocketSession session) {
        String workflowId = sessionWorkflows.remove(session.getId());
        if (workflowId == null) {
            return sessionParticipants.remove(session.getId());
        }

        Set<WebSocketSession> sessions = workflowSessions.get(workflowId);
        if (sessions == null) {
            return sessionParticipants.remove(session.getId());
        }

        sessions.remove(session);
        if (sessions.isEmpty()) {
            workflowSessions.remove(workflowId);
        }

        return sessionParticipants.remove(session.getId());
    }

    public Set<WebSocketSession> getSubscribers(String workflowId) {
        return workflowSessions.getOrDefault(workflowId, Set.of());
    }

    public String getWorkflowId(WebSocketSession session) {
        return sessionWorkflows.get(session.getId());
    }

    public List<WorkflowCollaborationParticipant> getParticipants(String workflowId) {
        return getSubscribers(workflowId).stream()
                .map(session -> sessionParticipants.get(session.getId()))
                .filter(participant -> participant != null)
                .toList();
    }

    public WorkflowCollaborationParticipant getParticipant(WebSocketSession session) {
        return sessionParticipants.get(session.getId());
    }
}
