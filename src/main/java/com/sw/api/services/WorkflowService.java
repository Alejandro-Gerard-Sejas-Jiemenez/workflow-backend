package com.sw.api.services;

import com.sw.api.collaboration.WorkflowCollaborationBroadcaster;
import com.sw.api.models.Workflow;
import com.sw.api.dtos.WorkflowCreateDTO;
import com.sw.api.dtos.WorkflowResponseDTO;
import com.sw.api.dtos.WorkflowCollaboratorAddDTO;
import com.sw.api.dtos.WorkflowCollaboratorDTO;
import com.sw.api.dtos.WorkflowPasosUpdateDTO;
import com.sw.api.dtos.WorkflowReglasUpdateDTO;
import com.sw.api.dtos.WorkflowDiagramUpdateDTO;
import com.sw.api.models.Usuario;
import com.sw.api.repositories.UsuarioRepository;
import com.sw.api.repositories.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final UsuarioRepository usuarioRepository;
    private final WorkflowCollaborationBroadcaster workflowCollaborationBroadcaster;

    public WorkflowResponseDTO crear(WorkflowCreateDTO dto) {
        Workflow workflow = new Workflow();
        workflow.setNombre(dto.nombre());
        workflow.setDescripcion(dto.descripcion());
        
        List<Workflow.Paso> pasos = dto.pasos().stream().map(p -> 
            new Workflow.Paso(p.nombre(), p.orden(), p.departamento(), p.formularioId())
        ).collect(Collectors.toList());
        workflow.setPasos(pasos);

        List<Workflow.Regla> reglas = dto.reglas().stream().map(r -> 
            new Workflow.Regla(r.condicion(), r.accion())
        ).collect(Collectors.toList());
        workflow.setReglas(reglas);
        workflow.setDiagramData(dto.diagramData());
        workflow.setOwnerUserId(currentUser().getId());
        workflow.setCollaborators(new ArrayList<>());

        return mapToDTO(workflowRepository.save(workflow));
    }

    public WorkflowResponseDTO actualizarPasos(String id, WorkflowPasosUpdateDTO dto) {
        Workflow workflow = getEditableWorkflow(id);

        List<Workflow.Paso> nuevosPasos = dto.pasos().stream().map(p -> 
            new Workflow.Paso(p.nombre(), p.orden(), p.departamento(), p.formularioId())
        ).collect(Collectors.toList());
        
        workflow.setPasos(nuevosPasos);
        return mapToDTO(workflowRepository.save(workflow));
    }

    public WorkflowResponseDTO actualizarReglas(String id, WorkflowReglasUpdateDTO dto) {
        Workflow workflow = getEditableWorkflow(id);

        List<Workflow.Regla> nuevasReglas = dto.reglas().stream().map(r -> 
            new Workflow.Regla(r.condicion(), r.accion())
        ).collect(Collectors.toList());
        
        workflow.setReglas(nuevasReglas);
        return mapToDTO(workflowRepository.save(workflow));
    }

    public WorkflowResponseDTO actualizarDiagrama(String id, WorkflowDiagramUpdateDTO dto) {
        Workflow workflow = getEditableWorkflow(id);

        workflow.setDiagramData(dto.diagramData());
        Workflow savedWorkflow = workflowRepository.save(workflow);
        workflowCollaborationBroadcaster.broadcastDiagramUpdated(
            savedWorkflow.getId(),
            savedWorkflow.getDiagramData(),
            dto.sourceClientId()
        );
        return mapToDTO(savedWorkflow);
    }

    public List<WorkflowResponseDTO> obtenerTodos() {
        Usuario user = currentUser();
        List<Workflow> workflows = isAdmin(user)
                ? workflowRepository.findAll()
                : workflowRepository.findByOwnerUserIdOrCollaboratorsUserId(user.getId(), user.getId());

        return workflows.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public WorkflowResponseDTO obtenerPorId(String id) {
        Workflow workflow = getAccessibleWorkflow(id);
        return mapToDTO(workflow);
    }

    public List<WorkflowCollaboratorDTO> obtenerColaboradores(String id) {
        return mapCollaborators(getAccessibleWorkflow(id));
    }

    public WorkflowCollaboratorDTO agregarColaborador(String id, WorkflowCollaboratorAddDTO dto) {
        Workflow workflow = getManageableWorkflow(id);
        Usuario invited = usuarioRepository.findById(dto.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (!hasRole(invited, "ROLE_DESIGNER")) {
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

    public void quitarColaborador(String id, String userId) {
        Workflow workflow = getManageableWorkflow(id);
        ensureCollaborators(workflow).removeIf(collaborator -> Objects.equals(collaborator.getUserId(), userId));
        workflowRepository.save(workflow);
    }

    public void eliminarWorkflow(String id) {
        Workflow workflow = getManageableWorkflow(id);
        workflowRepository.delete(workflow);
    }

    public boolean canAccessWorkflow(String workflowId, Usuario user) {
        return workflowRepository.findById(workflowId)
                .map(workflow -> canAccess(workflow, user))
                .orElse(false);
    }

    public boolean canEditWorkflow(String workflowId, Usuario user) {
        return workflowRepository.findById(workflowId)
                .map(workflow -> canEdit(workflow, user))
                .orElse(false);
    }
    
    private WorkflowResponseDTO mapToDTO(Workflow w) {
        return new WorkflowResponseDTO(
            w.getId(),
            w.getNombre(),
            w.getDescripcion(),
            w.getPasos(),
            w.getReglas(),
            w.getDiagramData(),
            w.getOwnerUserId(),
            mapCollaborators(w)
        );
    }

    private Workflow getAccessibleWorkflow(String id) {
        Workflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow no encontrado con ID: " + id));

        if (!canAccess(workflow, currentUser())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a este workflow");
        }

        return workflow;
    }

    private Workflow getEditableWorkflow(String id) {
        Workflow workflow = getAccessibleWorkflow(id);
        if (!canEdit(workflow, currentUser())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes editar este workflow");
        }

        return workflow;
    }

    private Workflow getManageableWorkflow(String id) {
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

    private Usuario currentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Usuario usuario) {
            return usuario;
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
    }

    private boolean canAccess(Workflow workflow, Usuario user) {
        return isAdmin(user)
                || Objects.equals(workflow.getOwnerUserId(), user.getId())
                || ensureCollaborators(workflow).stream().anyMatch(collaborator -> Objects.equals(collaborator.getUserId(), user.getId()));
    }

    private boolean canEdit(Workflow workflow, Usuario user) {
        return isAdmin(user)
                || Objects.equals(workflow.getOwnerUserId(), user.getId())
                || ensureCollaborators(workflow).stream()
                        .anyMatch(collaborator -> Objects.equals(collaborator.getUserId(), user.getId()) && !"VIEWER".equals(collaborator.getRole()));
    }

    private boolean isAdmin(Usuario user) {
        return hasRole(user, "ROLE_ADMIN");
    }

    private boolean hasRole(Usuario user, String role) {
        return user.getRol() != null && role.equals(user.getRol().getNombre());
    }

    private List<Workflow.Collaborator> ensureCollaborators(Workflow workflow) {
        if (workflow.getCollaborators() == null) {
            workflow.setCollaborators(new ArrayList<>());
        }

        return workflow.getCollaborators();
    }

    private List<WorkflowCollaboratorDTO> mapCollaborators(Workflow workflow) {
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
}
