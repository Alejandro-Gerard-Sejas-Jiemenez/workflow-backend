package com.sw.api.tarea.services;

import com.sw.api.notificacion.services.NotificacionService;

import com.sw.api.formulario.repositories.FormularioRepository;

import com.sw.api.formulario.models.Formulario;

import com.sw.api.tarea.dtos.TareaAvanceDTO;

import com.sw.api.tarea.dtos.TareaResponseDTO;

import com.sw.api.tarea.dtos.TareaCreateDTO;

import com.sw.api.workflow.dtos.ValidarSolicitudRequest;

import com.sw.api.bitacora.services.BitacoraService;

import com.sw.api.tarea.models.Tarea;
import com.sw.api.usuario.models.Usuario;
import com.sw.api.workflow.models.Workflow;
import com.sw.api.tarea.repositories.TareaRepository;
import com.sw.api.usuario.repositories.UsuarioRepository;
import com.sw.api.workflow.repositories.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

@Service
@RequiredArgsConstructor
public class TareaService {

    private final TareaRepository tareaRepository;
    private final WorkflowRepository workflowRepository;
    private final UsuarioRepository usuarioRepository;
    private final com.sw.api.formulario.repositories.FormularioRepository formularioRepository;
    private final NotificacionService notificacionService;
    private final BitacoraService bitacoraService;
    private final ObjectMapper objectMapper;

    public TareaResponseDTO iniciarTarea(TareaCreateDTO dto, String username) {
        Usuario usuario = usuarioRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Workflow workflow = workflowRepository.findById(dto.workflowId())
                .orElseThrow(() -> new RuntimeException("Workflow no encontrado"));

        System.out.println("🚀 [CREATE] Iniciando tarea para Workflow: " + workflow.getNombre());

        Tarea tarea = new Tarea();
        tarea.setWorkflowId(workflow.getId());
        tarea.setSolicitanteId(usuario.getId());
        tarea.setEstado("PENDIENTE_VERIFICACION");
        tarea.setPasoActual(0);
        tarea.setAsignadoA("RECEPCION");
        tarea.setDatos(dto.datos() != null ? new HashMap<>(dto.datos()) : new HashMap<>());
        tarea.setDocumentosUrl(dto.documentosUrl());

        // Inicializar listas
        tarea.setHistorial(new ArrayList<>());
        tarea.setComentarios(new ArrayList<>());

        // Agregar primer hito al historial interno
        registrarHistorial(tarea, usuario.getId(), "INICIO_TRAMITE",
                "El ciudadano inició el trámite de " + workflow.getNombre());

        Tarea guardada = tareaRepository.save(tarea);

        System.out.println("   ✅ Tarea creada con ID: " + guardada.getId() + " asignada a: " + guardada.getAsignadoA());

        // Auditoría Global
        bitacoraService.registrarAccion("INICIO_TRAMITE", "Tarea",
                "Trámite " + guardada.getId() + " iniciado por " + username);

        // Notificaciones
        generarNotificacion("RECEPCION", "Nuevo trámite de " + workflow.getNombre() + " recibido.");

        return mapToDTO(guardada);
    }

    public List<TareaResponseDTO> listarTareasParaEmpleado(String username) {
        Usuario usuario = usuarioRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        List<String> depts = usuario.getDepartamentos();
        List<Tarea> tareas = new ArrayList<>();

        // Cargar todas las tareas una sola vez para filtrar en memoria (más seguro con
        // carriles)
        List<Tarea> todas = tareaRepository.findAll();
        System.out.println(
                "🕵️ [LIST] Usuario: " + username + " | Deptos: " + depts + " | Total Tareas: " + todas.size());

        for (Tarea t : todas) {
            String asignado = t.getAsignadoA();
            if (asignado == null) {
                System.out.println("   ⚠️ Tarea " + t.getId() + " no tiene departamento asignado.");
                continue;
            }

            // 1. Ver si está asignado directamente al email
            if (asignado.equalsIgnoreCase(username)) {
                tareas.add(t);
                continue;
            }

            // 2. Ver si es de RECEPCION (Global para empleados)
            if (asignado.equalsIgnoreCase("RECEPCION") && hasRole(usuario, "ROLE_EMPLEADO")) {
                tareas.add(t);
                continue;
            }

            // 3. Ver si es de alguno de sus departamentos (Insensible a mayúsculas)
            if (depts != null) {
                boolean pertenece = depts.stream()
                        .anyMatch(d -> d != null && d.trim().equalsIgnoreCase(asignado.trim()));
                if (pertenece) {
                    System.out.println("   ✅ Tarea aceptada por Depto Match: " + asignado);
                    tareas.add(t);
                    continue;
                }
            }
            System.out.println("   ❌ Tarea " + t.getId() + " rechazada. Asignada a: [" + asignado + "]");

            // 4. Ver si el usuario es el solicitante
            if (usuario.getId().equals(t.getSolicitanteId())) {
                tareas.add(t);
            }
        }

        return tareas.stream()
                .distinct()
                .filter(t -> !"COMPLETADO".equals(t.getEstado()) && !"RECHAZADO".equals(t.getEstado()))
                .map(this::mapToDTO)
                .toList();
    }

