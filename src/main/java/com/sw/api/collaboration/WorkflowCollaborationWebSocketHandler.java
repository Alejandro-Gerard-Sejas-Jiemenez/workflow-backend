package com.sw.api.collaboration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sw.api.models.Usuario;
import com.sw.api.repositories.UsuarioRepository;
import com.sw.api.security.JwtService;
import com.sw.api.services.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class WorkflowCollaborationWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final WorkflowCollaborationSessionRegistry sessionRegistry;
    private final WorkflowCollaborationBroadcaster broadcaster;
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final WorkflowService workflowService;

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Map<String, Object> payload = objectMapper.readValue(
                message.getPayload(),
                Map.class
        );

        String type = payload.get("type") instanceof String value ? value : null;
        String workflowId = payload.get("workflowId") instanceof String value ? value : null;

        if ("subscribe".equals(type) && workflowId != null && !workflowId.isBlank()) {
            String token = payload.get("token") instanceof String value ? value : null;
            String clientId = payload.get("clientId") instanceof String value ? value : session.getId();
            Usuario user = authenticate(token);

            if (!workflowService.canAccessWorkflow(workflowId, user)) {
                session.close(CloseStatus.POLICY_VIOLATION);
                return;
            }

            WorkflowCollaborationParticipant participant = new WorkflowCollaborationParticipant(
                    session.getId(),
                    clientId,
                    user.getId(),
                    user.getNombre(),
                    user.getRol() != null ? user.getRol().getNombre() : "SIN_ROL",
                    colorFor(user.getId())
            );
            sessionRegistry.subscribe(workflowId, session, participant);
            broadcaster.sendToSession(session, Map.of(
                    "type", "presence-snapshot",
                    "workflowId", workflowId,
                    "users", sessionRegistry.getParticipants(workflowId),
                    "updatedAt", System.currentTimeMillis()
            ));
            broadcaster.broadcastEvent(workflowId, Map.of(
                    "type", "user-joined",
                    "workflowId", workflowId,
                    "user", participant,
                    "updatedAt", System.currentTimeMillis()
            ));
            return;
        }

        if (workflowId != null && !workflowId.isBlank()) {
            String currentWorkflowId = sessionRegistry.getWorkflowId(session);
            if (!workflowId.equals(currentWorkflowId)) {
                session.close(CloseStatus.POLICY_VIOLATION);
                return;
            }

            payload.putIfAbsent("updatedAt", System.currentTimeMillis());
            WorkflowCollaborationParticipant participant = sessionRegistry.getParticipant(session);
            if (participant != null) {
                payload.put("user", participant);
            }
            broadcaster.broadcastEvent(workflowId, payload);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String workflowId = sessionRegistry.getWorkflowId(session);
        WorkflowCollaborationParticipant participant = sessionRegistry.unsubscribe(session);
        if (workflowId != null && participant != null) {
            broadcaster.broadcastEvent(workflowId, Map.of(
                    "type", "user-left",
                    "workflowId", workflowId,
                    "user", participant,
                    "updatedAt", System.currentTimeMillis()
            ));
        }
    }

    private Usuario authenticate(String token) throws Exception {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Missing token");
        }

        String email = jwtService.extractUsername(token);
        Usuario user = usuarioRepository.findByEmailAndActivoTrue(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user"));
        if (!jwtService.isTokenValid(token, user)) {
            throw new IllegalArgumentException("Invalid token");
        }

        return user;
    }

    private String colorFor(String userId) {
        String[] colors = {"#7c3aed", "#0891b2", "#dc2626", "#16a34a", "#ea580c", "#2563eb"};
        int index = Math.abs(userId.hashCode()) % colors.length;
        return colors[index];
    }
}
