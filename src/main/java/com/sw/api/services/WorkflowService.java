package com.sw.api.services;

import com.sw.api.models.Workflow;
import com.sw.api.dtos.WorkflowCreateDTO;
import com.sw.api.dtos.WorkflowResponseDTO;
import com.sw.api.dtos.WorkflowCollaboratorAddDTO;
import com.sw.api.dtos.WorkflowCollaboratorDTO;
import com.sw.api.dtos.WorkflowPasosUpdateDTO;
import com.sw.api.dtos.WorkflowReglasUpdateDTO;
import com.sw.api.dtos.WorkflowDiagramUpdateDTO;
import com.sw.api.dtos.WorkflowEstadoUpdateDTO;
import com.sw.api.models.Usuario;
import com.sw.api.repositories.UsuarioRepository;
import com.sw.api.repositories.WorkflowRepository;
import com.sw.api.models.Formulario;
import com.sw.api.repositories.FormularioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final UsuarioRepository usuarioRepository;
    private final FormularioRepository formularioRepository;
    private final BitacoraService bitacoraService;
    private final ObjectMapper objectMapper;

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

        Workflow saved = workflowRepository.save(workflow);
        bitacoraService.registrarAccion("CREAR_WORKFLOW", "Workflow", "Se creó el workflow " + saved.getNombre());
        return mapToDTO(saved);
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
        
        // Sincronizar pasos automáticamente desde el JSON del diagrama
        syncPasosDesdeDiagrama(workflow);
        Workflow savedWorkflow = workflowRepository.save(workflow);

        try {
            org.springframework.web.client.RestClient.create().post()
                .uri("http://localhost:8082/api/internal/collaboration/broadcast-diagram")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(Map.of(
                    "workflowId", savedWorkflow.getId(),
                    "diagramData", savedWorkflow.getDiagramData() != null ? savedWorkflow.getDiagramData() : "",
                    "sourceClientId", dto.sourceClientId() != null ? dto.sourceClientId() : ""
                ))
                .retrieve()
                .toBodilessEntity();
        } catch (Exception e) {
            System.err.println("No se pudo notificar al servidor de colaboración: " + e.getMessage());
        }

        bitacoraService.registrarAccion("ACTUALIZAR_DIAGRAMA", "Workflow", "Se actualizó el diagrama del workflow " + savedWorkflow.getNombre());
        return mapToDTO(savedWorkflow);
    }

    public WorkflowResponseDTO actualizarEstado(String id, WorkflowEstadoUpdateDTO dto) {
        Workflow workflow = getManageableWorkflow(id);
        workflow.setEstado(dto.estado());
        Workflow saved = workflowRepository.save(workflow);
        bitacoraService.registrarAccion("ACTUALIZAR_ESTADO", "Workflow", "Se cambió el estado de " + saved.getNombre() + " a " + saved.getEstado());
        return mapToDTO(saved);
    }


    public List<WorkflowResponseDTO> obtenerTodos() {
        Usuario user = currentUser();
        List<Workflow> workflows;
        
        if (isAdmin(user)) {
            workflows = workflowRepository.findAll();
        } else if (hasRole(user, "ROLE_CLIENTE") || hasRole(user, "ROLE_EMPLEADO")) {
            workflows = workflowRepository.findByEstado("PUBLICADO");
        } else {
            workflows = workflowRepository.findByOwnerUserIdOrCollaboratorsUserId(user.getId(), user.getId());
        }

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
        bitacoraService.registrarAccion("ELIMINAR_WORKFLOW", "Workflow", "Se eliminó el workflow " + workflow.getNombre());
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
            w.getEstado(),
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
                || "PUBLICADO".equals(workflow.getEstado())
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

    private void syncPasosDesdeDiagrama(Workflow workflow) {
        String diagramData = workflow.getDiagramData();
        if (diagramData == null || diagramData.isBlank()) {
            return;
        }

        try {
            System.out.println("🔍 [DEBUG] Iniciando sincronizacion: " + workflow.getNombre());
            JsonNode root = objectMapper.readTree(diagramData);
            JsonNode nodes = root.get("nodes");
            
            if (nodes == null || !nodes.isArray()) {
                System.out.println("⚠️ [DEBUG] El diagrama no tiene un array de nodos valido.");
                return;
            }

            List<Workflow.Paso> nuevosPasos = new ArrayList<>();
            Map<String, String> nodeNames = new HashMap<>();
            
            // 1. Mapear nombres de nodos para herencia de departamentos
            for (JsonNode node : nodes) {
                if (node.has("id") && node.has("data") && node.get("data").has("label")) {
                    nodeNames.put(node.get("id").asText(), node.get("data").get("label").asText());
                }
            }

            // 2. Procesar cada nodo del diagrama
            int orden = 1;
            for (JsonNode node : nodes) {
                String type = node.has("type") ? node.get("type").asText() : "unknown";
                
                if ("start".equals(type) || "task".equals(type)) {
                    JsonNode data = node.get("data");
                    if (data == null) continue;

                    String label = data.has("label") ? data.get("label").asText() : "Paso " + orden;
                    
                    // Detectar si tiene formulario (check manual o existencia de campos)
                    boolean tieneSchema = data.has("formSchema");
                    boolean tieneFields = tieneSchema && data.get("formSchema").has("fields") && data.get("formSchema").get("fields").size() > 0;
                    boolean formActivo = (data.has("formEnabled") && data.get("formEnabled").asBoolean()) || tieneFields;

                    if (formActivo) {
                        System.out.println("📍 [DEBUG] Nodo procesado: " + label + " (Campos: " + (tieneFields ? data.get("formSchema").get("fields").size() : 0) + ")");
                        
                        // Determinar departamento (IA usa 'role')
                        String dept = "RECEPCION";
                        if (data.has("role")) dept = data.get("role").asText();
                        else if (data.has("departamento")) dept = data.get("departamento").asText();
                        else if (data.has("lane")) dept = data.get("lane").asText();
                        else if (node.has("parentId")) {
                            String parentName = nodeNames.get(node.get("parentId").asText());
                            if (parentName != null) dept = parentName;
                        }
                        dept = dept.replace("ROLE_", "");

                        // Crear o actualizar formulario (Incluso si esta vacio, para evitar el mensaje de error)
                        Formulario formulario = new Formulario();
                        formulario.setNombre("Form: " + label + " (" + workflow.getNombre() + ")");
                        List<Formulario.Campo> campos = new ArrayList<>();
                        
                        if (tieneFields) {
                            for (JsonNode field : data.get("formSchema").get("fields")) {
                                String fNombre = field.has("label") ? field.get("label").asText() : (field.has("id") ? field.get("id").asText() : "campo");
                                String fTipo = field.has("type") ? field.get("type").asText() : "text";
                                boolean fReq = field.has("required") && field.get("required").asBoolean();
                                campos.add(new Formulario.Campo(fNombre, fTipo, fReq, null));
                            }
                        } else {
                            // Campo por defecto para que no este totalmente vacio si la IA fallo
                            campos.add(new Formulario.Campo("Observaciones", "textarea", false, null));
                        }
                        
                        formulario.setCampos(campos);
                        formulario = formularioRepository.save(formulario);
                        String formularioId = formulario.getId();
                        System.out.println("   ✅ Formulario listo: " + formularioId + " asignado a " + dept);
                        
                        nuevosPasos.add(new Workflow.Paso(label, orden++, dept, formularioId));
                    }
                }
            }

            workflow.setPasos(nuevosPasos);
            System.out.println("🚀 [DEBUG] Sincronizacion terminada. Pasos: " + nuevosPasos.size());

        } catch (Exception e) {
            System.err.println("❌ [DEBUG] Error en sincronizacion: " + e.getMessage());
            e.printStackTrace();
        }
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