    private boolean hasRole(Usuario user, String role) {
        if (user.getRol() == null || user.getRol().getNombre() == null)
            return false;
        String userRole = user.getRol().getNombre();
        String targetRole = role;

        // Normalizar ambos para comparar
        if (!userRole.startsWith("ROLE_"))
            userRole = "ROLE_" + userRole;
        if (!targetRole.startsWith("ROLE_"))
            targetRole = "ROLE_" + targetRole;

        return userRole.equalsIgnoreCase(targetRole);
    }

    public TareaResponseDTO validarSolicitud(String id, ValidarSolicitudRequest dto, String username) {
        Tarea tarea = tareaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tarea no encontrada"));

        Usuario usuario = usuarioRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (dto.aprobado()) {
            Workflow workflow = workflowRepository.findById(tarea.getWorkflowId())
                    .orElseThrow(() -> new RuntimeException(
                            "Error: No se encontró el Workflow relacionado (ID: " + tarea.getWorkflowId() + ")"));

            if (workflow.getPasos() != null && !workflow.getPasos().isEmpty()) {
                // Determinar dinámicamente si hay un nodo de decisión configurado
                int indexSiguiente = determinarSiguientePasoIndex(tarea, workflow, tarea.getPasoActual());
                if (indexSiguiente == -1) {
                    indexSiguiente = (workflow.getPasos().size() > 1) ? 1 : 0;
                }
                var proximoPaso = workflow.getPasos().get(indexSiguiente);
                if (proximoPaso != null) {
                    tarea.setAsignadoA(proximoPaso.getDepartamento());
                    tarea.setEstado("EN_PROGRESO - " + proximoPaso.getNombre());
                    tarea.setPasoActual(indexSiguiente);
                    generarNotificacion(tarea.getAsignadoA(), "Nueva tarea asignada por validación inicial");
                }
            } else {
                tarea.setEstado("EN_PROGRESO");
            }
            registrarHistorial(tarea, usuario.getId(), "VALIDACION_APROBADA",
                    "Solicitud validada: " + dto.observaciones());
            generarNotificacion(tarea.getSolicitanteId(), "Tu solicitud ha sido aprobada e iniciada.");
        } else {
            tarea.setEstado("RECHAZADO_CORRECCION");
            tarea.setAsignadoA(tarea.getSolicitanteId());
            registrarHistorial(tarea, usuario.getId(), "VALIDACION_RECHAZADA",
                    "Requiere corrección: " + dto.observaciones());
            generarNotificacion(tarea.getSolicitanteId(), "Tu solicitud requiere correcciones: " + dto.observaciones());
        }

        bitacoraService.registrarAccion("VALIDAR_SOLICITUD", "Tarea", "Validación de tarea " + id + " por " + username);
        return mapToDTO(tareaRepository.save(tarea));
    }

