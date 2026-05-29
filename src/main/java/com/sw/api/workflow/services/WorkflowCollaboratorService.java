package com.sw.api.workflow.services;

import com.sw.api.usuario.models.Usuario;
import com.sw.api.usuario.repositories.UsuarioRepository;
import com.sw.api.workflow.dtos.WorkflowCollaboratorAddDTO;
import com.sw.api.workflow.dtos.WorkflowCollaboratorDTO;
import com.sw.api.workflow.models.Workflow;
import com.sw.api.workflow.repositories.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class WorkflowCollaboratorService {

    @Value("${workflow.colab.base-url}")
    private String colabBaseUrl;

    private final UsuarioRepository usuarioRepository;
    private final WorkflowRepository workflowRepository;

    public List<WorkflowCollaboratorDTO> obtenerColaboradores(Workflow workflow) {
        return mapCollaborators(workflow);
    }

    public WorkflowCollaboratorDTO agregarColaborador(Workflow workflow, WorkflowCollaboratorAddDTO dto, WorkflowSecurityService securityService) {
        Usuario invited = usuarioRepository.findById(dto.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (!securityService.hasRole(invited, "ROLE_DESIGNER")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se pueden invitar usuarios DESIGNER");
        }

        if (Objects.equals(workflow.getOwnerUserId(), invited.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El dueño ya tiene acceso al workflow");
        }

        List<Workflow.Collaborator> collaborators = ensureCollaborators(workflow);
        String role = dto.role() == null || dto.role().isBlank() ? "EDITOR" : dto.role();
        collaborators.removeIf(collaborator -> Objects.equals(collaborator.getUserId(), invited.getId()));
        collaborators.add(new Workflow.Collaborator(invited.getId(), role));
        workflowRepository.save(workflow);

        return new WorkflowCollaboratorDTO(invited.getId(), invited.getNombre(), invited.getEmail(), role);
    }

    public void quitarColaborador(Workflow workflow, String userId) {
        ensureCollaborators(workflow).removeIf(collaborator -> Objects.equals(collaborator.getUserId(), userId));
        workflowRepository.save(workflow);
    }

    public void broadcastDiagramUpdate(Workflow workflow, String sourceClientId) {
        try {
            org.springframework.web.client.RestClient.create().post()
                    .uri(colabBaseUrl + "/api/internal/collaboration/broadcast-diagram")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "workflowId", workflow.getId(),
                            "diagramData", workflow.getDiagramData() != null ? workflow.getDiagramData() : "",
                            "sourceClientId", sourceClientId != null ? sourceClientId : ""
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            System.err.println("No se pudo notificar al servidor de colaboración: " + e.getMessage());
        }
    }

    public List<WorkflowCollaboratorDTO> mapCollaborators(Workflow workflow) {
        return ensureCollaborators(workflow).stream()
                .map(collaborator -> usuarioRepository.findById(collaborator.getUserId())
                        .map(usuario -> new WorkflowCollaboratorDTO(
                                usuario.getId(),
                                usuario.getNombre(),
                                usuario.getEmail(),
                                collaborator.getRole()))
                        .orElse(new WorkflowCollaboratorDTO(
                                collaborator.getUserId(),
                                "Usuario no encontrado",
                                "",
                                collaborator.getRole())))
                .toList();
    }

    public List<Workflow.Collaborator> ensureCollaborators(Workflow workflow) {
        if (workflow.getCollaborators() == null) {
            workflow.setCollaborators(new ArrayList<>());
        }
        return workflow.getCollaborators();
    }
}
