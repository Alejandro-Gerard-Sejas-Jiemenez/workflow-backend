package com.sw.api.services;

import com.sw.api.models.Tarea;
import com.sw.api.models.Usuario;
import com.sw.api.models.Workflow;
import com.sw.api.dtos.TareaCreateDTO;
import com.sw.api.dtos.TareaAvanceDTO;
import com.sw.api.dtos.TareaResponseDTO;
import com.sw.api.dtos.WorkflowResponseDTO;
import com.sw.api.repositories.NotificacionRepository;
import com.sw.api.models.Notificacion;
import com.sw.api.repositories.TareaRepository;
import com.sw.api.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TareaService {

    private final TareaRepository tareaRepository;
    private final WorkflowService workflowService;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionRepository notificacionRepository;

    public TareaResponseDTO iniciarTarea(TareaCreateDTO dto, String username) {
        // Encontrar al actor que inicia por su email asociado en el Token
        Usuario usuario = usuarioRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Extraer el catálogo para buscar la delegación del Paso 1
        WorkflowResponseDTO workflow = workflowService.obtenerPorId(dto.workflowId());
        
        Workflow.Paso pasoInicial = workflow.pasos().stream()
                .filter(p -> p.getOrden() == 1)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("El Workflow no tiene un Paso 1 configurado."));

        // Construir instancia nativa
        Tarea tarea = new Tarea();
        tarea.setWorkflowId(workflow.id());
        tarea.setEstado("EN_PROGRESO");
        tarea.setPasoActual(1);
        tarea.setAsignadoA(pasoInicial.getDepartamento()); // Asignación dinámica inferida del Catálogo
        tarea.setDatos(dto.datos());

        // Inyectar Historial auto-generado
        Tarea.Historial primerHistorial = new Tarea.Historial(
                usuario.getId(),
                "INICIO_TAREA",
                "Tarea iniciada desde el formulario del cliente",
                LocalDateTime.now()
        );
        
        List<Tarea.Historial> historialList = new ArrayList<>();
        historialList.add(primerHistorial);
        tarea.setHistorial(historialList);

        return mapToDTO(tareaRepository.save(tarea));
    }

    public TareaResponseDTO gestionarTarea(String id, TareaAvanceDTO dto, String username) {
        Tarea tarea = tareaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tarea no encontrada"));
        Usuario usuario = usuarioRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
                
        // 1. Fusión de Datos
        if (dto.nuevosDatos() != null) {
            if (tarea.getDatos() == null) {
                tarea.setDatos(new java.util.HashMap<>());
            }
            tarea.getDatos().putAll(dto.nuevosDatos());
        }

        WorkflowResponseDTO workflow = workflowService.obtenerPorId(tarea.getWorkflowId());

        // 2. Evaluador SpEL
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("datos", tarea.getDatos());

        boolean reglaAplicada = false;
        
        if (workflow.reglas() != null) {
            for (Workflow.Regla regla : workflow.reglas()) {
                Boolean condicionCumplida = parser.parseExpression(regla.getCondicion()).getValue(context, Boolean.class);
                if (condicionCumplida != null && condicionCumplida) {
                    aplicarAccion(tarea, regla.getAccion());
                    reglaAplicada = true;
                    break;
                }
            }
        }

        // Si ninguna regla aplica, asumimos un avance natural
        if (!reglaAplicada) {
            avanzarPasoNatural(tarea, workflow);
        }

        // 3. Auditoría Local
        Tarea.Historial nuevoHistorial = new Tarea.Historial(
                usuario.getId(),
                dto.accionUsuario(),
                dto.detalle() != null ? dto.detalle() : "Acción ejecutada sobre la tarea",
                LocalDateTime.now()
        );
        tarea.getHistorial().add(nuevoHistorial);

        // 4. Notificación Asíncrona
        if (!tarea.getEstado().equals("COMPLETADO") && !tarea.getEstado().equals("RECHAZADO")) {
            generarNotificacion(tarea.getAsignadoA(), "Tienes una nueva tarea asignada en el paso " + tarea.getPasoActual());
        }

        return mapToDTO(tareaRepository.save(tarea));
    }

    private void aplicarAccion(Tarea tarea, String accion) {
        if (accion.startsWith("SET_STATE(")) {
            String nuevoEstado = accion.substring(10, accion.length() - 1).replace("'", "");
            tarea.setEstado(nuevoEstado);
        } else if (accion.startsWith("SKIP_TO(")) {
            Integer nuevoPaso = Integer.parseInt(accion.substring(8, accion.length() - 1).replace("'", ""));
            tarea.setPasoActual(nuevoPaso);
        } else if (accion.startsWith("ASSIGN_TO(")) {
            String nuevoAsignado = accion.substring(10, accion.length() - 1).replace("'", "");
            tarea.setAsignadoA(nuevoAsignado);
        }
    }

    private void avanzarPasoNatural(Tarea tarea, WorkflowResponseDTO workflow) {
        int siguientePasoIdx = tarea.getPasoActual() + 1;
        
        Workflow.Paso siguientePaso = workflow.pasos().stream()
                .filter(p -> p.getOrden() == siguientePasoIdx)
                .findFirst()
                .orElse(null);

        if (siguientePaso != null) {
            tarea.setPasoActual(siguientePasoIdx);
            tarea.setAsignadoA(siguientePaso.getDepartamento());
        } else {
            tarea.setEstado("COMPLETADO");
        }
    }

    private void generarNotificacion(String asignadoA, String mensaje) {
        Notificacion notificacion = new Notificacion();
        notificacion.setUsuarioId(asignadoA);
        notificacion.setMensaje(mensaje);
        notificacion.setTipo("TAREA_ASIGNADA");
        notificacion.setLeido(false);
        notificacion.setFecha(LocalDateTime.now());
        notificacionRepository.save(notificacion);
    }

    private TareaResponseDTO mapToDTO(Tarea t) {
        return new TareaResponseDTO(
                t.getId(),
                t.getWorkflowId(),
                t.getEstado(),
                t.getPasoActual(),
                t.getAsignadoA(),
                t.getPrioridad(),
                t.getDatos(),
                t.getHistorial()
        );
    }
}