    public TareaResponseDTO gestionarTarea(String id, TareaAvanceDTO dto, String username) {
        Tarea tarea = tareaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tarea no encontrada"));

        Usuario usuario = usuarioRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Workflow workflow = workflowRepository.findById(tarea.getWorkflowId())
                .orElseThrow(() -> new RuntimeException("Workflow no encontrado"));

        // 1. Aplicar nuevos datos primero para que las decisiones tengan la información actualizada
        if (dto.nuevosDatos() != null) {
            if (tarea.getDatos() == null) {
                tarea.setDatos(new HashMap<>());
            }
            tarea.getDatos().putAll(dto.nuevosDatos());
        }

        // 2. Determinar el siguiente paso evaluando el diagrama y posibles nodos de decisión
        int siguientePasoIndex = -1;
        boolean ruteadoPorDecision = false;

        String diagramData = workflow.getDiagramData();
        if (diagramData != null && !diagramData.isBlank() && workflow.getPasos() != null) {
            try {
                JsonNode root = objectMapper.readTree(diagramData);
                JsonNode nodes = root.get("nodes");
                JsonNode edges = root.get("edges");

                if (nodes != null && nodes.isArray() && edges != null && edges.isArray()) {
                    var pasoActual = workflow.getPasos().get(tarea.getPasoActual());
                    String currentLabel = pasoActual.getNombre();

                    // Buscar el ID del nodo actual en el diagrama
                    String currentNodeId = null;
                    for (JsonNode n : nodes) {
                        String type = n.has("type") ? n.get("type").asText() : "";
                        String label = n.has("data") && n.get("data").has("label") ? n.get("data").get("label").asText() : "";
                        if (("task".equals(type) || "start".equals(type)) && label.trim().equalsIgnoreCase(currentLabel.trim())) {
                            currentNodeId = n.has("id") ? n.get("id").asText() : null;
                            break;
                        }
                    }

                    if (currentNodeId != null) {
                        // Buscar conexiones salientes del nodo actual
                        String targetNodeId = null;
                        for (JsonNode edge : edges) {
                            String source = edge.has("source") ? edge.get("source").asText() : "";
                            if (currentNodeId.equals(source)) {
                                targetNodeId = edge.has("target") ? edge.get("target").asText() : null;
                                break;
                            }
                        }

                        if (targetNodeId != null) {
                            // Buscar el nodo destino
                            JsonNode targetNode = null;
                            for (JsonNode n : nodes) {
                                if (targetNodeId.equals(n.has("id") ? n.get("id").asText() : "")) {
                                    targetNode = n;
                                    break;
                                }
                            }

                            if (targetNode != null) {
                                String targetType = targetNode.has("type") ? targetNode.get("type").asText() : "";

                                // Si el nodo destino es de tipo "decision" (exclusive gateway), evaluamos sus reglas
                                if ("decision".equals(targetType)) {
                                    boolean ruleEvaluationResult = evaluateDecisionNode(targetNode, tarea.getDatos());
                                    System.out.println("🤖 [DECISION] Evaluando nodo de decisión [" + targetNodeId + "] -> Resultado: " + ruleEvaluationResult);

                                    // Buscar las conexiones que salen de este nodo de decisión
                                    String chosenNextNodeId = null;
                                    for (JsonNode edge : edges) {
                                        String source = edge.has("source") ? edge.get("source").asText() : "";
                                        if (targetNodeId.equals(source)) {
                                            String edgeLabel = edge.has("data") && edge.get("data").has("label") ? edge.get("data").get("label").asText() : "";
                                            boolean isTrueBranch = isTrueBranch(edgeLabel);
                                            
                                            if (ruleEvaluationResult && isTrueBranch) {
                                                chosenNextNodeId = edge.has("target") ? edge.get("target").asText() : null;
                                                break;
                                            } else if (!ruleEvaluationResult && !isTrueBranch && (!edgeLabel.isBlank())) {
                                                // Rama de descarte (No / Rechazado / etc.)
                                                chosenNextNodeId = edge.has("target") ? edge.get("target").asText() : null;
                                                break;
                                            }
                                        }
                                    }

                                    // Fallback por si no encontramos rama específica de falso
                                    if (chosenNextNodeId == null && !ruleEvaluationResult) {
                                        for (JsonNode edge : edges) {
                                            String source = edge.has("source") ? edge.get("source").asText() : "";
                                            if (targetNodeId.equals(source)) {
                                                String edgeLabel = edge.has("data") && edge.get("data").has("label") ? edge.get("data").get("label").asText() : "";
                                                boolean isTrueBranch = isTrueBranch(edgeLabel);
                                                if (!isTrueBranch) {
                                                    chosenNextNodeId = edge.has("target") ? edge.get("target").asText() : null;
                                                    break;
                                                }
                                            }
                                        }
                                    }

                                    if (chosenNextNodeId != null) {
                                        // Encontrar el nombre del paso final
                                        String nextPasoLabel = null;
                                        for (JsonNode n : nodes) {
                                            if (chosenNextNodeId.equals(n.has("id") ? n.get("id").asText() : "")) {
                                                nextPasoLabel = n.has("data") && n.get("data").has("label") ? n.get("data").get("label").asText() : "";
                                                break;
                                            }
                                        }

                                        if (nextPasoLabel != null && !nextPasoLabel.isBlank()) {
                                            // Buscar el índice en los pasos del workflow
                                            for (int i = 0; i < workflow.getPasos().size(); i++) {
                                                if (workflow.getPasos().get(i).getNombre().trim().equalsIgnoreCase(nextPasoLabel.trim())) {
                                                    siguientePasoIndex = i;
                                                    ruteadoPorDecision = true;
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("❌ Error evaluando ruteo dinámico por decisión: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // 3. Aplicar transición
        if (ruteadoPorDecision && siguientePasoIndex != -1) {
            tarea.setPasoActual(siguientePasoIndex);
            var proximoPaso = workflow.getPasos().get(tarea.getPasoActual());
            tarea.setAsignadoA(proximoPaso.getDepartamento());
            tarea.setEstado("EN_PROGRESO - " + proximoPaso.getNombre());
            generarNotificacion(tarea.getAsignadoA(), "Tienes una nueva tarea asignada por flujo de decisión: " + workflow.getNombre());
            generarNotificacion(tarea.getSolicitanteId(), "Tu trámite ha avanzado a: " + proximoPaso.getNombre());
        } else {
            // Ruteo secuencial tradicional
            if (tarea.getPasoActual() < workflow.getPasos().size() - 1) {
                tarea.setPasoActual(tarea.getPasoActual() + 1);
                var proximoPaso = workflow.getPasos().get(tarea.getPasoActual());
                tarea.setAsignadoA(proximoPaso.getDepartamento());
                tarea.setEstado("EN_PROGRESO - " + proximoPaso.getNombre());
                generarNotificacion(tarea.getAsignadoA(), "Tienes una nueva tarea asignada: " + workflow.getNombre());
                generarNotificacion(tarea.getSolicitanteId(), "Tu trámite ha avanzado a: " + proximoPaso.getNombre());
            } else {
                tarea.setEstado("COMPLETADO");
                tarea.setAsignadoA(null);
                generarNotificacion(tarea.getSolicitanteId(), "¡Felicidades! Tu trámite de " + workflow.getNombre() + " ha finalizado.");
            }
        }

        registrarHistorial(tarea, usuario.getId(), dto.accionUsuario(), dto.detalle());
        Tarea guardada = tareaRepository.save(tarea);

        bitacoraService.registrarAccion("GESTIONAR_TAREA", "Tarea", "Tarea " + id + " gestionada por " + username);

        return mapToDTO(guardada);
    }

    public TareaResponseDTO obtenerPorId(String id) {
        return tareaRepository.findById(id)
                .map(this::mapToDTO)
                .orElseThrow(() -> new RuntimeException("Tarea no encontrada"));
    }

    private void registrarHistorial(Tarea tarea, String usuarioId, String accion, String detalle) {
        if (tarea.getHistorial() == null)
            tarea.setHistorial(new ArrayList<>());
        tarea.getHistorial().add(new Tarea.Historial(usuarioId, accion, detalle, LocalDateTime.now()));
    }

    private void generarNotificacion(String usuarioId, String mensaje) {
        notificacionService.generarNotificacionSystem(usuarioId, mensaje, "TAREA_ASIGNADA");
    }

    private boolean isTrueBranch(String edgeLabel) {
        if (edgeLabel == null) return false;
        String clean = java.text.Normalizer.normalize(edgeLabel, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .trim();
        return clean.equals("si") || 
               clean.contains("apro") || 
               clean.contains("acept") || 
               clean.contains("true") || 
               clean.contains("ture") || 
               clean.contains("yes") || 
               clean.contains("verdadero") || 
               clean.equals("ok");
    }

    private TareaResponseDTO mapToDTO(Tarea t) {
        com.sw.api.formulario.models.Formulario formulario = null;
        try {
            System.out.println(
                    "🔍 [MAP] Cargando DTO para Tarea: " + t.getId() + " (Paso Actual: " + t.getPasoActual() + ")");
            Workflow workflow = workflowRepository.findById(t.getWorkflowId()).orElse(null);

            if (workflow != null) {
                System.out.println("   📦 Workflow: " + workflow.getNombre() + " (Total Pasos: "
                        + (workflow.getPasos() != null ? workflow.getPasos().size() : 0) + ")");

                if (workflow.getPasos() != null && t.getPasoActual() < workflow.getPasos().size()) {
                    var paso = workflow.getPasos().get(t.getPasoActual());
                    System.out.println(
                            "   📍 Paso detectado: " + paso.getNombre() + " (FormID: " + paso.getFormularioId() + ")");

                    if (paso.getFormularioId() != null) {
                        formulario = formularioRepository.findById(paso.getFormularioId()).orElse(null);
                        if (formulario != null) {
                            System.out.println("   ✅ Formulario CARGADO: " + formulario.getNombre() + " con "
                                    + (formulario.getCampos() != null ? formulario.getCampos().size() : 0)
                                    + " campos.");
                        } else {
                            System.out.println("   ❌ Formulario NO ENCONTRADO en DB con ID: " + paso.getFormularioId());
                        }
                    } else {
                        System.out.println("   ⚠️ El paso no tiene asignado un FormularioId.");
                    }
                } else {
                    System.out.println("   ❌ Indice de paso fuera de rango o pasos nulos.");
                }
            } else {
                System.out.println("   ❌ Workflow no encontrado para la tarea.");
            }
        } catch (Exception e) {
            System.err.println("❌ Error al cargar formulario para DTO: " + e.getMessage());
            e.printStackTrace();
        }

        return new TareaResponseDTO(
                t.getId(),
                t.getWorkflowId(),
                t.getEstado(),
                t.getPasoActual(),
                t.getAsignadoA(),
                t.getPrioridad(),
                t.getDatos(),
                t.getDocumentosUrl(),
                t.getHistorial(),
                t.getComentarios(),
                formulario);
    }

    private int determinarSiguientePasoIndex(Tarea tarea, Workflow workflow, int pasoActualIndex) {
        String diagramData = workflow.getDiagramData();
        if (diagramData == null || diagramData.isBlank() || workflow.getPasos() == null) {
            return -1;
        }

        try {
            JsonNode root = objectMapper.readTree(diagramData);
            JsonNode nodes = root.get("nodes");
            JsonNode edges = root.get("edges");

            if (nodes == null || !nodes.isArray() || edges == null || !edges.isArray()) {
                return -1;
            }

            var pasoActual = workflow.getPasos().get(pasoActualIndex);
            String currentLabel = pasoActual.getNombre();

            // Buscar el ID del nodo actual en el diagrama
            String currentNodeId = null;
            for (JsonNode n : nodes) {
                String type = n.has("type") ? n.get("type").asText() : "";
                String label = n.has("data") && n.get("data").has("label") ? n.get("data").get("label").asText() : "";
                if (("task".equals(type) || "start".equals(type)) && label.trim().equalsIgnoreCase(currentLabel.trim())) {
                    currentNodeId = n.has("id") ? n.get("id").asText() : null;
                    break;
                }
            }

            if (currentNodeId == null) {
                return -1;
            }

            // Buscar conexiones salientes del nodo actual
            String targetNodeId = null;
            for (JsonNode edge : edges) {
                String source = edge.has("source") ? edge.get("source").asText() : "";
                if (currentNodeId.equals(source)) {
                    targetNodeId = edge.has("target") ? edge.get("target").asText() : null;
                    break;
                }
            }

            if (targetNodeId == null) {
                return -1;
            }

            // Buscar el nodo destino
            JsonNode targetNode = null;
            for (JsonNode n : nodes) {
                if (targetNodeId.equals(n.has("id") ? n.get("id").asText() : "")) {
                    targetNode = n;
                    break;
                }
            }

            if (targetNode == null) {
                return -1;
            }

            String targetType = targetNode.has("type") ? targetNode.get("type").asText() : "";

            // Si el nodo destino es de tipo "decision" (exclusive gateway), evaluamos sus reglas
            if ("decision".equals(targetType)) {
                boolean ruleEvaluationResult = evaluateDecisionNode(targetNode, tarea.getDatos());
                System.out.println("🤖 [DECISION] Evaluando nodo de decisión [" + targetNodeId + "] -> Resultado: " + ruleEvaluationResult);

                // Buscar las conexiones que salen de este nodo de decisión
                String chosenNextNodeId = null;
                for (JsonNode edge : edges) {
                    String source = edge.has("source") ? edge.get("source").asText() : "";
                    if (targetNodeId.equals(source)) {
                        String edgeLabel = edge.has("data") && edge.get("data").has("label") ? edge.get("data").get("label").asText() : "";
                        boolean isTrueBranch = isTrueBranch(edgeLabel);
                        
                        if (ruleEvaluationResult && isTrueBranch) {
                            chosenNextNodeId = edge.has("target") ? edge.get("target").asText() : null;
                            break;
                        } else if (!ruleEvaluationResult && !isTrueBranch && (!edgeLabel.isBlank())) {
                            chosenNextNodeId = edge.has("target") ? edge.get("target").asText() : null;
                            break;
                        }
                    }
                }

                // Fallback por si no encontramos rama específica de falso
                if (chosenNextNodeId == null && !ruleEvaluationResult) {
                    for (JsonNode edge : edges) {
                        String source = edge.has("source") ? edge.get("source").asText() : "";
                        if (targetNodeId.equals(source)) {
                            String edgeLabel = edge.has("data") && edge.get("data").has("label") ? edge.get("data").get("label").asText() : "";
                            boolean isTrueBranch = isTrueBranch(edgeLabel);
                            if (!isTrueBranch) {
                                chosenNextNodeId = edge.has("target") ? edge.get("target").asText() : null;
                                break;
                            }
                        }
                    }
                }

                if (chosenNextNodeId != null) {
                    // Encontrar el nombre del paso final
                    String nextPasoLabel = null;
                    for (JsonNode n : nodes) {
                        if (chosenNextNodeId.equals(n.has("id") ? n.get("id").asText() : "")) {
                            nextPasoLabel = n.has("data") && n.get("data").has("label") ? n.get("data").get("label").asText() : "";
                            break;
                        }
                    }

                    if (nextPasoLabel != null && !nextPasoLabel.isBlank()) {
                        // Buscar el índice en los pasos del workflow
                        for (int i = 0; i < workflow.getPasos().size(); i++) {
                            if (workflow.getPasos().get(i).getNombre().trim().equalsIgnoreCase(nextPasoLabel.trim())) {
                                return i;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error en determinarSiguientePasoIndex: " + e.getMessage());
        }

        return -1;
    }

    private boolean evaluateDecisionNode(JsonNode decisionNode, Map<String, Object> data) {
        if (data == null) {
            System.out.println("🕵️‍♂️ [DECISION EVAL] Data is null. Returning false.");
            return false;
        }
        try {
            JsonNode dataNode = decisionNode.get("data");
            if (dataNode == null || !dataNode.has("conditionConfig")) {
                System.out.println("🕵️‍♂️ [DECISION EVAL] No conditionConfig found in decisionNode.");
                return false;
            }
            JsonNode config = dataNode.get("conditionConfig");
            String logicalOperator = config.has("logicalOperator") ? config.get("logicalOperator").asText() : "AND";
            JsonNode rules = config.get("rules");

            System.out.println("🕵️‍♂️ [DECISION EVAL] logicalOperator: " + logicalOperator + ", Data: " + data);

            if (rules == null || !rules.isArray() || rules.size() == 0) {
                System.out.println("🕵️‍♂️ [DECISION EVAL] No rules defined. Returning true by default.");
                return true;
            }

            boolean isAnd = "AND".equalsIgnoreCase(logicalOperator);
            boolean result = isAnd;

            for (JsonNode rule : rules) {
                String field = rule.has("field") ? rule.get("field").asText() : "";
                String operator = rule.has("operator") ? rule.get("operator").asText() : "==";
                String expectedValue = rule.has("value") ? rule.get("value").asText() : "";

                Object actualValue = data.get(field);
                boolean rulePassed = evaluateRule(actualValue, operator, expectedValue);

                System.out.println("🕵️‍♂️ [RULE EVAL] Field: [" + field + "], Operator: [" + operator + "], Expected: [" + expectedValue + "], Actual: " + (actualValue != null ? actualValue.getClass().getSimpleName() + " (" + actualValue + ")" : "null") + " -> Passed: " + rulePassed);

                if (isAnd) {
                    result = result && rulePassed;
                    if (!result) {
                        System.out.println("   -> [AND] Rule failed. Short-circuiting.");
                        break;
                    }
                } else {
                    result = result || rulePassed;
                    if (result) {
                        System.out.println("   -> [OR] Rule passed. Short-circuiting.");
                        break;
                    }
                }
            }
            System.out.println("🕵️‍♂️ [DECISION EVAL FINAL] Result: " + result);
            return result;
        } catch (Exception e) {
            System.err.println("❌ Error evaluando reglas del nodo de decisión: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean evaluateRule(Object actualValue, String operator, String expectedValue) {
        if (actualValue == null) {
            boolean passed = "==".equals(operator) && (expectedValue == null || expectedValue.isBlank() || "false".equalsIgnoreCase(expectedValue));
            System.out.println("   🔍 [evaluateRule] actualValue is null. Expected: [" + expectedValue + "], Operator: [" + operator + "] -> Passed: " + passed);
            return passed;
        }

        String actualStr = actualValue.toString().trim();
        String expectedStr = expectedValue.trim();

        if (actualValue instanceof Boolean) {
            boolean actBool = (Boolean) actualValue;
            boolean expBool = "true".equalsIgnoreCase(expectedStr) || 
                              "ture".equalsIgnoreCase(expectedStr) || 
                              "1".equals(expectedStr) || 
                              "yes".equalsIgnoreCase(expectedStr);
            boolean passed = false;
            if ("==".equals(operator)) passed = (actBool == expBool);
            if ("!=".equals(operator)) passed = (actBool != expBool);
            System.out.println("   🔍 [evaluateRule] Boolean comparison. actBool: " + actBool + ", expBool: " + expBool + " -> Passed: " + passed);
            return passed;
        }

        if (actualValue instanceof java.util.Collection) {
            java.util.Collection<?> col = (java.util.Collection<?>) actualValue;
            boolean contains = col.stream().anyMatch(item -> item != null && item.toString().trim().equalsIgnoreCase(expectedStr));
            boolean passed = false;
            if ("==".equals(operator)) passed = contains;
            if ("!=".equals(operator)) passed = !contains;
            System.out.println("   🔍 [evaluateRule] Collection comparison. Collection: " + col + ", Expected: [" + expectedStr + "] -> Contains: " + contains + " -> Passed: " + passed);
            return passed;
        }

        if (actualStr.contains(",")) {
            String[] parts = actualStr.split(",");
            boolean contains = false;
            for (String p : parts) {
                if (p.trim().equalsIgnoreCase(expectedStr)) {
                    contains = true;
                    break;
                }
            }
            boolean passed = false;
            if ("==".equals(operator)) passed = contains;
            if ("!=".equals(operator)) passed = !contains;
            System.out.println("   🔍 [evaluateRule] Comma-separated string comparison. Actual: [" + actualStr + "], Expected: [" + expectedStr + "] -> Contains: " + contains + " -> Passed: " + passed);
            return passed;
        }

        try {
            double actualNum = Double.parseDouble(actualStr);
            double expectedNum = Double.parseDouble(expectedStr);
            boolean passed = false;
            switch (operator) {
                case "==": passed = (actualNum == expectedNum); break;
                case "!=": passed = (actualNum != expectedNum); break;
                case ">": passed = (actualNum > expectedNum); break;
                case "<": passed = (actualNum < expectedNum); break;
                case ">=": passed = (actualNum >= expectedNum); break;
                case "<=": passed = (actualNum <= expectedNum); break;
            }
            System.out.println("   🔍 [evaluateRule] Numeric comparison. Actual: " + actualNum + ", Expected: " + expectedNum + ", Operator: [" + operator + "] -> Passed: " + passed);
            return passed;
        } catch (NumberFormatException e) {
            boolean passed = false;
            switch (operator) {
                case "==": passed = actualStr.equalsIgnoreCase(expectedStr); break;
                case "!=": passed = !actualStr.equalsIgnoreCase(expectedStr); break;
            }
            System.out.println("   🔍 [evaluateRule] String comparison (fallback). Actual: [" + actualStr + "], Expected: [" + expectedStr + "], Operator: [" + operator + "] -> Passed: " + passed);
            return passed;
        }
    }
}
