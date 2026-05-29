package com.sw.api.workflow.services;

import com.sw.api.bitacora.services.BitacoraService;
import com.sw.api.workflow.models.Workflow;
import com.sw.api.workflow.dtos.WorkflowCreateDTO;
import com.sw.api.workflow.dtos.WorkflowResponseDTO;
import com.sw.api.workflow.dtos.WorkflowCollaboratorAddDTO;
import com.sw.api.workflow.dtos.WorkflowCollaboratorDTO;
import com.sw.api.workflow.dtos.WorkflowPasosUpdateDTO;
import com.sw.api.workflow.dtos.WorkflowReglasUpdateDTO;
import com.sw.api.workflow.dtos.WorkflowDiagramUpdateDTO;
import com.sw.api.workflow.dtos.WorkflowEstadoUpdateDTO;
import com.sw.api.usuario.models.Usuario;
import com.sw.api.usuario.repositories.UsuarioRepository;
import com.sw.api.workflow.repositories.WorkflowRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final UsuarioRepository usuarioRepository;
    private final BitacoraService bitacoraService;
    private final WorkflowSecurityService workflowSecurityService;
    private final WorkflowCollaboratorService workflowCollaboratorService;
    private final WorkflowDiagramSyncService workflowDiagramSyncService;

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
        workflow.setOwnerUserId(workflowSecurityService.currentUser().getId());
        workflow.setCollaborators(new ArrayList<>());

        Workflow saved = workflowRepository.save(workflow);
        bitacoraService.registrarAccion("CREAR_WORKFLOW", "Workflow", "Se creó el workflow " + saved.getNombre());
        return mapToDTO(saved);
    }

    public WorkflowResponseDTO actualizarPasos(String id, WorkflowPasosUpdateDTO dto) {
        Workflow workflow = workflowSecurityService.getEditableWorkflow(id);

        List<Workflow.Paso> nuevosPasos = dto.pasos().stream().map(p -> 
            new Workflow.Paso(p.nombre(), p.orden(), p.departamento(), p.formularioId())
        ).collect(Collectors.toList());
        
        workflow.setPasos(nuevosPasos);
        return mapToDTO(workflowRepository.save(workflow));
    }

    public WorkflowResponseDTO actualizarReglas(String id, WorkflowReglasUpdateDTO dto) {
        Workflow workflow = workflowSecurityService.getEditableWorkflow(id);

        List<Workflow.Regla> nuevasReglas = dto.reglas().stream().map(r -> 
            new Workflow.Regla(r.condicion(), r.accion())
        ).collect(Collectors.toList());
        
        workflow.setReglas(nuevasReglas);
        return mapToDTO(workflowRepository.save(workflow));
    }

    public WorkflowResponseDTO actualizarDiagrama(String id, WorkflowDiagramUpdateDTO dto) {
        Workflow workflow = workflowSecurityService.getEditableWorkflow(id);

        workflow.setDiagramData(dto.diagramData());
        
        // Sincronizar pasos automáticamente desde el JSON del diagrama
        workflowDiagramSyncService.syncPasosDesdeDiagrama(workflow);
        Workflow savedWorkflow = workflowRepository.save(workflow);

        workflowCollaboratorService.broadcastDiagramUpdate(savedWorkflow, dto.sourceClientId());

        bitacoraService.registrarAccion("ACTUALIZAR_DIAGRAMA", "Workflow", "Se actualizó el diagrama del workflow " + savedWorkflow.getNombre());
        return mapToDTO(savedWorkflow);
    }

    public WorkflowResponseDTO actualizarEstado(String id, WorkflowEstadoUpdateDTO dto) {
        Workflow workflow = workflowSecurityService.getManageableWorkflow(id);
        workflow.setEstado(dto.estado());
        Workflow saved = workflowRepository.save(workflow);
        bitacoraService.registrarAccion("ACTUALIZAR_ESTADO", "Workflow", "Se cambió el estado de " + saved.getNombre() + " a " + saved.getEstado());
        return mapToDTO(saved);
    }


    public List<WorkflowResponseDTO> obtenerTodos() {
        Usuario user = workflowSecurityService.currentUser();
        List<Workflow> workflows;
        
        if (workflowSecurityService.isAdmin(user)) {
            workflows = workflowRepository.findAll();
        } else if (workflowSecurityService.hasRole(user, "ROLE_CLIENTE") || workflowSecurityService.hasRole(user, "ROLE_EMPLEADO")) {
            workflows = workflowRepository.findByEstado("PUBLICADO");
        } else {
            workflows = workflowRepository.findByOwnerUserIdOrCollaboratorsUserId(user.getId(), user.getId());
        }

        return workflows.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public WorkflowResponseDTO obtenerPorId(String id) {
        Workflow workflow = workflowSecurityService.getAccessibleWorkflow(id);
        return mapToDTO(workflow);
    }

    public List<WorkflowCollaboratorDTO> obtenerColaboradores(String id) {
        return workflowCollaboratorService.obtenerColaboradores(workflowSecurityService.getAccessibleWorkflow(id));
    }

    public WorkflowCollaboratorDTO agregarColaborador(String id, WorkflowCollaboratorAddDTO dto) {
        Workflow workflow = workflowSecurityService.getManageableWorkflow(id);
        return workflowCollaboratorService.agregarColaborador(workflow, dto, workflowSecurityService);
    }

    public void quitarColaborador(String id, String userId) {
        Workflow workflow = workflowSecurityService.getManageableWorkflow(id);
        workflowCollaboratorService.quitarColaborador(workflow, userId);
    }

    public void eliminarWorkflow(String id) {
        Workflow workflow = workflowSecurityService.getManageableWorkflow(id);
        workflowRepository.delete(workflow);
        bitacoraService.registrarAccion("ELIMINAR_WORKFLOW", "Workflow", "Se eliminó el workflow " + workflow.getNombre());
    }

    public boolean canAccessWorkflow(String workflowId, Usuario user) {
        return workflowRepository.findById(workflowId)
                .map(workflow -> workflowSecurityService.canAccess(workflow, user))
                .orElse(false);
    }

    public boolean canEditWorkflow(String workflowId, Usuario user) {
        return workflowRepository.findById(workflowId)
                .map(workflow -> workflowSecurityService.canEdit(workflow, user))
                .orElse(false);
    }
    
    private WorkflowResponseDTO mapToDTO(Workflow w) {
        return new WorkflowResponseDTO(
            w.getId(),
            w.getNombre(),
            w.getDescripcion(),
            w.getEstado(),
            w.getPasos(),
            w.getReglas(),
            w.getDiagramData(),
            w.getOwnerUserId(),
            workflowCollaboratorService.mapCollaborators(w)
        );
    }
}
