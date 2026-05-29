package com.sw.api.workflow.services;

import com.sw.api.usuario.models.Usuario;
import com.sw.api.workflow.models.Workflow;
import com.sw.api.workflow.repositories.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class WorkflowSecurityService {

    private final WorkflowRepository workflowRepository;

    public Usuario currentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Usuario usuario) {
            return usuario;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
    }

    public Workflow getAccessibleWorkflow(String id) {
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow no encontrado con ID: " + id));

        if (!canAccess(workflow, currentUser())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a este workflow");
        }

        return workflow;
    }

    public Workflow getEditableWorkflow(String id) {
        Workflow workflow = getAccessibleWorkflow(id);
        if (!canEdit(workflow, currentUser())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes editar este workflow");
        }

        return workflow;
    }

    public Workflow getManageableWorkflow(String id) {
        Workflow workflow = getAccessibleWorkflow(id);
        Usuario user = currentUser();
        if (!isAdmin(user) && workflow.getOwnerUserId() != null && !Objects.equals(workflow.getOwnerUserId(), user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el dueño puede administrar colaboradores");
        }

        if (workflow.getOwnerUserId() == null || workflow.getOwnerUserId().isBlank()) {
            workflow.setOwnerUserId(user.getId());
        }

        return workflow;
    }

    public boolean canAccess(Workflow workflow, Usuario user) {
        return isAdmin(user)
                || "PUBLICADO".equals(workflow.getEstado())
                || Objects.equals(workflow.getOwnerUserId(), user.getId())
                || ensureCollaborators(workflow).stream().anyMatch(collaborator -> Objects.equals(collaborator.getUserId(), user.getId()));
    }

    public boolean canEdit(Workflow workflow, Usuario user) {
        return isAdmin(user)
                || Objects.equals(workflow.getOwnerUserId(), user.getId())
                || ensureCollaborators(workflow).stream()
                        .anyMatch(collaborator -> Objects.equals(collaborator.getUserId(), user.getId()) && !"VIEWER".equals(collaborator.getRole()));
    }

    public boolean isAdmin(Usuario user) {
        return hasRole(user, "ROLE_ADMIN");
    }

    public boolean hasRole(Usuario user, String role) {
        return user.getRol() != null && role.equals(user.getRol().getNombre());
    }

    private List<Workflow.Collaborator> ensureCollaborators(Workflow workflow) {
        if (workflow.getCollaborators() == null) {
            workflow.setCollaborators(new ArrayList<>());
        }
        return workflow.getCollaborators();
    }
}
