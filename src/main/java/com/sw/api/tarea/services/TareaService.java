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

@Service
@RequiredArgsConstructor
public class TareaService {

    private final TareaRepository tareaRepository;
    private final WorkflowRepository workflowRepository;
    private final UsuarioRepository usuarioRepository;
    private final com.sw.api.formulario.repositories.FormularioRepository formularioRepository;
    private final NotificacionService notificacionService;
    private final BitacoraService bitacoraService;

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
        registrarHistorial(tarea, usuario.getId(), "INICIO_TRAMITE", "El ciudadano inició el trámite de " + workflow.getNombre());

        Tarea guardada = tareaRepository.save(tarea);
        
        System.out.println("   ✅ Tarea creada con ID: " + guardada.getId() + " asignada a: " + guardada.getAsignadoA());
        
        // Auditoría Global
        bitacoraService.registrarAccion("INICIO_TRAMITE", "Tarea", "Trámite " + guardada.getId() + " iniciado por " + username);
        
        // Notificaciones
        generarNotificacion("RECEPCION", "Nuevo trámite de " + workflow.getNombre() + " recibido.");

        return mapToDTO(guardada);
    }

    public List<TareaResponseDTO> listarTareasParaEmpleado(String username) {
        Usuario usuario = usuarioRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        List<String> depts = usuario.getDepartamentos();
        List<Tarea> tareas = new ArrayList<>();
        
        // Cargar todas las tareas una sola vez para filtrar en memoria (más seguro con carriles)
        List<Tarea> todas = tareaRepository.findAll();
        System.out.println("🕵️ [LIST] Usuario: " + username + " | Deptos: " + depts + " | Total Tareas: " + todas.size());
        
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
        if (user.getRol() == null || user.getRol().getNombre() == null) return false;
        String userRole = user.getRol().getNombre();
        String targetRole = role;
        
        // Normalizar ambos para comparar
        if (!userRole.startsWith("ROLE_")) userRole = "ROLE_" + userRole;
        if (!targetRole.startsWith("ROLE_")) targetRole = "ROLE_" + targetRole;
        
        return userRole.equalsIgnoreCase(targetRole);
    }

    public TareaResponseDTO validarSolicitud(String id, ValidarSolicitudRequest dto, String username) {
        Tarea tarea = tareaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tarea no encontrada"));
        
        Usuario usuario = usuarioRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (dto.aprobado()) {
            Workflow workflow = workflowRepository.findById(tarea.getWorkflowId())
                    .orElseThrow(() -> new RuntimeException("Error: No se encontró el Workflow relacionado (ID: " + tarea.getWorkflowId() + ")"));
            
            if (workflow.getPasos() != null && !workflow.getPasos().isEmpty()) {
                // Saltamos al paso 1 (la primera tarea real) si existe, sino nos quedamos en el 0
                int indexSiguiente = (workflow.getPasos().size() > 1) ? 1 : 0;
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
            registrarHistorial(tarea, usuario.getId(), "VALIDACION_APROBADA", "Solicitud validada: " + dto.observaciones());
            generarNotificacion(tarea.getSolicitanteId(), "Tu solicitud ha sido aprobada e iniciada.");
        } else {
            tarea.setEstado("RECHAZADO_CORRECCION");
            tarea.setAsignadoA(tarea.getSolicitanteId());
            registrarHistorial(tarea, usuario.getId(), "VALIDACION_RECHAZADA", "Requiere corrección: " + dto.observaciones());
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

        if (dto.nuevosDatos() != null) {
            if (tarea.getDatos() == null) tarea.setDatos(new HashMap<>());
            tarea.getDatos().putAll(dto.nuevosDatos());
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
        if (tarea.getHistorial() == null) tarea.setHistorial(new ArrayList<>());
        tarea.getHistorial().add(new Tarea.Historial(usuarioId, accion, detalle, LocalDateTime.now()));
    }

    private void generarNotificacion(String usuarioId, String mensaje) {
        notificacionService.generarNotificacionSystem(usuarioId, mensaje, "TAREA_ASIGNADA");
    }

    private TareaResponseDTO mapToDTO(Tarea t) {
        com.sw.api.formulario.models.Formulario formulario = null;
        try {
            System.out.println("🔍 [MAP] Cargando DTO para Tarea: " + t.getId() + " (Paso Actual: " + t.getPasoActual() + ")");
            Workflow workflow = workflowRepository.findById(t.getWorkflowId()).orElse(null);
            
            if (workflow != null) {
                System.out.println("   📦 Workflow: " + workflow.getNombre() + " (Total Pasos: " + (workflow.getPasos() != null ? workflow.getPasos().size() : 0) + ")");
                
                if (workflow.getPasos() != null && t.getPasoActual() < workflow.getPasos().size()) {
                    var paso = workflow.getPasos().get(t.getPasoActual());
                    System.out.println("   📍 Paso detectado: " + paso.getNombre() + " (FormID: " + paso.getFormularioId() + ")");
                    
                    if (paso.getFormularioId() != null) {
                        formulario = formularioRepository.findById(paso.getFormularioId()).orElse(null);
                        if (formulario != null) {
                            System.out.println("   ✅ Formulario CARGADO: " + formulario.getNombre() + " con " + (formulario.getCampos() != null ? formulario.getCampos().size() : 0) + " campos.");
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
                formulario
        );
    }
}
